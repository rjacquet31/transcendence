package com.transcendance.backend.undercover.application;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.transcendance.backend.undercover.domain.UndercoverRoleDealer;
import com.transcendance.backend.undercover.domain.UndercoverTieBreaker;
import com.transcendance.backend.undercover.domain.UndercoverTurnOrder;
import com.transcendance.backend.undercover.domain.UndercoverVoteTally;
import com.transcendance.backend.undercover.domain.UndercoverWinCondition;
import com.transcendance.backend.undercover.domain.UndercoverWordMatcher;

@Configuration
public class UndercoverConfig {

    @Bean
    public UndercoverRoleDealer undercoverRoleDealer() {
        return new UndercoverRoleDealer();
    }

    @Bean
    public UndercoverTurnOrder undercoverTurnOrder() {
        return new UndercoverTurnOrder();
    }

    @Bean
    public UndercoverVoteTally undercoverVoteTally() {
        return new UndercoverVoteTally();
    }

    @Bean
    public UndercoverTieBreaker undercoverTieBreaker() {
        return new UndercoverTieBreaker();
    }

    @Bean
    public UndercoverWinCondition undercoverWinCondition() {
        return new UndercoverWinCondition();
    }

    @Bean
    public UndercoverWordMatcher undercoverWordMatcher() {
        return new UndercoverWordMatcher();
    }

    @Bean
    public Clock undercoverClock() {
        return Clock.systemUTC();
    }
}
