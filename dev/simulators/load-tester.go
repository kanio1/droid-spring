package main

import (
	"context"
	"database/sql"
	"encoding/json"
	"fmt"
	"log"
	"math/rand"
	"os"
	"os/signal"
	"runtime"
	"strconv"
	"sync"
	"sync/atomic"
	"syscall"
	"time"

	"github.com/go-redis/redis/v8"
	"github.com/segmentio/kafka-go"
	_ "github.com/lib/pq"
	cloudevents "github.com/cloudevents/sdk-go/v2"
)

type LoadTestConfig struct {
	DurationMinutes    int     `json:"duration_minutes"`
	KafkaEnabled       bool    `json:"kafka_enabled"`
	RedisEnabled       bool    `json:"redis_enabled"`
	PostgresEnabled    bool    `json:"postgres_enabled"`
	TargetEventsPerSec float64 `json:"target_events_per_sec"`
	NumTenants         int     `json:"num_tenants"`
	ReportInterval     int     `json:"report_interval_seconds"`
}

type LoadTestStats struct {
	TotalEvents   int64           `json:"total_events"`
	KafkaEvents   int64           `json:"kafka_events"`
	RedisEvents   int64           `json:"redis_events"`
	PgEvents      int64           `json:"postgres_events"`
	ErrorCount    int64           `json:"error_count"`
	StartTime     time.Time       `json:"start_time"`
	CurrentRate   float64         `json:"current_rate"`
	mu            sync.Mutex
}

func (s *LoadTestStats) AddEvent(component string, success bool) {
	s.mu.Lock()
	defer s.mu.Unlock()
	if success {
		s.TotalEvents++
		switch component {
		case "kafka":
			s.KafkaEvents++
		case "redis":
			s.RedisEvents++
		case "postgres":
			s.PgEvents++
		}
	} else {
		s.ErrorCount++
	}
}

func (s *LoadTestStats) UpdateRate() {
	s.mu.Lock()
	defer s.mu.Unlock()
	elapsed := time.Since(s.StartTime).Seconds()
	if elapsed > 0 {
		s.CurrentRate = float64(s.TotalEvents) / elapsed
	}
}

type LoadTester struct {
	config     LoadTestStats
	stats      *LoadTestStats
	kafkaW     *kafka.Writer
	redisC     *redis.Client
	pgDB       *sql.DB
	ctx        context.Context
	cancel     context.CancelFunc
}

func NewLoadTester(config LoadTestConfig) (*LoadTester, error) {
	ctx, cancel := context.WithCancel(context.Background())

	var kafkaW *kafka.Writer
	var redisC *redis.Client
	var pgDB *sql.DB

	// Initialize Kafka
	if config.KafkaEnabled {
		kafkaW = &kafka.Writer{
			Addr:         kafka.TCP("localhost:9092"),
			Topic:        "cloud-events",
			Balancer:     &kafka.LeastBytes{},
			WriteTimeout: 10 * time.Second,
			RequiredAcks: kafka.RequireAll,
			BatchSize:    100,
			BatchTimeout: 5 * time.Millisecond,
			Compression:  kafka.CompressionSnappy,
		}
	}

	// Initialize Redis
	if config.RedisEnabled {
		redisC = redis.NewClient(&redis.Options{
			Addr:     "localhost:6379",
			Password: "",
			DB:       0,
		})
		if err := redisC.Ping(ctx).Err(); err != nil {
			return nil, fmt.Errorf("failed to connect to Redis: %w", err)
		}
	}

	// Initialize PostgreSQL
	if config.PostgresEnabled {
		dsn := "host=localhost port=5432 user=postgres password=postgres dbname=bss_events sslmode=disable"
		db, err := sql.Open("postgres", dsn)
		if err != nil {
			return nil, fmt.Errorf("failed to connect to PostgreSQL: %w", err)
		}
		if err := db.Ping(); err != nil {
			return nil, fmt.Errorf("failed to ping PostgreSQL: %w", err)
		}
		pgDB = db
	}

	return &LoadTester{
		config: config,
		stats: &LoadTestStats{
			StartTime: time.Now(),
		},
		kafkaW: kafkaW,
		redisC: redisC,
		pgDB:   pgDB,
		ctx:    ctx,
		cancel: cancel,
	}, nil
}

func (lt *LoadTester) Close() {
	lt.cancel()
	if lt.kafkaW != nil {
		lt.kafkaW.Close()
	}
	if lt.redisC != nil {
		lt.redisC.Close()
	}
	if lt.pgDB != nil {
		lt.pgDB.Close()
	}
}

