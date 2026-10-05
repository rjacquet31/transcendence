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
@Table(name = "undercover_clues", uniqueConstraints = @UniqueConstraint(name = "uk_undercover_clue_once_per_round", columnNames = {
        "match_id", "round", "participant_id" }))
public class UndercoverClueEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "undercover_clue_id")
    private Long undercoverClueId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "match_id")
    private UndercoverMatchEntity match;

    @Column(name = "round", nullable = false)
    private int round;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "participant_id")
    private UndercoverParticipantEntity participant;

    @Column(name = "word", nullable = false)
    private String word;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public UndercoverClueEntity(UndercoverMatchEntity match, int round,
            UndercoverParticipantEntity participant, String word) {
        this.match = match;
        this.round = round;
        this.participant = participant;
        this.word = word;
    }
}
