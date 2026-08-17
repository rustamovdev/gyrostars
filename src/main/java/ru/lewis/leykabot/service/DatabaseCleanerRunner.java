package ru.lewis.leykabot.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DatabaseCleanerRunner implements CommandLineRunner {

    private final AdminService adminService;

    @Override
    public void run(String... args) {
        try {
            log.info("🧹 Initiating requested database reset on startup...");
            adminService.wipeAllDatabase();
            log.info("✅ Database reset complete.");
        } catch (Throwable t) {
            log.warn("⚠️ Database wipe warning: {}", t.getMessage());
        }
    }
}
