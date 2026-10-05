package com.transcendance.backend.undercover.api.dto;

import java.time.Instant;

import com.transcendance.backend.undercover.application.UndercoverChatMessage;

public record UndercoverChatMessageDto(long matchId, int seat, String userName, String text, Instant sentAt) {

    public static UndercoverChatMessageDto of(UndercoverChatMessage message, String userName) {
        return new UndercoverChatMessageDto(message.matchId(), message.seat(), userName, message.text(),
                message.sentAt());
    }
}
