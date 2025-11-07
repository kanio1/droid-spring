package com.droid.bss.infrastructure.outbox;

import com.droid.bss.domain.outbox.OutboxStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Outbox Cleanup Job
 *
 * Periodically cleans up old published events to keep the database performant
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OutboxCleanupJob {

    private final OutboxRepository outboxRepository;

    /**
     * Clean up old published events daily at 2 AM
     * Keeps published events for 30 days
     */
    @Scheduled(cron = "0 0 2 * * ?")
    @Transactional
    public void cleanupOldPublishedEvents() {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(30);

        log.info("Starting cleanup of published events older than {}", cutoffDate);

        try {
            int deletedCount = outboxRepository.deletePublishedEventsOlderThan(cutoffDate);

            if (deletedCount > 0) {
                log.info("Cleaned up {} old published events", deletedCount);
            } else {
                log.debug("No old published events to clean up");
            }
        } catch (Exception e) {
            log.error("Failed to clean up old published events", e);
        }
    }

    /**
     * Clean up dead letter events older than 90 days (weekly)
     */
    @Scheduled(cron = "0 0 3 ? * SUN")
    @Transactional
    public void cleanupOldDeadLetterEvents() {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(90);

        log.info("Starting cleanup of dead letter events older than {}", cutoffDate);

        try {
            int deletedCount = outboxRepository.deleteDeadLetterEventsOlderThan(cutoffDate);

            if (deletedCount > 0) {
                log.info("Cleaned up {} old dead letter events", deletedCount);
            } else {
                log.debug("No old dead letter events to clean up");
            }
        } catch (Exception e) {
            log.error("Failed to clean up old dead letter events", e);
        }
    }

    /**
     * Log outbox statistics daily at 2:30 AM
     */
    @Scheduled(cron = "0 30 2 * * ?")
    public void logStatistics() {
        try {
            long pending = outboxRepository.countByStatus(OutboxStatus.PENDING);
            long published = outboxRepository.countByStatus(OutboxStatus.PUBLISHED);
            long retry = outboxRepository.countByStatus(OutboxStatus.RETRY);
            long deadLetter = outboxRepository.countByStatus(OutboxStatus.DEAD_LETTER);

            long total = pending + published + retry + deadLetter;

            log.info("Outbox Statistics - PENDING: {}, PUBLISHED: {}, RETRY: {}, DEAD_LETTER: {}, TOTAL: {}",
                    pending, published, retry, deadLetter, total);

            if (deadLetter > 0) {
                log.warn("Warning: {} dead letter events detected! Check outbox_dead_letters view for details", deadLetter);
            }
        } catch (Exception e) {
            log.error("Failed to log outbox statistics", e);
        }
    }
}
