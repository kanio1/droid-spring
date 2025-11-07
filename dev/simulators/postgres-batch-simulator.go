package main

import (
	"context"
	"database/sql"
	"encoding/hex"
	"fmt"
	"log"
	"math/rand"
	"os"
	"os/signal"
	"strconv"
	"sync"
	"syscall"
	"time"

	_ "github.com/lib/pq"
)

const (
	pgHost     = "localhost"
	pgPort     = 5432
	pgUser     = "postgres"
	pgPassword = "postgres"
	pgDB       = "bss_events"
)

type EventRecord struct {
	EventID      string    `json:"event_id"`
	TenantID     string    `json:"tenant_id"`
	EventType    string    `json:"event_type"`
	Source       string    `json:"source"`
	Data         string    `json:"data"`
	CreatedAt    time.Time `json:"created_at"`
	PartitionKey string    `json:"partition_key"`
}

type PostgresStats struct {
	TotalInserts  int64     `json:"total_inserts"`
	SuccessCount  int64     `json:"success_count"`
	ErrorCount    int64     `json:"error_count"`
	StartTime     time.Time `json:"start_time"`
	InsertsPerSec float64   `json:"inserts_per_sec"`
	mu            sync.Mutex
}

func (s *PostgresStats) AddInsert(success bool) {
	s.mu.Lock()
	defer s.mu.Unlock()
	if success {
		s.SuccessCount++
	} else {
		s.ErrorCount++
	}
	s.TotalInserts++
}

func (s *PostgresStats) UpdateRate() {
	s.mu.Lock()
	defer s.mu.Unlock()
	elapsed := time.Since(s.StartTime).Seconds()
	if elapsed > 0 {
		s.InsertsPerSec = float64(s.SuccessCount) / elapsed
	}
}

type BatchInserter struct {
	db         *sql.DB
	batchSize  int
	numWorkers int
	ctx        context.Context
	cancel     context.CancelFunc
}

func NewBatchInserter(dsn string, batchSize, numWorkers int) (*BatchInserter, error) {
	db, err := sql.Open("postgres", dsn)
	if err != nil {
		return nil, fmt.Errorf("failed to connect to database: %w", err)
	}

	// Test connection
	if err := db.Ping(); err != nil {
		return nil, fmt.Errorf("failed to ping database: %w", err)
	}

	ctx, cancel := context.WithCancel(context.Background())

	return &BatchInserter{
		db:         db,
		batchSize:  batchSize,
		numWorkers: numWorkers,
		ctx:        ctx,
		cancel:     cancel,
	}, nil
}

func (bi *BatchInserter) Close() {
	bi.cancel()
	if err := bi.db.Close(); err != nil {
		log.Printf("Error closing database: %v", err)
	}
}

func (bi *BatchInserter) StartInserter(tenantID string, stats *PostgresStats) {
	for i := 0; i < bi.numWorkers; i++ {
		go func(workerID int) {
			bi.worker(tenantID, workerID, stats)
		}(i)
	}
}

func (bi *BatchInserter) worker(tenantID string, workerID int, stats *PostgresStats) {
	log.Printf("Worker %d started for tenant %s", workerID, tenantID)

	for {
		select {
		case <-bi.ctx.Done():
			log.Printf("Worker %d stopping", workerID)
			return
		default:
			// Generate batch
			events := bi.generateEventBatch(tenantID, bi.batchSize)

			// Insert batch
			elapsed, err := bi.insertBatch(events)
			if err != nil {
				log.Printf("Worker %d: Error inserting batch: %v", workerID, err)
				stats.AddInsert(false)
			} else {
				stats.AddInsert(true)
				log.Printf("Worker %d: Inserted batch in %v (%.2f events/sec)",
					workerID, elapsed, float64(len(events))/elapsed.Seconds())
			}
		}
	}
}

func (bi *BatchInserter) generateEventBatch(tenantID string, size int) []EventRecord {
	events := make([]EventRecord, size)
	eventTypes := []string{
		"order.created", "order.updated", "order.cancelled",
		"payment.processed", "payment.failed", "payment.refunded",
		"invoice.generated", "invoice.sent", "invoice.paid",
		"customer.created", "customer.updated",
		"subscription.started", "subscription.cancelled",
		"fraud.alert", "fraud.resolved",
	}

	for i := 0; i < size; i++ {
		eventType := eventTypes[rand.Intn(len(eventTypes))]
		eventID := bi.generateEventID()
		partitionKey := strconv.Itoa(rand.Intn(100)) // 0-99 for partitioning

		events[i] = EventRecord{
			EventID:      eventID,
			TenantID:     tenantID,
			EventType:    eventType,
			Source:       fmt.Sprintf("/tenants/%s/services/%s", tenantID, bi.getServiceName(eventType)),
			Data:         bi.generateEventData(eventType, tenantID),
			CreatedAt:    time.Now(),
			PartitionKey: partitionKey,
		}
	}

	return events
}

