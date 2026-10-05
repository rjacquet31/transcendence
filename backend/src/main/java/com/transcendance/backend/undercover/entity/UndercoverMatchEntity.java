package com.transcendance.backend.undercover.entity;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.CreationTimestamp;

import com.transcendance.backend.undercover.domain.UndercoverMatchStatus;
import com.transcendance.backend.undercover.domain.UndercoverGameMode;
import com.transcendance.backend.undercover.domain.UndercoverPhase;
import com.transcendance.backend.undercover.domain.UndercoverRules;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@AllArgsConstructor
@Setter
@Getter
@Entity
@Table(name = "undercover_match", indexes = {
        // The phase sweep runs on a timer forever; without this it reads the
        // whole table every time to almost always find nothing.
        @Index(name = "idx_undercover_match_phase_deadline", columnList = "phase_deadline")
})
public class UndercoverMatchEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "undercover_match_id")
    private Long undercoverMatchId;

    @OneToMany(mappedBy = "match", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("seatOrder")
    private List<UndercoverParticipantEntity> participants = new ArrayList<>();

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private UndercoverMatchStatus status = UndercoverMatchStatus.IN_PROGRESS;

    @Column(name = "undercover_word")
    private String undercoverWord;

    @Column(name = "agents_word")
    private String agentsWord;

    @Column(name = "current_round", nullable = false)
    private int currentRound = 1;

    @Column(name = "phase", nullable = false)
    @Enumerated(EnumType.STRING)
    private UndercoverPhase phase = UndercoverPhase.DESCRIPTION;

    @Column(name = "current_speaker_seat")
    private Integer currentSpeakerSeat;

    @Column(name = "phase_deadline")
    private Instant phaseDeadline;

    @Column(name = "votes_used", nullable = false)
    private int votesUsed = 0;

    @Column(name = "rounds_since_last_vote", nullable = false)
    private int roundsSinceLastVote = 0;

    @Column(name = "vote_attempt")
    private int voteAttempt = 0;

    @Column(name = "game_mode")
    @Enumerated(EnumType.STRING)
    private UndercoverGameMode gameMode = UndercoverGameMode.CLASSIC;

    @Embedded
    private UndercoverRules rules;

    @Version
    private Long version;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "ended_at")
    private Instant endedAt;

    /**
     * Moves the match into a phase and starts its countdown.
     *
     * @param deadline when the phase expires, or null for a phase that is not
     *                 on a clock
     */
    public void startPhase(UndercoverPhase phase, Instant deadline) {
        this.phase = phase;
        this.phaseDeadline = deadline;
    }
}
