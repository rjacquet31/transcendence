package com.transcendance.backend.undercover.application;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.transcendance.backend.undercover.domain.UndercoverException;
import com.transcendance.backend.undercover.domain.UndercoverPhase;
import com.transcendance.backend.undercover.domain.UndercoverRole;
import com.transcendance.backend.undercover.domain.UndercoverVoteTally;
import com.transcendance.backend.undercover.domain.UndercoverVoteTally.VoteResult;
import com.transcendance.backend.undercover.entity.UndercoverMatchEntity;
import com.transcendance.backend.undercover.entity.UndercoverPlayerEntity;
import com.transcendance.backend.undercover.entity.repository.UndercoverClueRepository;
import com.transcendance.backend.undercover.entity.repository.UndercoverMatchRepository;
import com.transcendance.backend.undercover.entity.repository.UndercoverPlayerRepository;
import com.transcendance.backend.undercover.entity.repository.UndercoverVoteRepository;

import lombok.RequiredArgsConstructor;

/**
 * Reads matches for display. Never changes anything.
 */
@Service
@RequiredArgsConstructor
public class UndercoverQueryService {

    private final UndercoverMatchRepository matchRepository;
    private final UndercoverClueRepository clueRepository;
    private final UndercoverVoteRepository voteRepository;
    private final UndercoverPlayerRepository playerRepository;
    private final UndercoverVoteTally voteTally;

    /**
     * @throws UndercoverException.MatchNotFound no match carries this id
     */
    @Transactional(readOnly = true)
    public UndercoverMatchSnapshot snapshot(long matchId) {
        UndercoverMatchEntity match = matchRepository.findByIdWithParticipants(matchId)
                .orElseThrow(() -> new UndercoverException.MatchNotFound(matchId));
        long id = match.getUndercoverMatchId();
        int round = match.getCurrentRound();
        int attempt = match.getVoteAttempt();

        Set<Integer> votedSeats = match.getPhase() != UndercoverPhase.VOTE ? Set.of()
                : voteRepository.findAttempt(id, round, attempt).stream()
                        .map(vote -> vote.getVoter().getSeatOrder())
                        .collect(Collectors.toSet());

        List<Integer> tiedSeats = match.getPhase() != UndercoverPhase.VOTE || attempt == 0 ? List.of()
                : voteTally.tally(voteRepository.findAttempt(id, round, attempt - 1).stream()
                        .map(vote -> vote.getTarget().getSeatOrder())
                        .toList()) instanceof VoteResult.Tie tie ? tie.seats() : List.of();

        Integer guesserSeat = match.getPhase() != UndercoverPhase.MR_WHITE_GUESS ? null
                : match.getParticipants().stream()
                        .filter(p -> p.getRole() == UndercoverRole.MR_WHITE && p.isEliminated()
                                && Integer.valueOf(round).equals(p.getEliminatedRound()))
                        .map(p -> p.getSeatOrder())
                        .findFirst()
                        .orElse(null);

        return new UndercoverMatchSnapshot(match, clueRepository.findTranscript(id), votedSeats, tiedSeats,
                guesserSeat);
    }

    /**
     * The match this user still sits at, if any.
     */
    @Transactional(readOnly = true)
    public Optional<UndercoverMatchSnapshot> activeSnapshot(UUID userId) {
        return matchRepository.findActiveMatchId(userId).map(this::snapshot);
    }

    /**
     * @return this user's record, all zeros if he never played
     */
    @Transactional(readOnly = true)
    public UndercoverPlayerEntity stats(UUID userId) {
        return playerRepository.findById(userId).orElseGet(() -> {
            UndercoverPlayerEntity blank = new UndercoverPlayerEntity();
            blank.setUserId(userId);
            return blank;
        });
    }
}
