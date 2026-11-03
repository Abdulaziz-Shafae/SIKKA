package com.example.sikka.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class LegacyRsvpMigration implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        int updated = jdbcTemplate.update("UPDATE visit_member SET rsvp_status = 'INVITED' WHERE rsvp_status = 'MAYBE'");
        if (updated > 0) log.info("Migrated {} undecided RSVP records to INVITED", updated);
    }
}
