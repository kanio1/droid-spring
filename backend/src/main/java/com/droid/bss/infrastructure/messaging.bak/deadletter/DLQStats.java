package com.droid.bss.infrastructure.messaging.deadletter;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Statistics for Dead Letter Queue operations.
 *
 * @since 1.0
 */
public class DLQStats {

    private final Instant createdAt;
    private final String queueName;
    private final AtomicLong totalMessages = new AtomicLong(0);
    private final AtomicLong totalAdded = new AtomicLong(0);
    private final AtomicLong totalRequeued = new AtomicLong(0);
    private final AtomicLong totalDeleted = new AtomicLong(0);
    private final AtomicLong totalPurged = new AtomicLong(0);
    private final AtomicLong totalErrors = new AtomicLong(0);
    private final AtomicLong totalRetries = new AtomicLong(0);

    private final ConcurrentMap<String, AtomicLong> messagesByTopic = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, AtomicLong> messagesByErrorType = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, AtomicLong> messagesByErrorCode = new ConcurrentHashMap<>();

    // Recent statistics (last hour)
    private final AtomicLong recentMessagesAdded = new AtomicLong(0);
    private final AtomicLong recentMessagesRequeued = new AtomicLong(0);
    private final AtomicLong recentErrors = new AtomicLong(0);

    /**
     * Creates a new DLQStats.
     *
     * @param queueName the queue name
     */
    public DLQStats(String queueName) {
        this.queueName = queueName;
        this.createdAt = Instant.now();
    }

    /**
     * Records a message being added to the DLQ.
     *
     * @param topic the topic
     * @param errorType the error type
     * @param errorCode the error code
     */
    public void recordAdd(String topic, String errorType, String errorCode) {
        totalMessages.incrementAndGet();
        totalAdded.incrementAndGet();
        recentMessagesAdded.incrementAndGet();

        messagesByTopic.computeIfAbsent(topic, k -> new AtomicLong(0)).incrementAndGet();
        messagesByErrorType.computeIfAbsent(errorType, k -> new AtomicLong(0)).incrementAndGet();
        if (errorCode != null) {
            messagesByErrorCode.computeIfAbsent(errorCode, k -> new AtomicLong(0)).incrementAndGet();
        }
    }

    /**
     * Records a message being requeued.
     *
     * @param topic the topic
     */
    public void recordRequeue(String topic) {
        totalRequeued.incrementAndGet();
        recentMessagesRequeued.incrementAndGet();

        messagesByTopic.computeIfAbsent(topic, k -> new AtomicLong(0)).decrementAndGet();
    }

    /**
     * Records a message being deleted.
     */
    public void recordDelete() {
        totalDeleted.incrementAndGet();
    }

    /**
     * Records a message being purged.
     */
    public void recordPurge() {
        totalPurged.incrementAndGet();
    }

    /**
     * Records an error.
     */
    public void recordError() {
        totalErrors.incrementAndGet();
        recentErrors.incrementAndGet();
    }

    /**
     * Records a retry attempt.
     */
    public void recordRetry() {
        totalRetries.incrementAndGet();
    }

    /**
     * Gets the total number of messages.
     *
     * @return the total messages
     */
    public long getTotalMessages() {
        return totalMessages.get();
    }

    /**
     * Gets the number of messages added.
     *
     * @return the added count
     */
    public long getTotalAdded() {
        return totalAdded.get();
    }

    /**
     * Gets the number of messages requeued.
     *
     * @return the requeued count
     */
    public long getTotalRequeued() {
        return totalRequeued.get();
    }

    /**
     * Gets the number of messages deleted.
     *
     * @return the deleted count
     */
    public long getTotalDeleted() {
        return totalDeleted.get();
    }

    /**
     * Gets the number of messages purged.
     *
     * @return the purged count
     */
    public long getTotalPurged() {
        return totalPurged.get();
    }

    /**
     * Gets the number of errors.
     *
     * @return the error count
     */
    public long getTotalErrors() {
        return totalErrors.get();
    }

    /**
     * Gets the number of retries.
     *
     * @return the retry count
     */
    public long getTotalRetries() {
        return totalRetries.get();
    }

