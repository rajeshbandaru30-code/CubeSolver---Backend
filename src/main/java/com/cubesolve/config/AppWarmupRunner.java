package com.cubesolve.config;

import com.cubesolve.engine.KociembaSolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

/**
 * AppWarmupRunner
 *
 * Precomputes CPU-intensive resources (such as Kociemba solver move and pruning tables)
 * asynchronously in the background as soon as the Spring Boot container finishes starting.
 *
 * This ensures:
 * 1. Spring Boot startup and /api/health endpoint are never delayed.
 * 2. When the user submits their first cube to /api/cube/solve, the solver tables
 *    are already warm in memory, eliminating the 2.3-second initial table computation penalty.
 */
@Component
public class AppWarmupRunner {

    private static final Logger log = LoggerFactory.getLogger(AppWarmupRunner.class);

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        CompletableFuture.runAsync(() -> {
            log.info("Eagerly precomputing Kociemba solver tables in background worker thread...");
            long t0 = System.currentTimeMillis();
            try {
                KociembaSolver.initializeTablesEagerly();
                long elapsed = System.currentTimeMillis() - t0;
                log.info("Kociemba solver tables precomputed successfully in {}ms. Ready for <5ms solving.", elapsed);
            } catch (Exception e) {
                log.warn("Background Kociemba table precomputation encountered an error: {}", e.getMessage());
            }
        });
    }
}
