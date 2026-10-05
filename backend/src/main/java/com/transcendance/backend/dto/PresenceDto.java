package com.transcendance.backend.dto;

import java.time.Instant;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class PresenceDto {
    private UUID idUser;
    private boolean online;
    private Instant lastSeen;
}