    /**
     * Gets the messages by topic.
     *
     * @return the map of topic to count
     */
    public ConcurrentMap<String, Long> getMessagesByTopic() {
        ConcurrentMap<String, Long> result = new ConcurrentHashMap<>();
        messagesByTopic.forEach((k, v) -> result.put(k, v.get()));
        return result;
    }

    /**
     * Gets the messages by error type.
     *
     * @return the map of error type to count
     */
    public ConcurrentMap<String, Long> getMessagesByErrorType() {
        ConcurrentMap<String, Long> result = new ConcurrentHashMap<>();
        messagesByErrorType.forEach((k, v) -> result.put(k, v.get()));
        return result;
    }

    /**
     * Gets the messages by error code.
     *
     * @return the map of error code to count
     */
    public ConcurrentMap<String, Long> getMessagesByErrorCode() {
        ConcurrentMap<String, Long> result = new ConcurrentHashMap<>();
        messagesByErrorCode.forEach((k, v) -> result.put(k, v.get()));
        return result;
    }

    /**
     * Gets the recent messages added (last hour).
     *
     * @return the recent added count
     */
    public long getRecentMessagesAdded() {
        return recentMessagesAdded.get();
    }

    /**
     * Gets the recent messages requeued (last hour).
     *
     * @return the recent requeued count
     */
    public long getRecentMessagesRequeued() {
        return recentMessagesRequeued.get();
    }

    /**
     * Gets the recent errors (last hour).
     *
     * @return the recent error count
     */
    public long getRecentErrors() {
        return recentErrors.get();
    }

    /**
     * Gets the queue name.
     *
     * @return the queue name
     */
    public String getQueueName() {
        return queueName;
    }

    /**
     * Gets the stats age in milliseconds.
     *
     * @return the age in milliseconds
     */
    public long getAgeMs() {
        return java.time.Duration.between(createdAt, Instant.now()).toMillis();
    }

    /**
     * Gets the average messages per minute.
     *
     * @return the average messages per minute
     */
    public double getAverageMessagesPerMinute() {
        long ageMinutes = getAgeMs() / (1000 * 60);
        if (ageMinutes == 0) {
            return 0;
        }
        return (double) totalAdded.get() / ageMinutes;
    }

    /**
     * Gets the requeue rate percentage.
     *
     * @return the requeue rate (0-100)
     */
    public double getRequeueRate() {
        if (totalAdded.get() == 0) {
            return 0;
        }
        return (double) totalRequeued.get() / totalAdded.get() * 100;
    }

    /**
     * Gets the error rate percentage.
     *
     * @return the error rate (0-100)
     */
    public double getErrorRate() {
        if (totalMessages.get() == 0) {
            return 0;
        }
        return (double) totalErrors.get() / totalMessages.get() * 100;
    }

    /**
     * Resets all statistics.
     */
    public void reset() {
        totalMessages.set(0);
        totalAdded.set(0);
        totalRequeued.set(0);
        totalDeleted.set(0);
        totalPurged.set(0);
        totalErrors.set(0);
        totalRetries.set(0);
        recentMessagesAdded.set(0);
        recentMessagesRequeued.set(0);
        recentErrors.set(0);
        messagesByTopic.clear();
        messagesByErrorType.clear();
        messagesByErrorCode.clear();
    }

    /**
     * Gets a summary of the statistics.
     *
     * @return the summary
     */
    public String getSummary() {
        return String.format(
            "DLQ Stats [%s]: total=%d, added=%d, requeued=%d, deleted=%d, errors=%d, " +
            "requeueRate=%.2f%%, errorRate=%.2f%%, avgPerMin=%.2f",
            queueName,
            getTotalMessages(),
            getTotalAdded(),
            getTotalRequeued(),
            getTotalDeleted(),
            getTotalErrors(),
            getRequeueRate(),
            getErrorRate(),
            getAverageMessagesPerMinute()
        );
    }

    @Override
    public String toString() {
        return "DLQStats{" +
            "queueName='" + queueName + '\'' +
            ", totalMessages=" + getTotalMessages() +
            ", totalAdded=" + getTotalAdded() +
            ", totalRequeued=" + getTotalRequeued() +
            ", totalErrors=" + getTotalErrors() +
            ", ageMs=" + getAgeMs() +
            '}';
    }
}
