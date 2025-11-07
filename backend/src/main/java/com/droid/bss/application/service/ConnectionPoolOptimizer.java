package com.droid.bss.application.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Connection pool optimization and monitoring service
 */
@Service
public class ConnectionPoolOptimizer {

    private static final Logger logger = LoggerFactory.getLogger(ConnectionPoolOptimizer.class);
    private final int corePoolSize;
    private final int maxPoolSize;
    private final long keepAliveTime;
    private final int queueCapacity;
    private final ThreadPoolExecutor threadPoolExecutor;
    private final ScheduledExecutorService monitorExecutor;
    private final AtomicInteger activeConnections = new AtomicInteger(0);
    private final AtomicInteger totalConnectionsCreated = new AtomicInteger(0);
    private final AtomicInteger totalConnectionsRejected = new AtomicInteger(0);

    public ConnectionPoolOptimizer(
            @Value("${bss.pool.core-size:10}") int corePoolSize,
            @Value("${bss.pool.max-size:50}") int maxPoolSize,
            @Value("${bss.pool.keep-alive:60}") long keepAliveTime,
            @Value("${bss.pool.queue-capacity:100}") int queueCapacity) {

        this.corePoolSize = corePoolSize;
        this.maxPoolSize = maxPoolSize;
        this.keepAliveTime = keepAliveTime;
        this.queueCapacity = queueCapacity;

        this.threadPoolExecutor = new ThreadPoolExecutor(
            corePoolSize,
            maxPoolSize,
            keepAliveTime,
            TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(queueCapacity),
            new NamedThreadFactory("bss-pool"),
            new RejectedExecutionHandler() {
                @Override
                public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
                    totalConnectionsRejected.incrementAndGet();
                    logger.warn("Task rejected from pool. Active: {}, Pool: {}, Queue: {}",
                        executor.getActiveCount(), executor.getPoolSize(), executor.getQueue().size());
                }
            }
        );

        this.monitorExecutor = Executors.newSingleThreadScheduledExecutor(new NamedThreadFactory("bss-pool-monitor"));
        startMonitoring();
    }

    public void executeTask(Runnable task) {
        activeConnections.incrementAndGet();
        totalConnectionsCreated.incrementAndGet();
        threadPoolExecutor.execute(() -> {
            try {
                task.run();
            } finally {
                activeConnections.decrementAndGet();
            }
        });
    }

    public <T> Future<T> submitTask(Callable<T> task) {
        activeConnections.incrementAndGet();
        totalConnectionsCreated.incrementAndGet();
        return threadPoolExecutor.submit(() -> {
            try {
                return task.call();
            } finally {
                activeConnections.decrementAndGet();
            }
        });
    }

    public PoolStats getPoolStats() {
        return new PoolStats(
            activeConnections.get(),
            threadPoolExecutor.getPoolSize(),
            threadPoolExecutor.getCorePoolSize(),
            threadPoolExecutor.getMaximumPoolSize(),
            threadPoolExecutor.getQueue().size(),
            queueCapacity,
            threadPoolExecutor.getActiveCount(),
            totalConnectionsCreated.get(),
            totalConnectionsRejected.get(),
            threadPoolExecutor.getCompletedTaskCount()
        );
    }

    public void optimizePool() {
        int active = threadPoolExecutor.getActiveCount();
        int poolSize = threadPoolExecutor.getPoolSize();
        int queueSize = threadPoolExecutor.getQueue().size();

        if (active > poolSize * 0.8 && poolSize < maxPoolSize) {
            int newPoolSize = Math.min(poolSize + 5, maxPoolSize);
            threadPoolExecutor.setCorePoolSize(newPoolSize);
            logger.info("Optimized pool: increased core size to {}", newPoolSize);
        } else if (active < poolSize * 0.3 && poolSize > corePoolSize) {
            int newPoolSize = Math.max(poolSize - 5, corePoolSize);
            threadPoolExecutor.setCorePoolSize(newPoolSize);
            logger.info("Optimized pool: decreased core size to {}", newPoolSize);
        }

        if (queueSize > queueCapacity * 0.8) {
            logger.warn("Queue capacity warning: {}/{}", queueSize, queueCapacity);
        }
    }

    private void startMonitoring() {
        monitorExecutor.scheduleAtFixedRate(this::optimizePool, 30, 30, TimeUnit.SECONDS);
    }

    public void shutdown() {
        monitorExecutor.shutdown();
        threadPoolExecutor.shutdown();
    }

    public static class PoolStats {
        private final int activeConnections;
        private final int poolSize;
        private final int corePoolSize;
        private final int maxPoolSize;
        private final int queueSize;
        private final int queueCapacity;
        private final int activeCount;
        private final long totalCreated;
        private final long totalRejected;
        private final long completedTasks;

        public PoolStats(int activeConnections, int poolSize, int corePoolSize, int maxPoolSize,
                        int queueSize, int queueCapacity, int activeCount, long totalCreated,
                        long totalRejected, long completedTasks) {
            this.activeConnections = activeConnections;
            this.poolSize = poolSize;
            this.corePoolSize = corePoolSize;
            this.maxPoolSize = maxPoolSize;
            this.queueSize = queueSize;
            this.queueCapacity = queueCapacity;
            this.activeCount = activeCount;
            this.totalCreated = totalCreated;
            this.totalRejected = totalRejected;
            this.completedTasks = completedTasks;
        }

        public double getUtilization() {
            return poolSize > 0 ? (double) activeCount / poolSize * 100 : 0;
        }

        public double getQueueUtilization() {
            return queueCapacity > 0 ? (double) queueSize / queueCapacity * 100 : 0;
        }

        public int getActiveConnections() {
            return activeConnections;
        }

        public int getPoolSize() {
            return poolSize;
        }

        public int getCorePoolSize() {
            return corePoolSize;
        }

        public int getMaxPoolSize() {
            return maxPoolSize;
        }

        public int getQueueSize() {
            return queueSize;
        }

        public int getQueueCapacity() {
            return queueCapacity;
        }

        public int getActiveCount() {
            return activeCount;
        }

        public long getTotalCreated() {
            return totalCreated;
        }

        public long getTotalRejected() {
            return totalRejected;
        }

        public long getCompletedTasks() {
            return completedTasks;
        }
    }

    private static class NamedThreadFactory implements ThreadFactory {
        private final String prefix;
        private final AtomicInteger counter = new AtomicInteger(0);

        public NamedThreadFactory(String prefix) {
            this.prefix = prefix;
        }

        @Override
        public Thread newThread(Runnable r) {
            Thread thread = new Thread(r, prefix + "-" + counter.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        }
    }
}