func (lt *LoadTester) Run() error {
	log.Printf("Starting integrated load test")
	log.Printf("Duration: %d minutes", lt.config.DurationMinutes)
	log.Printf("Kafka: %v, Redis: %v, PostgreSQL: %v",
		lt.config.KafkaEnabled, lt.config.RedisEnabled, lt.config.PostgresEnabled)
	log.Printf("Target rate: %.2f events/sec", lt.config.TargetEventsPerSec)
	log.Printf("Tenants: %d", lt.config.NumTenants)

	// Create workers
	numWorkers := 10
	eventsPerWorker := lt.config.TargetEventsPerSec / float64(numWorkers)
	interval := time.Duration(float64(time.Second) / eventsPerWorker)

	log.Printf("Workers: %d, Events/worker/sec: %.2f, Interval: %v",
		numWorkers, eventsPerWorker, interval)

	// Setup stats reporter
	go func() {
		ticker := time.NewTicker(time.Duration(lt.config.ReportInterval) * time.Second)
		defer ticker.Stop()
		for {
			select {
			case <-ticker.C:
				lt.stats.UpdateRate()
				log.Printf("Stats: Total=%d [K:%d R:%d P:%d], Errors=%d, Rate=%.2f events/sec, Mem: %s",
					lt.stats.TotalEvents, lt.stats.KafkaEvents, lt.stats.RedisEvents,
					lt.stats.PgEvents, lt.stats.ErrorCount, lt.stats.CurrentRate,
					formatBytes(uint64(runtime.MemStats{}.Alloc)))
			case <-lt.ctx.Done():
				return
			}
		}
	}()

	// Create tenant IDs
	tenants := make([]string, lt.config.NumTenants)
	for i := 0; i < lt.config.NumTenants; i++ {
		tenants[i] = fmt.Sprintf("tenant-%03d", i+1)
	}

	// Create ticker
	ticker := time.NewTicker(interval)
	defer ticker.Stop()

	// Run for specified duration
	endTime := time.Now().Add(time.Duration(lt.config.DurationMinutes) * time.Minute)

	for time.Now().Before(endTime) {
		select {
		case <-lt.ctx.Done():
			log.Println("Load test cancelled")
			return nil
		case <-ticker.C:
			var wg sync.WaitGroup
			for _, tenant := range tenants {
				wg.Add(1)
				go func(tenantID string) {
					defer wg.Done()
					lt.generateAndSendEvent(tenantID)
				}(tenant)
			}
			wg.Wait()
		}
	}

	lt.stats.UpdateRate()
	elapsed := time.Since(lt.stats.StartTime).Seconds()
	log.Printf("\n=== Load Test Completed ===")
	log.Printf("Duration: %.2f seconds", elapsed)
	log.Printf("Total events: %d", lt.stats.TotalEvents)
	log.Printf("Kafka: %d, Redis: %d, PostgreSQL: %d",
		lt.stats.KafkaEvents, lt.stats.RedisEvents, lt.stats.PgEvents)
	log.Printf("Errors: %d", lt.stats.ErrorCount)
	log.Printf("Final rate: %.2f events/sec", lt.stats.CurrentRate)
	log.Printf("Average rate: %.2f events/sec", float64(lt.stats.TotalEvents)/elapsed)
	log.Printf("Peak memory: %s", formatBytes(uint64(runtime.MemStats{}.Alloc)))

	return nil
}

func (lt *LoadTester) generateAndSendEvent(tenantID string) {
	// Random event type
	eventTypes := []string{
		"order.created", "order.updated", "payment.processed",
		"invoice.generated", "customer.created", "subscription.activated",
		"fraud.alert", "payment.failed",
	}
	eventType := eventTypes[rand.Intn(len(eventTypes))]

	// Send to Kafka
	if lt.kafkaW != nil {
		err := lt.sendKafkaEvent(tenantID, eventType)
		if err != nil {
			log.Printf("Kafka error: %v", err)
		} else {
			lt.stats.AddEvent("kafka", true)
		}
	}

	// Send to Redis
	if lt.redisC != nil {
		err := lt.sendRedisEvent(tenantID, eventType)
		if err != nil {
			log.Printf("Redis error: %v", err)
		} else {
			lt.stats.AddEvent("redis", true)
		}
	}

	// Insert to PostgreSQL
	if lt.pgDB != nil {
		err := lt.insertPostgresEvent(tenantID, eventType)
		if err != nil {
			log.Printf("PostgreSQL error: %v", err)
		} else {
			lt.stats.AddEvent("postgres", true)
		}
	}
}

