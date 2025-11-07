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

	"github.com/segmentio/kafka-go"
	cloudevents "github.com/cloudevents/sdk-go/v2"
)

const (
	kafkaURL = "localhost:9092"
	topic    = "cloud-events"
)

type EventConfig struct {
	Tenants            int     `json:"tenants"`
	EventsPerTenant    int     `json:"events_per_tenant"`
	DurationMinutes    int     `json:"duration_minutes"`
	BatchSize          int     `json:"batch_size"`
	Compression        string  `json:"compression"` // none, gzip, snappy
	EnableTransactions bool    `json:"enable_transactions"`
	Throughput         float64 `json:"throughput"` // events per second
}

type PaymentEvent struct {
	EventType    string    `json:"event_type"`
	TenantID     string    `json:"tenant_id"`
	Amount       float64   `json:"amount"`
	Currency     string    `json:"currency"`
	PaymentMethod string   `json:"payment_method"`
	CustomerID   string    `json:"customer_id"`
	Timestamp    time.Time `json:"timestamp"`
}

type OrderEvent struct {
	EventType   string    `json:"event_type"`
	TenantID    string    `json:"tenant_id"`
	OrderID     string    `json:"order_id"`
	CustomerID  string    `json:"customer_id"`
	Items       []Item    `json:"items"`
	Status      string    `json:"status"`
	TotalAmount float64   `json:"total_amount"`
	Timestamp   time.Time `json:"timestamp"`
}

type Item struct {
	ProductID string  `json:"product_id"`
	Quantity  int     `json:"quantity"`
	UnitPrice float64 `json:"unit_price"`
}

type InvoiceEvent struct {
	EventType  string    `json:"event_type"`
	TenantID   string    `json:"tenant_id"`
	InvoiceID  string    `json:"invoice_id"`
	OrderID    string    `json:"order_id"`
	Amount     float64   `json:"amount"`
	DueDate    time.Time `json:"due_date"`
	Status     string    `json:"status"`
	Timestamp  time.Time `json:"timestamp"`
}

type FraudEvent struct {
	EventType  string    `json:"event_type"`
	TenantID   string    `json:"tenant_id"`
	AlertID    string    `json:"alert_id"`
	Severity   string    `json:"severity"`
	RuleType   string    `json:"rule_type"`
	RiskScore  float64   `json:"risk_score"`
	CustomerID string    `json:"customer_id"`
	Details    string    `json:"details"`
	Timestamp  time.Time `json:"timestamp"`
}

type GeneratorStats struct {
	TotalEvents    int64     `json:"total_events"`
	SuccessCount   int64     `json:"success_count"`
	ErrorCount     int64     `json:"error_count"`
	StartTime      time.Time `json:"start_time"`
	EventsPerSec   float64   `json:"events_per_sec"`
	mu             sync.Mutex
}

func (s *GeneratorStats) AddEvent(success bool) {
	s.mu.Lock()
	defer s.mu.Unlock()
	if success {
		s.SuccessCount++
	} else {
		s.ErrorCount++
	}
	s.TotalEvents++
}

func (s *GeneratorStats) UpdateRate() {
	s.mu.Lock()
	defer s.mu.Unlock()
	elapsed := time.Since(s.StartTime).Seconds()
	if elapsed > 0 {
		s.EventsPerSec = float64(s.SuccessCount) / elapsed
	}
}

func main() {
	// Load config
	config := loadConfig()

	// Setup signal handling for graceful shutdown
	ctx, cancel := context.WithCancel(context.Background())
	defer cancel()

	c := make(chan os.Signal, 1)
	signal.Notify(c, os.Interrupt, syscall.SIGTERM)
	go func() {
		<-c
		log.Println("Received shutdown signal, stopping generator...")
		cancel()
	}()

	// Create Kafka writer
	writer := createKafkaWriter(config)
	defer writer.Close()

	// Create statistics tracker
	stats := &GeneratorStats{
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
				log.Printf("Stats: Total=%d, Success=%d, Errors=%d, Rate=%.2f events/sec",
					stats.TotalEvents, stats.SuccessCount, stats.ErrorCount, stats.EventsPerSec)
			case <-ctx.Done():
				return
			}
		}
	}()

	// Run load test
	runLoadTest(ctx, writer, config, stats)

	// Final statistics
	stats.UpdateRate()
	elapsed := time.Since(stats.StartTime).Seconds()
	log.Printf("Load test completed in %.2f seconds", elapsed)
	log.Printf("Final stats: Total=%d, Success=%d, Errors=%d, Rate=%.2f events/sec",
		stats.TotalEvents, stats.SuccessCount, stats.ErrorCount, stats.EventsPerSec)
}

