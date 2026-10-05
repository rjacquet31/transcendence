package com.transcendance.backend.undercover.api.dto;

import java.util.UUID;

import com.transcendance.backend.model.User;

public record UndercoverUserDto(UUID userId, String userName, String avatarUrl) {

    public static UndercoverUserDto of(UUID userId, User user) {
        return user == null
                ? new UndercoverUserDto(userId, null, null)
                : new UndercoverUserDto(userId, user.getUserName(), user.getAvatarUrl());
    }
}
