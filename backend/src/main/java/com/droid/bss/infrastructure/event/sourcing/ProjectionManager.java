package com.droid.bss.infrastructure.event.sourcing;

import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;

/**
 * Manages projections and their updates
 */
@Service
public class ProjectionManager {

    private final EventStore eventStore;
    private final Map<String, Projection> projections = new ConcurrentHashMap<>();
    private final List<ProjectionStatus> projectionStatuses = new CopyOnWriteArrayList<>();
    private final Executor projectionExecutor = ForkJoinPool.commonPool();

    public ProjectionManager(EventStore eventStore) {
        this.eventStore = eventStore;
    }

    /**
     * Register a projection
     */
    public void registerProjection(Projection projection) {
        projections.put(projection.getName(), projection);
        projectionStatuses.add(new ProjectionStatus(
                projection.getName(),
                projection.getVersion(),
                projection.isUpToDate()
        ));
    }

    /**
     * Unregister a projection
     */
    public void unregisterProjection(String name) {
        projections.remove(name);
        projectionStatuses.removeIf(status -> status.getName().equals(name));
    }

    /**
     * Update all projections with an event
     */
    public void updateProjections(StoredEvent event) {
        projections.values().forEach(projection -> {
            try {
                projection.handleEvent(event);
            } catch (Exception e) {
                // Log error but continue with other projections
                System.err.println("Error updating projection " + projection.getName() + ": " + e.getMessage());
            }
        });

        // Update status
        updateProjectionStatus(event);
    }

    /**
     * Rebuild a specific projection
     */
    public void rebuildProjection(String name) {
        Projection projection = projections.get(name);
        if (projection == null) {
            throw new IllegalArgumentException("Projection not found: " + name);
        }

        // Clear projection data
        // Note: This would require a clear() method on the projection

        // Load all events and apply to projection
        // Note: This would need to load events by aggregate type or all events
        // For now, we'll update the version
    }

    /**
     * Rebuild all projections
     */
    public void rebuildAllProjections() {
        projections.keySet().forEach(this::rebuildProjection);
    }

    /**
     * Get projection status
     */
    public List<ProjectionStatus> getProjectionStatuses() {
        return new ArrayList<>(projectionStatuses);
    }

    /**
     * Get status of a specific projection
     */
    public Optional<ProjectionStatus> getProjectionStatus(String name) {
        return projectionStatuses.stream()
                .filter(status -> status.getName().equals(name))
                .findFirst();
    }

    /**
     * Check if all projections are up to date
     */
    public boolean areAllProjectionsUpToDate() {
        return projectionStatuses.stream().allMatch(ProjectionStatus::isUpToDate);
    }

    /**
     * Update projection status
     */
    private void updateProjectionStatus(StoredEvent event) {
        String projectionName = event.getAggregateType();
        if (projections.containsKey(projectionName)) {
            Projection projection = projections.get(projectionName);
            projectionStatuses.replaceAll(status -> {
                if (status.getName().equals(projectionName)) {
                    return new ProjectionStatus(
                            projectionName,
                            projection.getVersion(),
                            projection.isUpToDate()
                    );
                }
                return status;
            });
        }
    }

    /**
     * Status of a projection
     */
    public static class ProjectionStatus {
        private final String name;
        private final long version;
        private final boolean upToDate;

        public ProjectionStatus(String name, long version, boolean upToDate) {
            this.name = name;
            this.version = version;
            this.upToDate = upToDate;
        }

        public String getName() {
            return name;
        }

        public long getVersion() {
            return version;
        }

        public boolean isUpToDate() {
            return upToDate;
        }
    }
}
