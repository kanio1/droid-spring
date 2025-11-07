package main

import (
	"context"
	"encoding/json"
	"fmt"
	"log"
	"math/rand"
	"os"
	"os/signal"
	"strconv"
	"sync"
	"syscall"
	"time"

	"github.com/go-redis/redis/v8"
)

const (
	redisAddr   = "localhost:6379"
	dbIndex     = 0
	streamName  = "events:stream"
	consumerGrp = "event-processors"
)

type StreamEvent struct {
	EventID      string    `json:"event_id"`
	EventType    string    `json:"event_type"`
	TenantID     string    `json:"tenant_id"`
	Source       string    `json:"source"`
	Data         string    `json:"data"`
	Timestamp    time.Time `json:"timestamp"`
	PartitionKey string    `json:"partition_key"`
}

type SimulatorStats struct {
	TotalMessages  int64     `json:"total_messages"`
	SuccessCount   int64     `json:"success_count"`
	ErrorCount     int64     `json:"error_count"`
	StartTime      time.Time `json:"start_time"`
	MsgsPerSec     float64   `json:"msgs_per_sec"`
	mu             sync.Mutex
}

func (s *SimulatorStats) AddMessage(success bool) {
	s.mu.Lock()
	defer s.mu.Unlock()
	if success {
		s.SuccessCount++
	} else {
		s.ErrorCount++
	}
	s.TotalMessages++
}

func (s *SimulatorStats) UpdateRate() {
	s.mu.Lock()
	defer s.mu.Unlock()
	elapsed := time.Since(s.StartTime).Seconds()
	if elapsed > 0 {
		s.MsgsPerSec = float64(s.SuccessCount) / elapsed
	}
}

func main() {
	// Setup signal handling
	ctx, cancel := context.WithCancel(context.Background())
	defer cancel()

	c := make(chan os.Signal, 1)
	signal.Notify(c, os.Interrupt, syscall.SIGTERM)
	go func() {
		<-c
		log.Println("Received shutdown signal, stopping simulator...")
		cancel()
	}()

	// Create Redis client
	rdb := redis.NewClient(&redis.Options{
		Addr:     redisAddr,
		DB:       dbIndex,
		Password: "", // No password by default
	})

	// Test connection
	if err := rdb.Ping(ctx).Err(); err != nil {
		log.Fatalf("Failed to connect to Redis: %v", err)
	}
	log.Println("Connected to Redis successfully")

	// Initialize stream and consumer group
	if err := initializeStream(rdb); err != nil {
		log.Fatalf("Failed to initialize stream: %v", err)
	}

	// Create statistics tracker
	stats := &SimulatorStats{
		StartTime: time.Now(),
	}

	// Start statistics reporter
	go func() {
		ticker := time.NewTicker(5 * time.Second)
		defer ticker.Stop()
		for {
			select {
			case <-ticker.C:
				stats.UpdateRate()
				log.Printf("Stats: Total=%d, Success=%d, Errors=%d, Rate=%.2f msgs/sec",
					stats.TotalMessages, stats.SuccessCount, stats.ErrorCount, stats.MsgsPerSec)
			case <-ctx.Done():
				return
			}
		}
	}()

	// Run stream simulation
	runStreamSimulation(ctx, rdb, stats)

	// Final statistics
	stats.UpdateRate()
	elapsed := time.Since(stats.StartTime).Seconds()
	log.Printf("Stream simulation completed in %.2f seconds", elapsed)
	log.Printf("Final stats: Total=%d, Success=%d, Errors=%d, Rate=%.2f msgs/sec",
		stats.TotalMessages, stats.SuccessCount, stats.ErrorCount, stats.MsgsPerSec)
}

func initializeStream(rdb *redis.Client) error {
	log.Printf("Initializing stream: %s", streamName)

	// Create a test entry to initialize the stream
	err := rdb.XAdd(ctx, &redis.XAddArgs{
		Stream: streamName,
		Values: map[string]interface{}{
			"event_id":     "init-" + fmt.Sprint(time.Now().Unix()),
			"event_type":   "system.init",
			"tenant_id":    "system",
			"source":       "/init",
			"data":         "Stream initialized",
			"timestamp":    time.Now().Format(time.RFC3339),
			"partition_key": "0",
		},
	}).Err()

	if err != nil {
		return fmt.Errorf("failed to add init entry: %w", err)
	}

	// Create consumer group
	err = rdb.XGroupCreate(ctx, streamName, consumerGrp, "0-0").Err()
	if err != nil && err != redis.ErrConsumerGroup {
		return fmt.Errorf("failed to create consumer group: %w", err)
	}

	log.Println("Stream and consumer group initialized")
	return nil
}

