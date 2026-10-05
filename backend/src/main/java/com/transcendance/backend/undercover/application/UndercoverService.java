package com.transcendance.backend.undercover.application;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.transcendance.backend.undercover.domain.UndercoverException;
import com.transcendance.backend.undercover.domain.UndercoverGameMode;
import com.transcendance.backend.undercover.domain.UndercoverMatchStatus;
import com.transcendance.backend.undercover.domain.UndercoverPhase;
import com.transcendance.backend.undercover.domain.UndercoverRole;
import com.transcendance.backend.undercover.domain.UndercoverRoleDealer;
import com.transcendance.backend.undercover.domain.UndercoverRoleDealer.SeatAssignment;
import com.transcendance.backend.undercover.domain.UndercoverRules;
import com.transcendance.backend.undercover.domain.UndercoverTieBreaker;
import com.transcendance.backend.undercover.domain.UndercoverTieBreaker.TieOutcome;
import com.transcendance.backend.undercover.domain.UndercoverTurnOrder;
import com.transcendance.backend.undercover.domain.UndercoverVoteTally;
import com.transcendance.backend.undercover.domain.UndercoverVoteTally.VoteResult;
import com.transcendance.backend.undercover.domain.UndercoverWinCondition;
import com.transcendance.backend.undercover.domain.UndercoverWordMatcher;
import com.transcendance.backend.undercover.domain.UndercoverWordPair;
import com.transcendance.backend.undercover.entity.UndercoverClueEntity;
import com.transcendance.backend.undercover.entity.UndercoverMatchEntity;
import com.transcendance.backend.undercover.entity.UndercoverParticipantEntity;
import com.transcendance.backend.undercover.entity.UndercoverPlayerEntity;
import com.transcendance.backend.undercover.entity.UndercoverVoteEntity;
import com.transcendance.backend.undercover.entity.UndercoverWordPairEntity;
import com.transcendance.backend.undercover.entity.repository.UndercoverClueRepository;
import com.transcendance.backend.undercover.entity.repository.UndercoverMatchRepository;
import com.transcendance.backend.undercover.entity.repository.UndercoverPlayerRepository;
import com.transcendance.backend.undercover.entity.repository.UndercoverVoteRepository;
import com.transcendance.backend.undercover.entity.repository.UndercoverWordPairRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UndercoverService {

    public static final int MAX_CLUE_LENGTH = 50;

    public static final int MAX_CHAT_LENGTH = 300;

    private final SecureRandom random = new SecureRandom();

    private final UndercoverMatchRepository matchRepository;
    private final UndercoverPlayerRepository playerRepository;
    private final UndercoverWordPairRepository wordPairRepository;
    private final UndercoverClueRepository clueRepository;
    private final UndercoverVoteRepository voteRepository;
    private final UndercoverRoleDealer dealer;
    private final UndercoverTurnOrder turnOrder;
    private final UndercoverVoteTally voteTally;
    private final UndercoverTieBreaker tieBreaker;
    private final UndercoverWinCondition winCondition;
    private final UndercoverWordMatcher wordMatcher;
    private final Clock clock;

    /**
     * Seats the players, deals the roles and the words, and opens the first
     * description phase.
     *
     * Does not tell anyone their word: that is for the player channel, once
     * the match is saved.
     *
     * @param userIds     the players to seat, in any order - seats are drawn
     * @param mode        the mode the match is labelled with
     * @param customRules     the rules to play by for CUSTOM; ignored otherwise,
     *                        since a preset mode brings its own
     * @param categories      the seeded categories to draw the words from,
     *                        empty for any
     * @param customWordPairs the host's own pairs; when not empty, the words
     *                        are drawn from them and the categories are ignored
     * @return the saved match, participants included
     * @throws UndercoverException.InvalidRules    CUSTOM without rules, or rules
     *                                             that cannot seat these players
     * @throws UndercoverException.DuplicatePlayer the same player listed twice
     * @throws UndercoverException.AlreadyInMatch  a player still has a match in
     *                                             progress
     * @throws UndercoverException.NoWordPair      no word pair to draw from
     */
    @Transactional
    public UndercoverMatchEntity createMatch(List<UUID> userIds, UndercoverGameMode mode, UndercoverRules customRules,
            List<String> categories, List<UndercoverWordPair> customWordPairs) {
        UndercoverRules rules = rulesFor(mode, customRules);

        for (UUID userId : userIds) {
            if (matchRepository.existsActiveMatch(userId)) {
                throw new UndercoverException.AlreadyInMatch(userId);
            }
        }

        UndercoverWordPair wordPair = drawWordPair(categories, customWordPairs);

        List<SeatAssignment> assignments = dealer.dealAssignment(userIds, rules);

        UndercoverMatchEntity match = new UndercoverMatchEntity();
        match.setGameMode(mode);
        match.setRules(rules);
        match.setAgentsWord(wordPair.agentsWord());
        match.setUndercoverWord(wordPair.undercoverWord());

        Map<UUID, UndercoverPlayerEntity> players = getOrCreatePlayers(userIds);

        for (SeatAssignment assignment : assignments) {
            UndercoverParticipantEntity participant = new UndercoverParticipantEntity();
            participant.setMatch(match);
            participant.setPlayer(players.get(assignment.userId()));
            participant.setRole(assignment.role());
            participant.setSeatOrder(assignment.seatOrder());
            match.getParticipants().add(participant);
        }

        List<Integer> seats = assignments.stream().map(SeatAssignment::seatOrder).toList();
        match.setCurrentSpeakerSeat(turnOrder.firstSpeaker(seats)
                .orElseThrow(() -> new IllegalStateException("A dealt table has no seat")));

        Instant now = clock.instant();
        match.startPhase(UndercoverPhase.DESCRIPTION, now.plus(rules.durationOf(UndercoverPhase.DESCRIPTION)));

        return matchRepository.save(match);
    }


    /**
     * The player whose turn it is says his word, and the table moves on.
     *
     * @param word the clue, trimmed before it is stored
     * @return the match, moved to the next speaker - or to the vote, or to the
     *         next round, when this was the last speaker of a round
     * @throws UndercoverException.MatchNotFound    no match carries this id
     * @throws UndercoverException.MatchAlreadyOver the match is decided
     * @throws UndercoverException.NotAPlayer       the user holds no seat here
     * @throws UndercoverException.PlayerEliminated the user was voted out
     * @throws UndercoverException.WrongPhase       the match is not describing
     * @throws UndercoverException.NotYourTurn      another seat is speaking
     * @throws UndercoverException.InvalidClue      blank, too long, or the
     *                                              player's own secret word
     */
    @Transactional
    public UndercoverMatchEntity submitClue(long matchId, UUID userId, String word) {
        UndercoverMatchEntity match = lockMatch(matchId);
        UndercoverParticipantEntity speaker = activeParticipant(match, userId);
        requirePhase(match, UndercoverPhase.DESCRIPTION);

        if (match.getCurrentSpeakerSeat() == null || speaker.getSeatOrder() != match.getCurrentSpeakerSeat()) {
            throw new UndercoverException.NotYourTurn();
        }

        String clue = validateClue(match, speaker, word);
        clueRepository.save(new UndercoverClueEntity(match, match.getCurrentRound(), speaker, clue));

        advanceSpeaker(match);
        return match;
    }

    /**
     * Hands the turn to the next living seat, or closes the round.
     *
     * Also what a speaker whose time ran out goes through: he simply passes.
     */
    void advanceSpeaker(UndercoverMatchEntity match) {
        OptionalInt next = turnOrder.nextSpeaker(aliveSeats(match), match.getCurrentSpeakerSeat());
        if (next.isPresent()) {
            match.setCurrentSpeakerSeat(next.getAsInt());
            startPhase(match, UndercoverPhase.DESCRIPTION);
            return;
        }

        match.setRoundsSinceLastVote(match.getRoundsSinceLastVote() + 1);
        if (match.getRoundsSinceLastVote() >= match.getRules().getDescriptionRounds()) {
            openVote(match);
        } else {
            startDescriptionRound(match);
        }
    }

    /**
     * A living player votes against a seat.
     *
     * The vote is counted as soon as every living player has voted; otherwise
     * it waits for the others, or for the clock.
     *
     * @param targetSeat the seat voted against
     * @return the match, unchanged while ballots are missing, otherwise moved
     *         on: a revote, Mr White's guess, the next round, or the end
     * @throws UndercoverException.MatchNotFound     no match carries this id
     * @throws UndercoverException.MatchAlreadyOver  the match is decided
     * @throws UndercoverException.NotAPlayer        the user holds no seat here
     * @throws UndercoverException.PlayerEliminated  the user was voted out
     * @throws UndercoverException.WrongPhase        the match is not voting
     * @throws UndercoverException.AlreadyVoted      the user voted in this attempt
     * @throws UndercoverException.InvalidVoteTarget no living seat there, the
     *                                               voter himself, or a seat
     *                                               outside the tie of a revote
     */
    @Transactional
    public UndercoverMatchEntity submitVote(long matchId, UUID userId, int targetSeat) {
        UndercoverMatchEntity match = lockMatch(matchId);
        UndercoverParticipantEntity voter = activeParticipant(match, userId);
        requirePhase(match, UndercoverPhase.VOTE);

        if (voteRepository.hasVoted(match.getUndercoverMatchId(), match.getCurrentRound(),
                match.getVoteAttempt(), voter.getId())) {
            throw new UndercoverException.AlreadyVoted();
        }

        UndercoverParticipantEntity target = validateVoteTarget(match, voter, targetSeat);
        voteRepository.save(new UndercoverVoteEntity(match, match.getCurrentRound(), match.getVoteAttempt(),
                voter, target));

        long ballots = voteRepository.countAttempt(match.getUndercoverMatchId(), match.getCurrentRound(),
                match.getVoteAttempt());
        if (ballots >= aliveSeats(match).size()) {
            resolveVote(match);
        }
        return match;
    }

    /**
     * Counts the ballots of the current attempt and acts on the result.
     *
     * Also what the clock calls once the vote runs out of time: the players who
     * did not vote are counted as abstaining.
     */
    void resolveVote(UndercoverMatchEntity match) {
        VoteResult result = voteTally.tally(currentBallotTargets(match));

        switch (result) {
            case VoteResult.Eliminated eliminated -> eliminate(match, eliminated.seat());
            case VoteResult.NoVotes noVotes -> closeVote(match);
            case VoteResult.Tie tie -> {
                switch (tieBreaker.resolve(tie, match.getVoteAttempt(), match.getRules())) {
                    case TieOutcome.Revote revote -> {
                        match.setVoteAttempt(match.getVoteAttempt() + 1);
                        startPhase(match, UndercoverPhase.VOTE);
                    }
                    case TieOutcome.Eliminated drawn -> eliminate(match, drawn.seat());
                    case TieOutcome.NobodyOut nobody -> closeVote(match);
                }
            }
        }
    }

    /**
     * The Mr White who was just voted out tries to name the agents' word.
     *
     * Case, accents and stray spaces do not count. A right guess wins the match
     * for the impostors on the spot; a wrong one lets the game carry on as if
     * he had simply been eliminated.
     *
     * @param guess the word he names
     * @return the match, either over with MR_WHITE_WIN, or moved on - to the
     *         next round, or to the end if his elimination decided it
     * @throws UndercoverException.MatchNotFound    no match carries this id
     * @throws UndercoverException.MatchAlreadyOver the match is decided
     * @throws UndercoverException.WrongPhase       no guess is awaited
     * @throws UndercoverException.NotAPlayer       the user holds no seat here
     * @throws UndercoverException.NotTheGuesser    the user is not the Mr White
     *                                              just voted out
     * @throws UndercoverException.InvalidGuess     the guess is blank
     */
    @Transactional
    public UndercoverMatchEntity submitMrWhiteGuess(long matchId, UUID userId, String guess) {
        UndercoverMatchEntity match = lockMatch(matchId);
        requirePhase(match, UndercoverPhase.MR_WHITE_GUESS);

        UndercoverParticipantEntity participant = match.getParticipants().stream()
                .filter(p -> p.getPlayer().getUserId().equals(userId))
                .findFirst()
                .orElseThrow(UndercoverException.NotAPlayer::new);
        if (!participant.equals(guesser(match))) {
            throw new UndercoverException.NotTheGuesser();
        }
        // Normalized, since punctuation is ignored: "--" names no word at all.
        if (wordMatcher.normalize(guess).isEmpty()) {
            throw new UndercoverException.InvalidGuess("La proposition ne peut pas être vide");
        }

        resolveMrWhiteGuess(match, guess);
        return match;
    }

    /**
     * Settles the guess.
     *
     * Also what the clock calls once Mr White runs out of time, with a null
     * guess - which never matches.
     */
    void resolveMrWhiteGuess(UndercoverMatchEntity match, String guess) {
        if (wordMatcher.matches(guess, match.getAgentsWord())) {
            finish(match, UndercoverMatchStatus.MR_WHITE_WIN);
            return;
        }
        // His vote was already spent when he was eliminated.
        continueAfterVote(match);
    }

    /**
     * The Mr White the match is waiting on: the one eliminated in the current
     * round.
     *
     * One vote eliminates one player, and the round moves on right after, so
     * there is never more than one.
     */
    private UndercoverParticipantEntity guesser(UndercoverMatchEntity match) {
        return match.getParticipants().stream()
                .filter(p -> p.getRole() == UndercoverRole.MR_WHITE && p.isEliminated()
                        && Integer.valueOf(match.getCurrentRound()).equals(p.getEliminatedRound()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "Waiting on a guess, but no Mr White was eliminated this round"));
    }

    /**
     * The player walks away from the table.
     *
     * Still in the game, he is eliminated on the spot and his match counts as a
     * loss; already eliminated, he only stops following it. Either way he is
     * free to join another match afterwards. Leaving twice changes nothing.
     *
     * @return the match, moved on if his departure decided anything - the turn
     *         he was holding, a vote now complete, or the end
     * @throws UndercoverException.MatchNotFound    no match carries this id
     * @throws UndercoverException.MatchAlreadyOver the match is decided
     * @throws UndercoverException.NotAPlayer       the user holds no seat here
     */
    @Transactional
    public UndercoverMatchEntity leaveMatch(long matchId, UUID userId) {
        UndercoverMatchEntity match = lockMatch(matchId);
        UndercoverParticipantEntity leaver = match.getParticipants().stream()
                .filter(p -> p.getPlayer().getUserId().equals(userId))
                .findFirst()
                .orElseThrow(UndercoverException.NotAPlayer::new);
        if (leaver.isLeftMatch()) {
            return match;
        }
        leaver.setLeftMatch(true);

        if (leaver.isEliminated()) {
            // The only thing an eliminated player can still hold up is his own
            // guess: he leaves without giving one.
            if (match.getPhase() == UndercoverPhase.MR_WHITE_GUESS && leaver.equals(guesser(match))) {
                resolveMrWhiteGuess(match, null);
            }
            return match;
        }

        leaver.setForfeited(true);
        leaver.setEliminated(true);
        leaver.setEliminatedRound(match.getCurrentRound());

        switch (match.getPhase()) {
            case DESCRIPTION -> {
                if (!endIfDecided(match) && leaver.getSeatOrder() == match.getCurrentSpeakerSeat()) {
                    advanceSpeaker(match);
                }
            }
            case VOTE -> {
                voteRepository.deleteBallotsInvolving(match, match.getCurrentRound(), match.getVoteAttempt(), leaver);
                if (!endIfDecided(match) && voteRepository.countAttempt(match.getUndercoverMatchId(),
                        match.getCurrentRound(), match.getVoteAttempt()) >= aliveSeats(match).size()) {
                    resolveVote(match);
                }
            }
            // The guess decides what comes next, and checks for a win once it is
            // settled.
            case MR_WHITE_GUESS, END -> {
            }
        }
        return match;
    }

    /**
     * Checks a chat message and tells who it goes to.
     *
     * Nothing is stored: messages only travel over the socket. Eliminated
     * players can still read the table, but no longer write to it.
     *
     * @return the message, with every player still following the match as
     *         recipients
     * @throws UndercoverException.MatchNotFound      no match carries this id
     * @throws UndercoverException.MatchAlreadyOver   the match is decided
     * @throws UndercoverException.NotAPlayer         the user holds no seat here,
     *                                                or left it
     * @throws UndercoverException.PlayerEliminated   the user was voted out
     * @throws UndercoverException.InvalidChatMessage blank or too long
     */
    @Transactional(readOnly = true)
    public UndercoverChatMessage prepareChatMessage(long matchId, UUID userId, String text) {
        UndercoverMatchEntity match = matchRepository.findByIdWithParticipants(matchId)
                .orElseThrow(() -> new UndercoverException.MatchNotFound(matchId));
        if (match.getStatus().isOver()) {
            throw new UndercoverException.MatchAlreadyOver(match.getStatus());
        }
        UndercoverParticipantEntity author = match.getParticipants().stream()
                .filter(p -> p.getPlayer().getUserId().equals(userId) && !p.isLeftMatch())
                .findFirst()
                .orElseThrow(UndercoverException.NotAPlayer::new);
        if (author.isEliminated()) {
            throw new UndercoverException.PlayerEliminated();
        }

        String message = text == null ? "" : text.trim();
        if (message.isEmpty()) {
            throw new UndercoverException.InvalidChatMessage("Le message ne peut pas être vide");
        }
        if (message.length() > MAX_CHAT_LENGTH) {
            throw new UndercoverException.InvalidChatMessage(
                    "Un message fait au plus " + MAX_CHAT_LENGTH + " caractères");
        }

        List<UUID> recipients = match.getParticipants().stream()
                .filter(p -> !p.isLeftMatch())
                .map(p -> p.getPlayer().getUserId())
                .toList();
        return new UndercoverChatMessage(matchId, author.getSeatOrder(), userId, message, clock.instant(),
                recipients);
    }

    /**
     * The matches whose clock has run out, for the scheduled sweep.
     */
    @Transactional(readOnly = true)
    public List<Long> expiredMatchIds() {
        return matchRepository.findExpiredPhaseIds(clock.instant());
    }

    /**
     * Moves a match whose clock ran out: the speaker passes his turn, the
     * missing voters abstain, a silent Mr White fails his guess.
     *
     * Checked again under the lock, since a player may have acted after the
     * sweep read the deadline.
     *
     * @return whether anything changed
     */
    @Transactional
    public boolean expirePhase(long matchId) {
        UndercoverMatchEntity match = matchRepository.findByIdForUpdate(matchId).orElse(null);
        if (match == null || match.getStatus().isOver()) {
            return false;
        }
        Instant deadline = match.getPhaseDeadline();
        if (deadline == null || deadline.isAfter(clock.instant())) {
            return false;
        }

        switch (match.getPhase()) {
            case DESCRIPTION -> advanceSpeaker(match);
            case VOTE -> resolveVote(match);
            case MR_WHITE_GUESS -> resolveMrWhiteGuess(match, null);
            case END -> match.startPhase(UndercoverPhase.END, null);
        }
        return true;
    }

    /**
     * Ends the match if a departure decided it, outside of any vote.
     *
     * @return whether the match is now over
     */
    private boolean endIfDecided(UndercoverMatchEntity match) {
        UndercoverMatchStatus status = currentStatus(match);
        if (status.isOver()) {
            finish(match, status);
            return true;
        }
        return false;
    }

    private UndercoverMatchStatus currentStatus(UndercoverMatchEntity match) {
        return winCondition.checkWinCondition(
                aliveCount(match, UndercoverRole.AGENT),
                aliveCount(match, UndercoverRole.UNDERCOVER),
                aliveCount(match, UndercoverRole.MR_WHITE),
                match.getVotesUsed(),
                match.getRules());
    }

    private void openVote(UndercoverMatchEntity match) {
        match.setCurrentSpeakerSeat(null);
        match.setVoteAttempt(0);
        startPhase(match, UndercoverPhase.VOTE);
    }

    private void startDescriptionRound(UndercoverMatchEntity match) {
        match.setCurrentRound(match.getCurrentRound() + 1);
        match.setCurrentSpeakerSeat(turnOrder.firstSpeaker(aliveSeats(match))
                .orElseThrow(() -> new IllegalStateException("No living seat left to describe")));
        startPhase(match, UndercoverPhase.DESCRIPTION);
    }

    private void eliminate(UndercoverMatchEntity match, int seat) {
        UndercoverParticipantEntity out = participantAt(match, seat)
                .orElseThrow(() -> new IllegalStateException("No participant at seat " + seat));
        out.setEliminated(true);
        out.setEliminatedRound(match.getCurrentRound());

        if (out.getRole() == UndercoverRole.MR_WHITE) {
            match.setVotesUsed(match.getVotesUsed() + 1);
            match.setVoteAttempt(0);
            startPhase(match, UndercoverPhase.MR_WHITE_GUESS);
            return;
        }
        closeVote(match);
    }

    /**
     * Spends the vote - a vote and its revotes count as one - then either ends
     * the match or opens the next round.
     */
    private void closeVote(UndercoverMatchEntity match) {
        match.setVotesUsed(match.getVotesUsed() + 1);
        match.setVoteAttempt(0);
        continueAfterVote(match);
    }

    /**
     * Ends the match if it is decided, otherwise starts a new round of
     * descriptions.
     *
     * Kept apart from closeVote because Mr White's guess lands here too, once
     * his vote was already spent.
     */
    void continueAfterVote(UndercoverMatchEntity match) {
        UndercoverMatchStatus status = currentStatus(match);

        if (status.isOver()) {
            finish(match, status);
            return;
        }
        match.setRoundsSinceLastVote(0);
        startDescriptionRound(match);
    }

    /**
     * Closes the match and records the result on every player of the table,
     * eliminated ones included: a team wins or loses as a whole. A player who
     * walked away mid-game loses whatever his team did.
     */
    void finish(UndercoverMatchEntity match, UndercoverMatchStatus status) {
        match.setStatus(status);
        match.setCurrentSpeakerSeat(null);
        match.startPhase(UndercoverPhase.END, null);
        match.setEndedAt(clock.instant());

        boolean agentsWon = status == UndercoverMatchStatus.AGENTS_WIN;
        for (UndercoverParticipantEntity participant : match.getParticipants()) {
            boolean isAgent = participant.getRole() == UndercoverRole.AGENT;
            if (!participant.isForfeited() && isAgent == agentsWon) {
                participant.getPlayer().incrementWins();
            } else {
                participant.getPlayer().incrementLosses();
            }
        }
    }

    private void startPhase(UndercoverMatchEntity match, UndercoverPhase phase) {
        match.startPhase(phase, clock.instant().plus(match.getRules().durationOf(phase)));
    }

    private UndercoverMatchEntity lockMatch(long matchId) {
        UndercoverMatchEntity match = matchRepository.findByIdForUpdate(matchId)
                .orElseThrow(() -> new UndercoverException.MatchNotFound(matchId));
        if (match.getStatus().isOver()) {
            throw new UndercoverException.MatchAlreadyOver(match.getStatus());
        }
        return match;
    }

    /**
     * The seat this user holds, provided he is still in the game.
     */
    private UndercoverParticipantEntity activeParticipant(UndercoverMatchEntity match, UUID userId) {
        UndercoverParticipantEntity participant = match.getParticipants().stream()
                .filter(p -> p.getPlayer().getUserId().equals(userId))
                .findFirst()
                .orElseThrow(UndercoverException.NotAPlayer::new);
        if (participant.isEliminated()) {
            throw new UndercoverException.PlayerEliminated();
        }
        return participant;
    }

    private void requirePhase(UndercoverMatchEntity match, UndercoverPhase expected) {
        if (match.getPhase() != expected) {
            throw new UndercoverException.WrongPhase(expected, match.getPhase());
        }
    }

    private String validateClue(UndercoverMatchEntity match, UndercoverParticipantEntity speaker, String word) {
        String clue = word == null ? "" : word.trim();
        if (clue.isEmpty()) {
            throw new UndercoverException.InvalidClue("L'indice ne peut pas être vide");
        }
        if (clue.length() > MAX_CLUE_LENGTH) {
            throw new UndercoverException.InvalidClue(
                    "Un indice fait au plus " + MAX_CLUE_LENGTH + " caractères");
        }
        // Saying the word itself would end the game for everyone. Mr White has
        // no word to give away.
        String ownWord = switch (speaker.getRole()) {
            case AGENT -> match.getAgentsWord();
            case UNDERCOVER -> match.getUndercoverWord();
            case MR_WHITE -> null;
        };
        if (ownWord != null && wordMatcher.matches(clue, ownWord)) {
            throw new UndercoverException.InvalidClue("Tu ne peux pas dire ton propre mot");
        }
        return clue;
    }

    private UndercoverParticipantEntity validateVoteTarget(UndercoverMatchEntity match,
            UndercoverParticipantEntity voter, int targetSeat) {
        UndercoverParticipantEntity target = participantAt(match, targetSeat)
                .filter(p -> !p.isEliminated())
                .orElseThrow(() -> new UndercoverException.InvalidVoteTarget(
                        "Aucun joueur en vie à la place " + targetSeat));
        if (target.getSeatOrder() == voter.getSeatOrder()) {
            throw new UndercoverException.InvalidVoteTarget("Tu ne peux pas voter contre toi-même");
        }
        if (match.getVoteAttempt() > 0 && !tiedSeats(match).contains(targetSeat)) {
            throw new UndercoverException.InvalidVoteTarget(
                    "Le second vote ne concerne que les joueurs à égalité : " + tiedSeats(match));
        }
        return target;
    }

    /**
     * The seats a revote is held between.
     *
     * Recounted from the attempt that tied rather than stored: the ballots are
     * already persisted, and a restart mid-revote loses nothing.
     */
    private List<Integer> tiedSeats(UndercoverMatchEntity match) {
        List<Integer> previousTargets = voteRepository.findAttempt(match.getUndercoverMatchId(),
                match.getCurrentRound(), match.getVoteAttempt() - 1).stream()
                .map(vote -> vote.getTarget().getSeatOrder())
                .toList();
        return voteTally.tally(previousTargets) instanceof VoteResult.Tie tie ? tie.seats() : List.of();
    }

    private List<Integer> currentBallotTargets(UndercoverMatchEntity match) {
        return voteRepository.findAttempt(match.getUndercoverMatchId(), match.getCurrentRound(),
                match.getVoteAttempt()).stream()
                .map(vote -> vote.getTarget().getSeatOrder())
                .toList();
    }

    private List<Integer> aliveSeats(UndercoverMatchEntity match) {
        return match.getParticipants().stream()
                .filter(p -> !p.isEliminated())
                .map(UndercoverParticipantEntity::getSeatOrder)
                .toList();
    }

    private int aliveCount(UndercoverMatchEntity match, UndercoverRole role) {
        return (int) match.getParticipants().stream()
                .filter(p -> !p.isEliminated() && p.getRole() == role)
                .count();
    }

    private Optional<UndercoverParticipantEntity> participantAt(UndercoverMatchEntity match, int seat) {
        return match.getParticipants().stream()
                .filter(p -> p.getSeatOrder() == seat)
                .findFirst();
    }

    private UndercoverRules rulesFor(UndercoverGameMode mode, UndercoverRules customRules) {
        if (mode != UndercoverGameMode.CUSTOM) {
            return UndercoverRules.defaultsFor(mode);
        }
        if (customRules == null) {
            throw new UndercoverException.InvalidRules("Une partie personnalisée doit avoir ses règles");
        }
        return customRules;
    }

    /**
     * The words the match is played on: one of the host's pairs if he wrote
     * any, a seeded one otherwise.
     *
     * A custom pair is flipped half the time, so its author cannot tell from
     * the order he typed it in which side the undercover got.
     */
    private UndercoverWordPair drawWordPair(List<String> categories, List<UndercoverWordPair> customWordPairs) {
        if (customWordPairs != null && !customWordPairs.isEmpty()) {
            UndercoverWordPair pair = customWordPairs.get(random.nextInt(customWordPairs.size()));
            return random.nextBoolean() ? pair : new UndercoverWordPair(pair.undercoverWord(), pair.agentsWord());
        }

        Optional<UndercoverWordPairEntity> seeded = categories == null || categories.isEmpty()
                ? wordPairRepository.findRandomWordPair()
                : wordPairRepository.findRandomWordPairInCategories(categories);
        return seeded
                .map(entity -> new UndercoverWordPair(entity.getAgentsWord(), entity.getUndercoverWord()))
                .orElseThrow(UndercoverException.NoWordPair::new);
    }

    /**
     * The player rows of these users, created for those who never played.
     *
     * Read in one query rather than one per player.
     */
    private Map<UUID, UndercoverPlayerEntity> getOrCreatePlayers(List<UUID> userIds) {
        Map<UUID, UndercoverPlayerEntity> players = new HashMap<>(playerRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(UndercoverPlayerEntity::getUserId, Function.identity())));

        for (UUID userId : userIds) {
            if (!players.containsKey(userId)) {
                UndercoverPlayerEntity player = new UndercoverPlayerEntity();
                player.setUserId(userId);
                players.put(userId, playerRepository.save(player));
            }
        }
        return players;
    }
}