func loadConfig() EventConfig {
	config := EventConfig{
		Tenants:            5,
		EventsPerTenant:    80000,
		DurationMinutes:    1,
		BatchSize:          100,
		Compression:        "snappy",
		EnableTransactions: false,
		Throughput:         6667,
	}

	return config
}

func createKafkaWriter(config EventConfig) *kafka.Writer {
	dialer := &kafka.Dialer{
		Timeout:   10 * time.Second,
		DualStack: true,
	}

	compression := kafka.CompressionNone
	switch config.Compression {
	case "gzip":
		compression = kafka.CompressionGZIP
	case "snappy":
		compression = kafka.CompressionSnappy
	case "lz4":
		compression = kafka.CompressionLZ4
	case "zstd":
		compression = kafka.CompressionZSTD
	}

	return &kafka.Writer{
		Addr:         kafka.TCP(kafkaURL),
		Topic:        topic,
		Balancer:     &kafka.LeastBytes{},
		WriteTimeout: 10 * time.Second,
		ReadTimeout:  10 * time.Second,
		RequiredAcks: kafka.RequireAll,
		BatchSize:    config.BatchSize,
		BatchTimeout: 5 * time.Millisecond,
		Compression:  compression,
		Async:        false, // Synchronous for accurate counting
	}
}

func runLoadTest(ctx context.Context, writer *kafka.Writer, config EventConfig, stats *GeneratorStats) {
	log.Printf("Starting load test: %d tenants, %d events/tenant, duration %d min",
		config.Tenants, config.EventsPerTenant, config.DurationMinutes)
	log.Printf("Target throughput: %.2f events/sec", config.Throughput)

	startTime := time.Now()
	endTime := startTime.Add(time.Duration(config.DurationMinutes) * time.Minute)
	eventsPerTenant := config.EventsPerTenant
	tenants := make([]string, config.Tenants)

	// Generate tenant IDs
	for i := 0; i < config.Tenants; i++ {
		tenants[i] = fmt.Sprintf("tenant-%03d", i+1)
	}

	// Calculate events per second per tenant
	eventsPerSecPerTenant := config.Throughput / float64(config.Tenants)
	interval := time.Duration(float64(time.Second) / eventsPerSecPerTenant)

	ticker := time.NewTicker(interval)
	defer ticker.Stop()

	for time.Now().Before(endTime) {
		select {
		case <-ctx.Done():
			log.Println("Load test cancelled")
			return
		case <-ticker.C:
			for _, tenant := range tenants {
				// Generate random event type
				eventType := rand.Intn(4) // 0: payment, 1: order, 2: invoice, 3: fraud

				var err error
				switch eventType {
				case 0: // Payment
					err = generatePaymentEvent(writer, tenant)
				case 1: // Order
					err = generateOrderEvent(writer, tenant)
				case 2: // Invoice
					err = generateInvoiceEvent(writer, tenant)
				case 3: // Fraud
					err = generateFraudEvent(writer, tenant)
				}

				if err != nil {
					log.Printf("Error generating event: %v", err)
					stats.AddEvent(false)
				} else {
					stats.AddEvent(true)
				}
			}
		}
	}
}

func generatePaymentEvent(writer *kafka.Writer, tenantID string) error {
	event := PaymentEvent{
		EventType:     "payment.created",
		TenantID:      tenantID,
		Amount:        round(rand.Float64()*1000, 2),
		Currency:      "USD",
		PaymentMethod: getRandomPaymentMethod(),
		CustomerID:    fmt.Sprintf("customer-%s-%06d", tenantID, rand.Intn(1000000)),
		Timestamp:     time.Now(),
	}

	return sendCloudEvent(writer, tenantID, event)
}