func runStreamSimulation(ctx context.Context, rdb *redis.Client, stats *SimulatorStats) {
	log.Println("Starting Redis streams simulation")

	// Configuration
	numTenants := 10
	targetRate := 50000.0 // messages per second
	batchSize := 1000

	// Calculate interval based on target rate
	interval := time.Duration(float64(time.Second) / (targetRate / float64(numTenants)))

	log.Printf("Target: %d tenants, %.2f msgs/sec/tenant, interval: %v",
		numTenants, targetRate/float64(numTenants), interval)

	// Create ticker
	ticker := time.NewTicker(interval)
	defer ticker.Stop()

	// Generate tenant IDs
	tenants := make([]string, numTenants)
	for i := 0; i < numTenants; i++ {
		tenants[i] = fmt.Sprintf("tenant-%03d", i+1)
	}

	// Generate events
	for time.Now().Before(time.Now().Add(5 * time.Minute)) { // Run for 5 minutes
		select {
		case <-ctx.Done():
			log.Println("Simulation cancelled")
			return
		case <-ticker.C:
			// Generate batch for all tenants
			var wg sync.WaitGroup
			for _, tenant := range tenants {
				wg.Add(1)
				go func(tenantID string) {
					defer wg.Done()
					err := generateEventBatch(rdb, tenantID, batchSize/numTenants)
					if err != nil {
						log.Printf("Error generating batch for %s: %v", tenantID, err)
						stats.AddMessage(false)
					} else {
						stats.AddMessage(true)
					}
				}(tenant)
			}
			wg.Wait()
		}
	}
}

func generateEventBatch(rdb *redis.Client, tenantID string, batchSize int) error {
	// Use pipeline for batch operations
	pipe := rdb.Pipeline()

	eventTypes := []string{
		"order.created",
		"order.updated",
		"order.cancelled",
		"payment.processed",
		"payment.failed",
		"invoice.generated",
		"customer.updated",
		"subscription.activated",
		"subscription.cancelled",
		"fraud.alert",
	}

	for i := 0; i < batchSize; i++ {
		eventType := eventTypes[rand.Intn(len(eventTypes))]
		eventID := fmt.Sprintf("evt-%s-%s-%d", tenantID, time.Now().Format("20060102150405"), i)
		partitionKey := strconv.Itoa(rand.Intn(100)) // Random partition 0-99

		event := StreamEvent{
			EventID:      eventID,
			EventType:    eventType,
			TenantID:     tenantID,
			Source:       fmt.Sprintf("/tenants/%s/services/%s", tenantID, getServiceName(eventType)),
			Data:         generateEventData(eventType, tenantID),
			Timestamp:    time.Now(),
			PartitionKey: partitionKey,
		}

		// Add to pipeline
		pipe.XAdd(ctx, &redis.XAddArgs{
			Stream: streamName,
			Values: map[string]interface{}{
				"event_id":       event.EventID,
				"event_type":     event.EventType,
				"tenant_id":      event.TenantID,
				"source":         event.Source,
				"data":           event.Data,
				"timestamp":      event.Timestamp.Format(time.RFC3339),
				"partition_key":  event.PartitionKey,
			},
		})
	}

	// Execute pipeline
	_, err := pipe.Exec(ctx)
	return err
}

func getServiceName(eventType string) string {
	switch eventType {
	case "order.created", "order.updated", "order.cancelled":
		return "order-service"
	case "payment.processed", "payment.failed":
		return "payment-service"
	case "invoice.generated":
		return "billing-service"
	case "customer.updated":
		return "customer-service"
	case "subscription.activated", "subscription.cancelled":
		return "subscription-service"
	case "fraud.alert":
		return "fraud-service"
	default:
		return "unknown-service"
	}
}

func generateEventData(eventType, tenantID string) string {
	switch eventType {
	case "order.created":
		return fmt.Sprintf(`{"order_value": %.2f, "items_count": %d, "customer_segment": "%s"}`,
			rand.Float64()*1000, rand.Intn(10)+1, getRandomSegment())
	case "payment.processed":
		return fmt.Sprintf(`{"amount": %.2f, "currency": "USD", "method": "%s", "status": "success"}`,
			rand.Float64()*500, getRandomPaymentMethod())
	case "fraud.alert":
		return fmt.Sprintf(`{"risk_score": %.2f, "rule_triggered": "%s", "action": "%s"}`,
			rand.Float64()*100, getRandomFraudRule(), getRandomAction())
	case "customer.updated":
		return fmt.Sprintf(`{"customer_id": "%s", "tier": "%s", "last_activity": "%s"}`,
			fmt.Sprintf("cust-%s-%06d", tenantID, rand.Intn(100000)),
			getRandomTier(), time.Now().Add(-time.Duration(rand.Intn(30))*24*time.Hour).Format(time.RFC3339))
	default:
		return fmt.Sprintf(`{"metadata": {"processed_at": "%s", "version": "1.0"}}`,
			time.Now().Format(time.RFC3339))
	}
}

func getRandomSegment() string {
	segments := []string{"standard", "premium", "enterprise", "vip", "basic"}
	return segments[rand.Intn(len(segments))]
}

func getRandomPaymentMethod() string {
	methods := []string{"credit_card", "debit_card", "bank_transfer", "digital_wallet", "crypto"}
	return methods[rand.Intn(len(methods))]
}

func getRandomFraudRule() string {
	rules := []string{"VELOCITY_CHECK", "HIGH_VALUE_TRANSACTION", "LOCATION_ANOMALY", "PATTERN_MATCH", "BLACKLIST_CHECK"}
	return rules[rand.Intn(len(rules))]
}

func getRandomAction() string {
	actions := []string{"BLOCK", "REVIEW", "ALERT", "WARN", "ALLOW"}
	return actions[rand.Intn(len(actions))]
}

func getRandomTier() string {
	tiers := []string{"Bronze", "Silver", "Gold", "Platinum", "Diamond"}
	return tiers[rand.Intn(len(tiers))]
}