func (lt *LoadTester) sendKafkaEvent(tenantID, eventType string) error {
	event := map[string]interface{}{
		"event_id":     fmt.Sprintf("evt-%d", time.Now().UnixNano()),
		"event_type":   eventType,
		"tenant_id":    tenantID,
		"source":       fmt.Sprintf("/tenants/%s/services/%s", tenantID, getServiceName(eventType)),
		"data":         generateEventData(eventType, tenantID),
		"timestamp":    time.Now().Format(time.RFC3339),
	}

	// Create CloudEvent
	ce := cloudevents.NewEvent("1.0")
	ce.SetID(fmt.Sprintf("ce-%d", time.Now().UnixNano()))
	ce.SetSource(fmt.Sprintf("/tenants/%s", tenantID))
	ce.SetType(eventType)
	ce.SetTime(time.Now())
	if err := ce.SetData(cloudevents.ApplicationJSON, event); err != nil {
		return err
	}

	jsonBytes, err := json.Marshal(ce)
	if err != nil {
		return err
	}

	return lt.kafkaW.WriteMessages(context.Background(), kafka.Message{
		Key:   []byte(tenantID),
		Value: jsonBytes,
		Headers: []kafka.Header{
			{Key: "ce-specversion", Value: []byte("1.0")},
			{Key: "ce-type", Value: []byte(ce.Type())},
			{Key: "ce-source", Value: []byte(ce.Source())},
		},
	})
}

func (lt *LoadTester) sendRedisEvent(tenantID, eventType string) error {
	pipe := lt.redisC.Pipeline()

	event := map[string]interface{}{
		"event_id":     fmt.Sprintf("evt-%d", time.Now().UnixNano()),
		"event_type":   eventType,
		"tenant_id":    tenantID,
		"source":       fmt.Sprintf("/tenants/%s/services/%s", tenantID, getServiceName(eventType)),
		"data":         generateEventData(eventType, tenantID),
		"timestamp":    time.Now().Format(time.RFC3339),
	}

	// Add to stream
	pipe.XAdd(context.Background(), &redis.XAddArgs{
		Stream: "events:stream",
		Values: event,
	})

	// Add to cache
	pipe.Set(context.Background(),
		fmt.Sprintf("event:%s:%s", tenantID, eventType),
		generateEventData(eventType, tenantID),
		10*time.Minute)

	_, err := pipe.Exec(context.Background())
	return err
}

func (lt *LoadTester) insertPostgresEvent(tenantID, eventType string) error {
	eventID := fmt.Sprintf("evt-%d", time.Now().UnixNano())
	data := generateEventData(eventType, tenantID)

	stmt := `
		INSERT INTO events (event_id, tenant_id, event_type, source, data, created_at, partition_key)
		VALUES ($1, $2, $3, $4, $5, $6, $7)
	`

	_, err := lt.pgDB.Exec(stmt,
		eventID,
		tenantID,
		eventType,
		fmt.Sprintf("/tenants/%s/services/%s", tenantID, getServiceName(eventType)),
		data,
		time.Now(),
		strconv.Itoa(rand.Intn(100)))

	return err
}

func getServiceName(eventType string) string {
	switch {
	case eventType == "fraud.alert":
		return "fraud-service"
	case eventType == "subscription.activated":
		return "subscription-service"
	case eventType == "payment.processed" || eventType == "payment.failed":
		return "payment-service"
	case eventType == "order.created" || eventType == "order.updated":
		return "order-service"
	case eventType == "invoice.generated":
		return "billing-service"
	case eventType == "customer.created":
		return "customer-service"
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
	case "customer.created":
		return fmt.Sprintf(`{"tier": "%s", "email": "%s", "signup_channel": "%s"}`,
			getRandomTier(), fmt.Sprintf("user%d@%s.example.com", rand.Intn(1000000), tenantID), getRandomChannel())
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
	rules := []string{"VELOCITY_CHECK", "HIGH_VALUE_TRANSACTION", "LOCATION_ANOMALY", "PATTERN_MATCH"}
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

func getRandomChannel() string {
	channels := []string{"web", "mobile", "api", "partner", "direct", "social"}
	return channels[rand.Intn(len(channels))]
}

func formatBytes(bytes uint64) string {
	const (
		KB = 1024
		MB = 1024 * KB
		GB = 1024 * MB
	)

	if bytes >= GB {
		return fmt.Sprintf("%.2f GB", float64(bytes)/GB)
	} else if bytes >= MB {
		return fmt.Sprintf("%.2f MB", float64(bytes)/MB)
	} else if bytes >= KB {
		return fmt.Sprintf("%.2f KB", float64(bytes)/KB)
	}
	return fmt.Sprintf("%d B", bytes)
}

func loadConfig() LoadTestConfig {
	return LoadTestConfig{
		DurationMinutes:    1,
		KafkaEnabled:       true,
		RedisEnabled:       true,
		PostgresEnabled:    true,
		TargetEventsPerSec: 6667,
		NumTenants:         5,
		ReportInterval:     5,
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
		log.Println("Received shutdown signal, stopping load tester...")
		cancel()
	}()

	// Load configuration
	config := loadConfig()

	// Create load tester
	tester, err := NewLoadTester(config)
	if err != nil {
		log.Fatalf("Failed to create load tester: %v", err)
	}
	defer tester.Close()

	// Run load test
	if err := tester.Run(); err != nil {
		log.Fatalf("Load test failed: %v", err)
	}
}
