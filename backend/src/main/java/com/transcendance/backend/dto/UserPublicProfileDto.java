package com.transcendance.backend.dto;

import java.time.Instant;
import java.util.UUID;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class UserPublicProfileDto {
        private UUID            id;
        private String          userName;
        private String          email;
        private Instant         online;
        private String          avatarUrl;
        private String          role;
        // Only filled on /api/users/me: null on other users' profiles.
        private Boolean         twoFactorEnabled;
        private Boolean         oauth42Linked;
        //private UserStatsDto    stats;
}
