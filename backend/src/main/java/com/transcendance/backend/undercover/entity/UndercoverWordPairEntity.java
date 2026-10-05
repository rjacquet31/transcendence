package com.transcendance.backend.undercover.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * The two words a match is played on: one for the agents, a close one for the
 * undercover.
 *
 * Seeded by data.sql, which is replayed on every startup - the unique
 * constraint is what gives its ON CONFLICT DO NOTHING something to conflict
 * on, without which each restart would insert every pair once more.
 */
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "undercover_word_pair", uniqueConstraints = @UniqueConstraint(name = "uk_undercover_word_pair", columnNames = {
        "undercover_word", "agents_word" }))
public class UndercoverWordPairEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "undercover_word_pair_id")
    private Long undercoverWordPairId;

    @Column(name = "undercover_word", nullable = false)
    private String undercoverWord;

    @Column(name = "agents_word", nullable = false)
    private String agentsWord;

    @Column(name = "category", nullable = false)
    private String category;

}
