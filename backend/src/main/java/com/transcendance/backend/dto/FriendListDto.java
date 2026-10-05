package com.transcendance.backend.dto;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

import com.transcendance.backend.model.FriendshipStatus;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class FriendListDto {

    /** The other end of the relationship: the id every friend-list route takes. */
    private UUID idUser;
    private String userName;
    private LocalDateTime createAt;
    private FriendshipStatus status;

    /** Who opened the relationship: tells a received request from a sent one. */
    private UUID senderId;

    /**
     * Whether this friend has an open session right now.
     *
     * The starting picture the client would otherwise be missing: presence
     * changes travel over WebSocket, but a page that has just been opened has
     * seen none of them yet and would show every friend as away.
     */
    private boolean online;

    /** When he was last seen. Only meaningful while online is false. */
    private Instant lastSeen;

    /** Who blocked whom: only the blocker sees the row in his blocked list. */
    private UUID blockerId;
}
