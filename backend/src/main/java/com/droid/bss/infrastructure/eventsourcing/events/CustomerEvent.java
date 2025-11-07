package com.droid.bss.infrastructure.eventsourcing.events;

import com.droid.bss.infrastructure.eventsourcing.DomainEvent;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Customer-related domain events
 */
public class CustomerEvent extends DomainEvent {

    public static class CustomerCreated extends CustomerEvent {
        private UUID customerId;
        private String email;
        private String name;
        private String tier;

        public CustomerCreated() {
            super("CustomerCreated");
        }

        public CustomerCreated(UUID customerId, String email, String name, String tier) {
            super("CustomerCreated");
            this.customerId = customerId;
            this.email = email;
            this.name = name;
            this.tier = tier;
        }

        @Override
        public Map<String, Object> toMap() {
            return Map.of(
                "eventId", eventId,
                "eventType", eventType,
                "timestamp", timestamp.toString(),
                "version", version,
                "customerId", customerId.toString(),
                "email", email,
                "name", name,
                "tier", tier
            );
        }
    }

    public static class CustomerUpdated extends CustomerEvent {
        private UUID customerId;
        private String email;
        private String name;

        public CustomerUpdated() {
            super("CustomerUpdated");
        }

        public CustomerUpdated(UUID customerId, String email, String name) {
            super("CustomerUpdated");
            this.customerId = customerId;
            this.email = email;
            this.name = name;
        }

        @Override
        public Map<String, Object> toMap() {
            return Map.of(
                "eventId", eventId,
                "eventType", eventType,
                "timestamp", timestamp.toString(),
                "version", version,
                "customerId", customerId.toString(),
                "email", email,
                "name", name
            );
        }
    }

    public static class CustomerTierChanged extends CustomerEvent {
        private UUID customerId;
        private String oldTier;
        private String newTier;

        public CustomerTierChanged() {
            super("CustomerTierChanged");
        }

        public CustomerTierChanged(UUID customerId, String oldTier, String newTier) {
            super("CustomerTierChanged");
            this.customerId = customerId;
            this.oldTier = oldTier;
            this.newTier = newTier;
        }

        @Override
        public Map<String, Object> toMap() {
            return Map.of(
                "eventId", eventId,
                "eventType", eventType,
                "timestamp", timestamp.toString(),
                "version", version,
                "customerId", customerId.toString(),
                "oldTier", oldTier,
                "newTier", newTier
            );
        }
    }
}
