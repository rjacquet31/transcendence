package com.transcendance.backend.undercover.entity;

import java.time.Instant;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "undercover_votes", uniqueConstraints = @UniqueConstraint(name = "uk_undercover_vote_once_per_attempt", columnNames = {
        "match_id", "round", "attempt", "voter_id" }))
public class UndercoverVoteEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "undercover_vote_id")
    private Long undercoverVoteId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "match_id")
    private UndercoverMatchEntity match;

    @Column(name = "round", nullable = false)
    private int round;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "voter_id")
    private UndercoverParticipantEntity voter;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "target_id")
    private UndercoverParticipantEntity target;

    @Column(name = "attempt", nullable = false)
    private int attempt = 0;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public UndercoverVoteEntity(UndercoverMatchEntity match, int round, int attempt,
            UndercoverParticipantEntity voter, UndercoverParticipantEntity target) {
        this.match = match;
        this.round = round;
        this.attempt = attempt;
        this.voter = voter;
        this.target = target;
    }
}
