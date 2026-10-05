package com.transcendance.backend.dto;

import java.util.UUID;

import com.transcendance.backend.model.FriendNotificationType;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Pushed on /user/queue/notifications.friends. fromUserId is always the other
 * end of the relationship: the id the friend-list routes take.
 */
@Getter
@AllArgsConstructor
public class FriendNotificationDto {

    private FriendNotificationType type;

    private UUID fromUserId;

    private String fromUserName;
}
