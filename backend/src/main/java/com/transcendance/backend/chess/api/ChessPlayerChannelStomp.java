package com.transcendance.backend.chess.api;

import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.user.SimpUserRegistry;
import org.springframework.stereotype.Component;

import com.transcendance.backend.chess.api.dto.ChessInvitationDto;
import com.transcendance.backend.chess.api.dto.ChessMatchDto;
import com.transcendance.backend.chess.api.dto.ChessMovePlayedDto;
import com.transcendance.backend.chess.application.ChessPlayerChannel;
import com.transcendance.backend.chess.domain.ChessInvitation;
import com.transcendance.backend.chess.domain.ChessInvitationState;
import com.transcendance.backend.chess.persistence.entity.ChessMatchEntity;

import lombok.RequiredArgsConstructor;

/**
 * STOMP implementation of the ChessPlayerChannel port.
 *
 * The only class in the module aware of SimpUserRegistry and
 * SimpMessagingTemplate.
 */
@Component
@RequiredArgsConstructor
public class ChessPlayerChannelStomp implements ChessPlayerChannel {

    private static final String INVITATION_DESTINATION = "/queue/chess.invitations";

    private static final String MATCH_DESTINATION = "/queue/chess.matches";

    private static final String MOVE_DESTINATION = "/queue/chess.moves";

    private static final Logger log = LogManager.getLogger(ChessPlayerChannelStomp.class);

    private final SimpUserRegistry simpUserRegistry;
    private final SimpMessagingTemplate messagingTemplate;

    @Override
    public boolean isOnline(UUID userId) {
        return simpUserRegistry.getUser(userId.toString()) != null;
    }

    @Override
    public void invitationSent(UUID toUserId, ChessInvitation invitation, String fromUsername) {
        push(toUserId, INVITATION_DESTINATION, ChessInvitationDto.of(invitation, fromUsername, ChessInvitationState.PENDING));
    }

    @Override
    public void invitationDeclined(UUID toUserId, ChessInvitation invitation, String fromUsername) {
        push(toUserId, INVITATION_DESTINATION, ChessInvitationDto.of(invitation, fromUsername, ChessInvitationState.DECLINED));
    }

    @Override
    public void matchStarted(UUID toUserId, ChessMatchEntity match) {
        push(toUserId, MATCH_DESTINATION, ChessMatchDto.of(match));
    }

    @Override
    public void movePlayed(UUID toUserId, ChessMatchEntity match, String move) {
        push(toUserId, MOVE_DESTINATION, ChessMovePlayedDto.of(match, move));
    }

    @Override
    public void matchEnded(UUID toUserId, ChessMatchEntity match) {
        push(toUserId, MATCH_DESTINATION, ChessMatchDto.of(match));
    }

    private void push(UUID toUserId, String destination, Object payload) {
        try {
            messagingTemplate.convertAndSendToUser(toUserId.toString(), destination, payload);
        } catch (Exception e) {
            log.error("Chess payload could not be pushed to {} on {}: {}",
                    toUserId, destination, e.getMessage());
        }
    }
}
