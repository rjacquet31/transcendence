package com.transcendance.backend.chess.api.dto;

import java.util.UUID;

import com.transcendance.backend.chess.domain.ChessInvitation;
import com.transcendance.backend.chess.domain.ChessInvitationState;
import com.transcendance.backend.chess.domain.ChessSide;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ChessInvitationDto {
    private UUID invitationId;
    private UUID fromUserId;
    private String fromUsername;
    private ChessInvitationState state;
    /** Colour played by the sender; the invited user plays the other one. */
    private ChessSide fromSide;

    /**
     * Translates a domain invitation into the API contract.
     *
     * The username is not carried by the domain: it is supplied by the caller,
     * which either already holds the User or knows how to resolve it.
     */
    public static ChessInvitationDto of(ChessInvitation invitation, String fromUsername,
            ChessInvitationState state) {
        return new ChessInvitationDto(invitation.id(), invitation.fromUserId(), fromUsername, state,
                invitation.fromSide());
    }
}
