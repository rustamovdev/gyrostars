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
        log.info("?? Initiating requested full database cleanup & balance reset on startup...");
        adminService.wipeAllDatabase();
        log.info("? Database successfully wiped clean. All balances, orders, and stats are 0.");
    }
}
