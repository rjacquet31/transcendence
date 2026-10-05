package com.transcendance.backend.dto;

import java.time.LocalDateTime;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Pushed on /user/queue/notifications.chat to the receiver of a message. Carries
 * the sender's name and a short preview so the client can show it without any
 * lookup.
 */
@Getter
@AllArgsConstructor
public class ChatNotificationDto {

    private Long messageId;

    private UUID senderId;

    private String senderUserName;

    private String preview;

    private LocalDateTime sentAt;
}
