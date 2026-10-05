package com.transcendance.backend.undercover.api;

import java.util.Map;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.transcendance.backend.model.User;
import com.transcendance.backend.undercover.api.dto.UndercoverRequests;
import com.transcendance.backend.undercover.api.dto.UndercoverStatsDto;
import com.transcendance.backend.undercover.application.UndercoverPlayerChannel;
import com.transcendance.backend.undercover.application.UndercoverQueryService;
import com.transcendance.backend.undercover.application.UndercoverService;
import com.transcendance.backend.undercover.domain.UndercoverException;
import com.transcendance.backend.undercover.entity.UndercoverPlayerEntity;

import lombok.RequiredArgsConstructor;

/**
 * Playing a match.
 *
 * Every action answers with the caller's own view of the match, and pushes
 * every other player his - only once the service has committed, so nobody sees
 * a state a rollback would take back.
 */
@RequiredArgsConstructor
@RequestMapping("/api/undercover")
@RestController
public class UndercoverMatchController {

    private static final Logger log = LogManager.getLogger(UndercoverMatchController.class);

    private final UndercoverService undercoverService;
    private final UndercoverQueryService queryService;
    private final UndercoverPlayerChannel playerChannel;
    private final UndercoverViews views;

    /**
     * The match this user still sits at. 404 when there is none, which is the
     * normal case.
     */
    @GetMapping("/matches/me")
    public ResponseEntity<?> myMatch(@AuthenticationPrincipal User user) {
        if (user == null) {
            return unauthenticated("match lookup");
        }
        return queryService.activeSnapshot(user.getIdUser())
                .flatMap(snapshot -> views.matchFor(snapshot, user.getIdUser()))
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(404).body(Map.of("error", "Aucune partie d'Undercover en cours")));
    }

    @PostMapping("/matches/{matchId}/clues")
    public ResponseEntity<?> submitClue(@AuthenticationPrincipal User user, @PathVariable long matchId,
            @RequestBody UndercoverRequests.SubmitClue request) {
        if (user == null) {
            return unauthenticated("clue");
        }
        undercoverService.submitClue(matchId, user.getIdUser(), request.word());
        return afterAction(matchId, user.getIdUser());
    }

    @PostMapping("/matches/{matchId}/votes")
    public ResponseEntity<?> submitVote(@AuthenticationPrincipal User user, @PathVariable long matchId,
            @RequestBody UndercoverRequests.SubmitVote request) {
        if (user == null) {
            return unauthenticated("vote");
        }
        if (request.targetSeat() == null) {
            throw new UndercoverException.InvalidVoteTarget("Un vote doit viser une place");
        }
        undercoverService.submitVote(matchId, user.getIdUser(), request.targetSeat());
        return afterAction(matchId, user.getIdUser());
    }

    @PostMapping("/matches/{matchId}/guess")
    public ResponseEntity<?> submitGuess(@AuthenticationPrincipal User user, @PathVariable long matchId,
            @RequestBody UndercoverRequests.SubmitGuess request) {
        if (user == null) {
            return unauthenticated("guess");
        }
        undercoverService.submitMrWhiteGuess(matchId, user.getIdUser(), request.guess());
        return afterAction(matchId, user.getIdUser());
    }

    /**
     * Walks away from the table. The leaver gets nothing back to display: he no
     * longer follows the match.
     */
    @PostMapping("/matches/{matchId}/leave")
    public ResponseEntity<?> leaveMatch(@AuthenticationPrincipal User user, @PathVariable long matchId) {
        if (user == null) {
            return unauthenticated("leave");
        }
        undercoverService.leaveMatch(matchId, user.getIdUser());
        playerChannel.matchUpdated(matchId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Broadcast only, author included: the client shows a line once it comes
     * back from the socket, in the same order for everyone.
     */
    @PostMapping("/matches/{matchId}/chat")
    public ResponseEntity<?> sendChat(@AuthenticationPrincipal User user, @PathVariable long matchId,
            @RequestBody UndercoverRequests.SendChat request) {
        if (user == null) {
            return unauthenticated("chat");
        }
        playerChannel.chatMessage(undercoverService.prepareChatMessage(matchId, user.getIdUser(), request.text()));
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/stats/me")
    public ResponseEntity<?> myStats(@AuthenticationPrincipal User user) {
        if (user == null) {
            return unauthenticated("stats");
        }
        UndercoverPlayerEntity player = queryService.stats(user.getIdUser());
        return ResponseEntity.ok(new UndercoverStatsDto(player.getWins(), player.getLosses()));
    }

    private ResponseEntity<?> afterAction(long matchId, UUID userId) {
        playerChannel.matchUpdated(matchId);
        return ResponseEntity.ok(views.matchFor(queryService.snapshot(matchId), userId)
                .orElseThrow(UndercoverException.NotAPlayer::new));
    }

    private ResponseEntity<?> unauthenticated(String action) {
        log.warn("Undercover {} refused: no authenticated user", action);
        return ResponseEntity.status(401).body(Map.of("error", "Non authentifié"));
    }
}
