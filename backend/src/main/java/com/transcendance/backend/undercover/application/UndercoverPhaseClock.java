package com.transcendance.backend.undercover.application;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

/**
 * Moves along every match whose clock ran out: a silent speaker passes his
 * turn, missing voters abstain, a Mr White who does not answer fails his guess.
 *
 * Deadlines are persisted, so a restart loses nothing: the first sweep after it
 * catches up on whatever expired meanwhile.
 */
@Component
@RequiredArgsConstructor
public class UndercoverPhaseClock {

    private static final Logger log = LogManager.getLogger(UndercoverPhaseClock.class);

    private final UndercoverService undercoverService;
    private final UndercoverPlayerChannel playerChannel;

    /**
     * Every second: phases last tens of seconds, and a turn that hangs on a
     * little after its countdown shows zero is visible to the whole table.
     *
     * Each match is handled in its own transaction and everything is caught: an
     * exception thrown from a scheduled task is swallowed by the scheduler, and
     * one broken match must not stop the others.
     */
    @Scheduled(fixedDelayString = "${app.undercover.phase-sweep-ms:1000}")
    public void sweep() {
        for (Long matchId : undercoverService.expiredMatchIds()) {
            try {
                if (undercoverService.expirePhase(matchId)) {
                    playerChannel.matchUpdated(matchId);
                }
            } catch (Exception e) {
                log.error("Undercover match {} could not be moved on after its deadline: {}",
                        matchId, e.getMessage(), e);
            }
        }
    }
}
