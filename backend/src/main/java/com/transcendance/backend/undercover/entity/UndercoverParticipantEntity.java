package com.transcendance.backend.undercover.entity;

import com.transcendance.backend.undercover.domain.UndercoverRole;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@Table(name = "undercover_participants", uniqueConstraints = @UniqueConstraint(columnNames = { "match_id",
        "player_id" }))
public class UndercoverParticipantEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "match_id")
    private UndercoverMatchEntity match;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "player_id")
    private UndercoverPlayerEntity player;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private UndercoverRole role;

    @Column(name = "seat_order", nullable = false)
    private int seatOrder;

    @Column(name = "eliminated", nullable = false)
    private boolean eliminated = false;

    @Column(name = "eliminated_round")
    private Integer eliminatedRound;

    /**
     * The player walked away from the table: he no longer sees the match, and
     * is free to join another one.
     */
    @Column(name = "left_match", nullable = false)
    private boolean leftMatch = false;

    /**
     * He walked away while still in the game. Counted as a loss whatever his
     * team ends up doing - leaving is not a way to collect a win.
     */
    @Column(name = "forfeited", nullable = false)
    private boolean forfeited = false;
}