func (bi *BatchInserter) generateEventID() string {
	// Generate UUID v4-like ID
	var b [16]byte
	_, err := rand.Read(b[:])
	if err != nil {
		return fmt.Sprintf("evt-%d", time.Now().UnixNano())
	}
	// Set version (4) and variant bits
	b[6] = (b[6] & 0x0f) | 0x40
	b[8] = (b[8] & 0x3f) | 0x80
	return hex.EncodeToString(b[:])
}

func (bi *BatchInserter) getServiceName(eventType string) string {
	switch {
	case eventType == "fraud.alert" || eventType == "fraud.resolved":
		return "fraud-service"
	case eventType == "subscription.started" || eventType == "subscription.cancelled":
		return "subscription-service"
	case eventType == "payment.processed" || eventType == "payment.failed" || eventType == "payment.refunded":
		return "payment-service"
	case eventType == "order.created" || eventType == "order.updated" || eventType == "order.cancelled":
		return "order-service"
	case eventType == "invoice.generated" || eventType == "invoice.sent" || eventType == "invoice.paid":
		return "billing-service"
	case eventType == "customer.created" || eventType == "customer.updated":
		return "customer-service"
	default:
		return "unknown-service"
	}
}

func (bi *BatchInserter) generateEventData(eventType, tenantID string) string {
	switch eventType {
	case "order.created":
		return fmt.Sprintf(`{"order_value": %.2f, "items_count": %d, "customer_segment": "%s", "currency": "USD"}`,
			rand.Float64()*1000, rand.Intn(10)+1, bi.getRandomSegment())
	case "payment.processed":
		return fmt.Sprintf(`{"amount": %.2f, "currency": "USD", "method": "%s", "status": "success", "transaction_id": "%s"}`,
			rand.Float64()*500, bi.getRandomPaymentMethod(), bi.generateTransactionID())
	case "fraud.alert":
		return fmt.Sprintf(`{"risk_score": %.2f, "rule_triggered": "%s", "action": "%s", "customer_id": "%s"}`,
			rand.Float64()*100, bi.getRandomFraudRule(), bi.getRandomAction(), bi.generateCustomerID(tenantID))
	case "customer.created":
		return fmt.Sprintf(`{"tier": "%s", "email": "%s", "signup_channel": "%s"}`,
			bi.getRandomTier(), bi.generateEmail(tenantID), bi.getRandomChannel())
	case "subscription.started":
		return fmt.Sprintf(`{"plan": "%s", "duration_months": %d, "price": %.2f}`,
			bi.getRandomPlan(), (rand.Intn(12)+1)*3, rand.Float64()*200)
	default:
		return fmt.Sprintf(`{"metadata": {"processed_at": "%s", "version": "1.0", "worker": "%s"}}`,
			time.Now().Format(time.RFC3339), bi.db.Stats().OpenConnections)
	}
}

func (bi *BatchInserter) insertBatch(events []EventRecord) (time.Duration, error) {
	start := time.Now()

	// Prepare statement
	stmt := `
		INSERT INTO events (
			event_id, tenant_id, event_type, source, data, created_at, partition_key
		) VALUES ($1, $2, $3, $4, $5, $6, $7)
	`

	// Use transaction for batch
	tx, err := bi.db.Begin()
	if err != nil {
		return 0, fmt.Errorf("failed to begin transaction: %w", err)
	}
	defer tx.Rollback()

	stmtTx, err := tx.Prepare(stmt)
	if err != nil {
		return 0, fmt.Errorf("failed to prepare statement: %w", err)
	}

	// Insert all events
	for _, event := range events {
		_, err := stmtTx.Exec(
			event.EventID,
			event.TenantID,
			event.EventType,
			event.Source,
			event.Data,
			event.CreatedAt,
			event.PartitionKey,
		)
		if err != nil {
			return 0, fmt.Errorf("failed to insert event: %w", err)
		}
	}

	// Commit transaction
	if err := tx.Commit(); err != nil {
		return 0, fmt.Errorf("failed to commit transaction: %w", err)
	}

	elapsed := time.Since(start)
	return elapsed, nil
}

