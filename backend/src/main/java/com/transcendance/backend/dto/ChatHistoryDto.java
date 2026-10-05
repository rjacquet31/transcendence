package com.transcendance.backend.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ChatHistoryDto {
    private List<ChatMessageDto> messages;
    private Long nextCursor;
    private boolean hasMore;
}
