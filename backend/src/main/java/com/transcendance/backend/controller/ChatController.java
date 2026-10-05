package com.transcendance.backend.controller;

import java.security.Principal;
import java.util.Map;
import java.util.UUID;

import org.apache.logging.log4j.Logger;
import org.springframework.messaging.converter.MessageConversionException;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.support.MethodArgumentNotValidException;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;

import com.transcendance.backend.dto.ChatMessageDto;
import com.transcendance.backend.dto.ChatMessageRequestDto;
import com.transcendance.backend.services.ChatService;
import com.transcendance.backend.services.NotificationService;
import com.transcendance.backend.services.UserService;

import jakarta.validation.Valid;

@Controller
public class ChatController {

    private static final Logger log = org.apache.logging.log4j.LogManager.getLogger(ChatController.class);

    /**
     * Suffixes appended to /user/{principal} by convertAndSendToUser
     * and @SendToUser.
     */
    private static final String MESSAGE_DESTINATION = "/queue/messages";
    private static final String ERROR_DESTINATION = "/queue/errors";

    private final ChatService chatService;
    private final SimpMessagingTemplate messagingTemplate;
    private final NotificationService notificationService;
    private final UserService userService;

    public ChatController(ChatService chatService, SimpMessagingTemplate messagingTemplate,
            NotificationService notificationService, UserService userService) {
        this.chatService = chatService;
        this.messagingTemplate = messagingTemplate;
        this.notificationService = notificationService;
        this.userService = userService;
    }

    /**
     * Persists an incoming chat message, then pushes it to both participants.
     *
     * The sender comes from the Principal established during the WebSocket
     * handshake, never from the payload: a client must not be able to send a
     * message on someone else's behalf.
     */
    @MessageMapping("/chat.send")
    public void send(Principal principal, @Valid @Payload ChatMessageRequestDto request) {

        if (principal == null || principal.getName() == null) {
            log.error("chat.send reached the controller without a Principal: check UserHandshakeHandler");
            return;
        }

        UUID senderId = UUID.fromString(principal.getName());
        log.info("chat.send from {} to {} ({} chars)", senderId, request.getReceiverId(),
                request.getContent().length());

        ChatMessageDto message = chatService.sendMessage(senderId, request);

        // The receiver gets the message, and the sender gets it back so his other
        // open sessions stay in sync and can confirm the message was stored.
        messagingTemplate.convertAndSendToUser(
                message.getReceiverId().toString(), MESSAGE_DESTINATION, message);
        messagingTemplate.convertAndSendToUser(
                message.getSenderId().toString(), MESSAGE_DESTINATION, message);

        // sendMessage has already checked the sender exists: the lookup only
        // fetches his name for the receiver's notification.
        userService.getUserById(senderId).ifPresent(
                sender -> notificationService.chatMessageReceived(message, sender.getUserName()));
    }

    /**
     * Reports a rejected message back to its sender.
     *
     * STOMP carries no status code: without this handler the exception is only
     * logged server-side and the client keeps believing the message went through.
     */
    @MessageExceptionHandler(IllegalArgumentException.class)
    @SendToUser(ERROR_DESTINATION)
    public Map<String, String> handleRejectedMessage(IllegalArgumentException exception) {
        String reason = exception.getMessage() == null ? "Message refusé" : exception.getMessage();
        log.warn("chat.send rejected: {}", reason);
        return Map.of("error", reason);
    }

    /**
     * Reports a payload that failed @Valid: blank content, missing receiver, or
     * content over the size limit.
     */
    @MessageExceptionHandler(MethodArgumentNotValidException.class)
    @SendToUser(ERROR_DESTINATION)
    public Map<String, String> handleInvalidPayload(MethodArgumentNotValidException exception) {
        String reason = firstFieldError(exception.getBindingResult());
        log.warn("chat.send rejected: invalid payload ({})", reason);
        return Map.of("error", reason);
    }

    /**
     * Reports a payload the converter could not even read, such as a malformed
     * UUID or broken JSON. This one fires before the handler method is entered,
     * so it is not covered by the two handlers above.
     */
    @MessageExceptionHandler(MessageConversionException.class)
    @SendToUser(ERROR_DESTINATION)
    public Map<String, String> handleUnreadablePayload(MessageConversionException exception) {
        log.warn("chat.send rejected: unreadable payload ({})", exception.getMessage());
        return Map.of("error", "Message mal formé");
    }

    private String firstFieldError(BindingResult bindingResult) {
        if (bindingResult == null) {
            return "Message invalide";
        }
        return bindingResult.getFieldErrors().stream()
                .findFirst()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .orElse("Message invalide");
    }
}