func (bi *BatchInserter) getRandomSegment() string {
	segments := []string{"standard", "premium", "enterprise", "vip", "basic"}
	return segments[rand.Intn(len(segments))]
}

func (bi *BatchInserter) getRandomPaymentMethod() string {
	methods := []string{"credit_card", "debit_card", "bank_transfer", "digital_wallet", "crypto", "paypal"}
	return methods[rand.Intn(len(methods))]
}

func (bi *BatchInserter) getRandomFraudRule() string {
	rules := []string{"VELOCITY_CHECK", "HIGH_VALUE_TRANSACTION", "LOCATION_ANOMALY", "PATTERN_MATCH", "BLACKLIST_CHECK", "DEVICE_FINGERPRINT"}
	return rules[rand.Intn(len(rules))]
}

func (bi *BatchInserter) getRandomAction() string {
	actions := []string{"BLOCK", "REVIEW", "ALERT", "WARN", "ALLOW", "QUARANTINE"}
	return actions[rand.Intn(len(actions))]
}

func (bi *BatchInserter) getRandomTier() string {
	tiers := []string{"Bronze", "Silver", "Gold", "Platinum", "Diamond", "Titanium"}
	return tiers[rand.Intn(len(tiers))]
}

func (bi *BatchInserter) getRandomPlan() string {
	plans := []string{"basic", "standard", "premium", "enterprise", "ultimate"}
	return plans[rand.Intn(len(plans))]
}

func (bi *BatchInserter) getRandomChannel() string {
	channels := []string{"web", "mobile", "api", "partner", "direct", "social"}
	return channels[rand.Intn(len(channels))]
}

func (bi *BatchInserter) generateCustomerID(tenantID string) string {
	return fmt.Sprintf("cust-%s-%06d", tenantID, rand.Intn(100000))
}

func (bi *BatchInserter) generateEmail(tenantID string) string {
	return fmt.Sprintf("user%d@%s.example.com", rand.Intn(1000000), tenantID)
}

func (bi *BatchInserter) generateTransactionID() string {
	return fmt.Sprintf("txn-%d", rand.Intn(1000000000))
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

	// Create database connection string
	dsn := fmt.Sprintf("host=%s port=%d user=%s password=%s dbname=%s sslmode=disable",
		pgHost, pgPort, pgUser, pgPassword, pgDB)

	// Create batch inserter
	batchSize := 1000
	numWorkers := 10
	inserter, err := NewBatchInserter(dsn, batchSize, numWorkers)
	if err != nil {
		log.Fatalf("Failed to create batch inserter: %v", err)
	}
	defer inserter.Close()

	log.Printf("Connected to PostgreSQL at %s:%d", pgHost, pgPort)
	log.Printf("Batch size: %d, Workers: %d", batchSize, numWorkers)

	// Create statistics tracker
	stats := &PostgresStats{
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
				log.Printf("Stats: Total=%d, Success=%d, Errors=%d, Rate=%.2f inserts/sec",
					stats.TotalInserts, stats.SuccessCount, stats.ErrorCount, stats.InsertsPerSec)
			case <-ctx.Done():
				return
			}
		}
	}()

	// Start inserters for multiple tenants
	numTenants := 5
	tenants := make([]string, numTenants)
	for i := 0; i < numTenants; i++ {
		tenants[i] = fmt.Sprintf("tenant-%03d", i+1)
	}

	// Start all workers
	for _, tenant := range tenants {
		inserter.StartInserter(tenant, stats)
	}

	// Wait for duration or cancellation
	<-ctx.Done()

	// Final statistics
	stats.UpdateRate()
	elapsed := time.Since(stats.StartTime).Seconds()
	log.Printf("\n=== Simulation completed ===")
	log.Printf("Duration: %.2f seconds", elapsed)
	log.Printf("Total inserts: %d", stats.TotalInserts)
	log.Printf("Success: %d", stats.SuccessCount)
	log.Printf("Errors: %d", stats.ErrorCount)
	log.Printf("Final rate: %.2f inserts/sec", stats.InsertsPerSec)
	log.Printf("Database stats: %+v", inserter.db.Stats())
}
