package com.transcendance.backend.services;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;

import com.transcendance.backend.dto.ChatHistoryDto;
import com.transcendance.backend.dto.ChatMessageDto;
import com.transcendance.backend.dto.ChatMessageRequestDto;
import com.transcendance.backend.model.ChatMessage;
import com.transcendance.backend.model.User;
import com.transcendance.backend.repository.ChatMessageRepository;

import jakarta.transaction.Transactional;

@Service
public class ChatService {

        private static final org.apache.logging.log4j.Logger log = org.apache.logging.log4j.LogManager
                        .getLogger(ChatService.class);

        private final UserService userService;
        private final FriendListService friendListService;
        private final ChatMessageRepository chatMessageRepository;

        public ChatService(UserService userService, FriendListService friendListService,
                        ChatMessageRepository chatMessageRepository) {
                this.userService = userService;
                this.friendListService = friendListService;
                this.chatMessageRepository = chatMessageRepository;
        }

        /**
         * Persists an incoming chat message, then pushes it to both participants.
         *
         * The sender comes from the Principal established during the WebSocket
         * handshake, never from the payload: a client must not be able to send a
         * message on someone else's behalf.
         * 
         * @param senderId the UUID of the sender, extracted from the Principal
         * @param message  the chat message request DTO containing the content and
         *                 receiver ID
         * @return a ChatMessageDto representing the saved message
         * @throws IllegalArgumentException if the sender or receiver is not found, or
         *                                  if
         *                                  they are not friends
         */
        @Transactional
        public ChatMessageDto sendMessage(UUID senderId, ChatMessageRequestDto message) {
                User sender = userService.getUserById(senderId)
                                .orElseThrow(() -> new IllegalArgumentException("Expéditeur introuvable"));
                User receiver = userService.getUserById(message.getReceiverId())
                                .orElseThrow(() -> new IllegalArgumentException("Destinataire introuvable"));

                if (sender.getDeletedAt() != null || receiver.getDeletedAt() != null) {
                        throw new IllegalArgumentException("Cet utilisateur n'est plus disponible");
                }

                if (!friendListService.checkFriendship(sender, receiver)) {
                        log.warn("Users {} and {} are not friends. Message not sent.", sender.getUserName(),
                                        receiver.getUserName());
                        throw new IllegalArgumentException("Vous n'êtes pas amis");
                }
                ChatMessage chatMessage = new ChatMessage(message.getContent(), sender, receiver);
                chatMessageRepository.save(chatMessage);
                log.info("Message sent from {} to {}: {} chars", sender.getUserName(), receiver.getUserName(),
                                message.getContent().length());
                return new ChatMessageDto(chatMessage.getId(), chatMessage.getSender().getIdUser(),
                                chatMessage.getReceiver().getIdUser(), chatMessage.getContent(),
                                chatMessage.getSentAt(), chatMessage.isRead());
        }

        /**
         * Retrieves the chat history between two users.
         *
         * @param me      the current user
         * @param otherId the ID of the other user
         * @param before  the timestamp before which to retrieve messages
         * @param limit   the maximum number of messages to retrieve
         * @return a ChatHistoryDto containing the chat history
         */
        @Transactional
        public ChatHistoryDto getHistory(User me, UUID otherId, Long before, int limit) {

                User other = userService.getUserById(otherId)
                                .orElseThrow(() -> new IllegalArgumentException("Utilisateur introuvable"));

                if (!friendListService.checkFriendship(me, other)) {
                        throw new IllegalArgumentException("Vous n'êtes pas amis");
                }

                Slice<ChatMessage> slice = chatMessageRepository.findConversation(
                                me.getIdUser(), otherId, before, PageRequest.of(0, limit));

                List<ChatMessageDto> messages = slice.getContent().stream()
                                .map(m -> new ChatMessageDto(m.getId(), m.getSender().getIdUser(),
                                                m.getReceiver().getIdUser(), m.getContent(),
                                                m.getSentAt(), m.isRead()))
                                .toList();

                Long nextCursor = slice.hasNext() && !messages.isEmpty()
                                ? messages.get(messages.size() - 1).getId()
                                : null;

                return new ChatHistoryDto(messages, nextCursor, slice.hasNext());
        }

        /**
         * Marks every message received from another user as read.
         *
         * Idempotent: a second call simply reports 0, since the update only
         * touches messages still flagged unread.
         *
         * @param me      the current user, the recipient of the messages
         * @param otherId the ID of the user whose messages are being read
         * @return how many messages just went from unread to read
         * @throws IllegalArgumentException if the other user is unknown, or if the
         *                                  two users are not friends
         */
        @Transactional
        public int markAsRead(User me, UUID otherId) {

                User other = userService.getUserById(otherId)
                                .orElseThrow(() -> new IllegalArgumentException("Utilisateur introuvable"));

                if (!friendListService.checkFriendship(me, other)) {
                        throw new IllegalArgumentException("Vous n'êtes pas amis");
                }

                int updated = chatMessageRepository.markConversationAsRead(me.getIdUser(), otherId);
                log.info("{} read {} message(s) from {}", me.getUserName(), updated, other.getUserName());
                return updated;
        }

}
