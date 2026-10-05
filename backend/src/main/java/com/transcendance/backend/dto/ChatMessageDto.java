package com.transcendance.backend.dto;

import java.time.LocalDateTime;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ChatMessageDto {

    private Long id;

    private UUID senderId;

    private UUID receiverId;

    private String content;

    private LocalDateTime sentAt;

    @JsonProperty("isRead")
    private boolean isRead;
}