func generateOrderEvent(writer *kafka.Writer, tenantID string) error {
	itemCount := rand.Intn(5) + 1
	items := make([]Item, itemCount)
	totalAmount := 0.0

	for i := 0; i < itemCount; i++ {
		quantity := rand.Intn(5) + 1
		unitPrice := round(rand.Float64()*200, 2)
		items[i] = Item{
			ProductID: fmt.Sprintf("product-%06d", rand.Intn(100000)),
			Quantity:  quantity,
			UnitPrice: unitPrice,
		}
		totalAmount += float64(quantity) * unitPrice
	}

	event := OrderEvent{
		EventType:   "order.created",
		TenantID:    tenantID,
		OrderID:     fmt.Sprintf("order-%s-%08d", tenantID, rand.Intn(100000000)),
		CustomerID:  fmt.Sprintf("customer-%s-%06d", tenantID, rand.Intn(1000000)),
		Items:       items,
		Status:      "PENDING",
		TotalAmount: round(totalAmount, 2),
		Timestamp:   time.Now(),
	}

	return sendCloudEvent(writer, tenantID, event)
}

func generateInvoiceEvent(writer *kafka.Writer, tenantID string) error {
	event := InvoiceEvent{
		EventType: "invoice.created",
		TenantID:  tenantID,
		InvoiceID: fmt.Sprintf("inv-%s-%08d", tenantID, rand.Intn(100000000)),
		OrderID:   fmt.Sprintf("order-%s-%08d", tenantID, rand.Intn(100000000)),
		Amount:    round(rand.Float64()*2000, 2),
		DueDate:   time.Now().AddDate(0, 0, 30),
		Status:    "SENT",
		Timestamp: time.Now(),
	}

	return sendCloudEvent(writer, tenantID, event)
}

func generateFraudEvent(writer *kafka.Writer, tenantID string) error {
	rules := []string{"VELOCITY", "HIGH_VALUE", "LOCATION", "PATTERN", "BEHAVIORAL"}
	severities := []string{"LOW", "MEDIUM", "HIGH", "CRITICAL"}

	event := FraudEvent{
		EventType:  "fraud.alert.created",
		TenantID:   tenantID,
		AlertID:    fmt.Sprintf("fa-%s-%08d", tenantID, rand.Intn(100000000)),
		Severity:   severities[rand.Intn(len(severities))],
		RuleType:   rules[rand.Intn(len(rules))],
		RiskScore:  round(rand.Float64()*100, 2),
		CustomerID: fmt.Sprintf("customer-%s-%06d", tenantID, rand.Intn(1000000)),
		Details:    fmt.Sprintf("Suspicious activity detected for customer %s", tenantID),
		Timestamp:  time.Now(),
	}

	return sendCloudEvent(writer, tenantID, event)
}

func sendCloudEvent(writer *kafka.Writer, tenantID string, data interface{}) error {
	// Create CloudEvent
	event := cloudevents.NewEvent("1.0")
	event.SetID(fmt.Sprintf("ce-%d", rand.Intn(1000000000)))
	event.SetSource(fmt.Sprintf("/tenants/%s", tenantID))
	event.SetType(fmt.Sprintf("%T", data))
	event.SetTime(time.Now())
	event.SetExtension("tenantid", tenantID)

	if err := event.SetData(cloudevents.ApplicationJSON, data); err != nil {
		return fmt.Errorf("failed to set event data: %w", err)
	}

	// Convert to JSON
	jsonBytes, err := json.Marshal(event)
	if err != nil {
		return fmt.Errorf("failed to marshal event: %w", err)
	}

	// Write to Kafka
	err = writer.WriteMessages(context.Background(), kafka.Message{
		Key:   []byte(tenantID),
		Value: jsonBytes,
		Headers: []kafka.Header{
			{Key: "ce-specversion", Value: []byte("1.0")},
			{Key: "ce-type", Value: []byte(event.Type())},
			{Key: "ce-source", Value: []byte(event.Source())},
			{Key: "ce-id", Value: []byte(event.ID())},
			{Key: "ce-time", Value: []byte(event.Time().Format(time.RFC3339))},
		},
	})

	return err
}

func getRandomPaymentMethod() string {
	methods := []string{
		"credit_card",
		"debit_card",
		"bank_transfer",
		"digital_wallet",
		"cash",
	}
	return methods[rand.Intn(len(methods))]
}

func round(num float64, precision int) float64 {
	output := math.Pow(10, float64(precision))
	return math.Round(num*output) / output
}

// Import math for round function
import "math"
