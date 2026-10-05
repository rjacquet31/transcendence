package com.transcendance.backend.services;

import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import com.transcendance.backend.dto.ChatMessageDto;
import com.transcendance.backend.dto.ChatNotificationDto;
import com.transcendance.backend.dto.FriendNotificationDto;
import com.transcendance.backend.model.FriendNotificationType;
import com.transcendance.backend.model.User;

import lombok.RequiredArgsConstructor;

/**
 * Pushes user-facing notifications over STOMP.
 *
 * Callers invoke it once their service call has returned, so the transaction is
 * already committed: a client reacting to the notification finds the row in
 * database. Nothing is stored: a user without an open session simply misses the
 * notification and finds the state on his friends or chat page.
 */
@Service
@RequiredArgsConstructor
public class NotificationService {

    private static final Logger log = LogManager.getLogger(NotificationService.class);

    private static final String FRIEND_DESTINATION = "/queue/notifications.friends";

    private static final String CHAT_DESTINATION = "/queue/notifications.chat";

    private static final int PREVIEW_LENGTH = 80;

    private final SimpMessagingTemplate messagingTemplate;

    /** Tells the target that the sender has just asked him as a friend. */
    public void friendRequestReceived(UUID toUserId, User sender) {
        push(toUserId, FRIEND_DESTINATION, new FriendNotificationDto(
                FriendNotificationType.REQUEST_RECEIVED, sender.getIdUser(), sender.getUserName()));
    }

    /** Tells the original sender that his request has been accepted. */
    public void friendRequestAccepted(UUID toUserId, User accepter) {
        push(toUserId, FRIEND_DESTINATION, new FriendNotificationDto(
                FriendNotificationType.REQUEST_ACCEPTED, accepter.getIdUser(), accepter.getUserName()));
    }

    /** Tells the receiver of a stored message who wrote to him, with a preview. */
    public void chatMessageReceived(ChatMessageDto message, String senderUserName) {
        push(message.getReceiverId(), CHAT_DESTINATION, new ChatNotificationDto(
                message.getId(), message.getSenderId(), senderUserName,
                preview(message.getContent()), message.getSentAt()));
    }

    private String preview(String content) {
        if (content == null) {
            return "";
        }
        return content.length() <= PREVIEW_LENGTH ? content : content.substring(0, PREVIEW_LENGTH) + "…";
    }

    private void push(UUID toUserId, String destination, Object payload) {
        try {
            messagingTemplate.convertAndSendToUser(toUserId.toString(), destination, payload);
        } catch (Exception e) {
            log.error("Notification could not be pushed to {} on {}: {}",
                    toUserId, destination, e.getMessage());
        }
    }
}
