# Implementation Todos - Gap Resolution

**Date:** 2025-11-07
**Priority:** Based on gap analysis and system impact
**Estimated Total Effort:** 5-6 weeks

---

## Phase 1: Core Event Infrastructure (Priority 1 - CRITICAL)

### 1.1 Implement Kafka Event Consumers
**Status:** TODO | **Priority:** P0 - CRITICAL | **Effort:** 3-5 days

#### 1.1.1 CustomerEventConsumer
- [ ] Remove stub implementation
- [ ] Add Kafka consumer configuration
- [ ] Implement CloudEvents deserialization
- [ ] Add error handling with retry logic
- [ ] Add event deduplication (by event ID)
- [ ] Add logging and metrics
- [ ] Write unit tests

**File:** `/home/labadmin/projects/droid-spring/backend/src/main/java/com/droid/bss/infrastructure/messaging/CustomerEventConsumer.java`

#### 1.1.2 OrderEventConsumer
- [ ] Remove stub implementation
- [ ] Implement event handling
- [ ] Add retry logic
- [ ] Add metrics
- [ ] Write unit tests

**File:** `/home/labadmin/projects/droid-spring/backend/src/main/java/com/droid/bss/infrastructure/messaging/OrderEventConsumer.java`

#### 1.1.3 InvoiceEventConsumer
- [ ] Create new file
- [ ] Implement event handling
- [ ] Add retry logic
- [ ] Add metrics
- [ ] Write unit tests

**File:** `/home/labadmin/projects/droid-spring/backend/src/main/java/com/droid/bss/infrastructure/messaging/InvoiceEventConsumer.java`

#### 1.1.4 PaymentEventConsumer
- [ ] Create new file
- [ ] Implement event handling
- [ ] Add retry logic
- [ ] Add metrics
- [ ] Write unit tests

**File:** `/home/labadmin/projects/droid-spring/backend/src/main/java/com/droid/bss/infrastructure/messaging/PaymentEventConsumer.java`

#### 1.1.5 SubscriptionEventConsumer
- [ ] Create new file
- [ ] Implement event handling
- [ ] Add retry logic
- [ ] Add metrics
- [ ] Write unit tests

**File:** `/home/labadmin/projects/droid-spring/backend/src/main/java/com/droid/bss/infrastructure/messaging/SubscriptionEventConsumer.java`

#### 1.1.6 ServiceEventConsumer
- [ ] Create new file
- [ ] Implement event handling
- [ ] Add retry logic
- [ ] Add metrics
- [ ] Write unit tests

**File:** `/home/labadmin/projects/droid-spring/backend/src/main/java/com/droid/bss/infrastructure/messaging/ServiceEventConsumer.java`

### 1.2 Enable Frontend Event Listeners
**Status:** TODO | **Priority:** P0 - CRITICAL | **Effort:** 1 day

#### 1.2.1 Enable Invoice Event Listeners
- [ ] Uncomment invoice event listeners in events.client.ts
- [ ] Add invoiceEventsStore integration
- [ ] Test real-time invoice updates

**File:** `/home/labadmin/projects/droid-spring/frontend/app/plugins/events.client.ts`

#### 1.2.2 Enable Order Event Listeners
- [ ] Uncomment order event listeners in events.client.ts
- [ ] Add orderEventsStore integration (create if needed)
- [ ] Test real-time order updates

**File:** `/home/labadmin/projects/droid-spring/frontend/app/plugins/events.client.ts`

#### 1.2.3 Enable Service Event Listeners
- [ ] Uncomment service event listeners in events.client.ts
- [ ] Add serviceEventsStore integration (create if needed)
- [ ] Test real-time service updates

**File:** `/home/labadmin/projects/droid-spring/frontend/app/plugins/events.client.ts`

#### 1.2.4 Create Missing Event Stores
- [ ] Create invoice.events.ts store
- [ ] Create order.events.ts store
- [ ] Create service.events.ts store

**Files:**
- `/home/labadmin/projects/droid-spring/frontend/app/stores/invoice.events.ts`
- `/home/labadmin/projects/droid-spring/frontend/app/stores/order.events.ts`
- `/home/labadmin/projects/droid-spring/frontend/app/stores/service.events.ts`

### 1.3 Add Dead Letter Queue (DLQ)
**Status:** TODO | **Priority:** P0 - CRITICAL | **Effort:** 2 days

#### 1.3.1 Create DLQ Topic Configuration
- [ ] Add DLQ topic configuration to application.yaml
- [ ] Configure retry policy
- [ ] Configure max retry attempts

#### 1.3.2 Implement DLQ Handler
- [ ] Create DeadLetterQueueHandler
- [ ] Log failed events
- [ ] Add metrics for DLQ
- [ ] Implement alert on DLQ growth

**File:** `/home/labadmin/projects/droid-spring/backend/src/main/java/com/droid/bss/infrastructure/messaging/DeadLetterQueueHandler.java`

### 1.4 Add Event Metrics
**Status:** TODO | **Priority:** P1 - HIGH | **Effort:** 1 day

#### 1.4.1 Add Event Publishing Metrics
- [ ] Add Counter for events published
- [ ] Add Timer for event publishing duration
- [ ] Add Gauge for Kafka queue size
- [ ] Add error rate metrics

#### 1.4.2 Add Event Consumption Metrics
- [ ] Add Counter for events consumed
- [ ] Add Timer for event processing duration
- [ ] Add Counter for events failed
- [ ] Add Gauge for consumer lag

**File:** `/home/labadmin/projects/droid-spring/backend/src/main/java/com/droid/bss/infrastructure/messaging/EventMetrics.java`

---

## Phase 2: Caching Layer (Priority 1 - CRITICAL)

### 2.1 Add @Cacheable to Query Services
**Status:** TODO | **Priority:** P0 - CRITICAL | **Effort:** 2-3 days

#### 2.1.1 Customer Query Service
- [ ] Add @Cacheable to findById
- [ ] Add @Cacheable to findAll
- [ ] Add @Cacheable to search methods
- [ ] Define cache names and TTL

**File:** `/home/labadmin/projects/droid-spring/backend/src/main/java/com/droid/bss/application/query/customer/CustomerQueryService.java`

#### 2.1.2 Order Query Service
- [ ] Add @Cacheable to findById
- [ ] Add @Cacheable to findByCustomerId
- [ ] Add @Cacheable to search methods

**File:** `/home/labadmin/projects/droid-spring/backend/src/main/java/com/droid/bss/application/query/order/OrderQueryService.java`

#### 2.1.3 Invoice Query Service
- [ ] Add @Cacheable to findById
- [ ] Add @Cacheable to findByCustomerId
- [ ] Add @Cacheable to search methods

**File:** `/home/labadmin/projects/droid-spring/backend/src/main/java/com/droid/bss/application/query/invoice/InvoiceQueryService.java`

#### 2.1.4 Payment Query Service
- [ ] Add @Cacheable to findById
- [ ] Add @Cacheable to findByCustomerId

**File:** `/home/labadmin/projects/droid-spring/backend/src/main/java/com/droid/bss/application/query/payment/`

#### 2.1.5 Product Query Service
- [ ] Add @Cacheable to findById
- [ ] Add @Cacheable to findAll
- [ ] Add @Cacheable to search methods

**File:** `/home/labadmin/projects/droid-spring/backend/src/main/java/com/droid/bss/infrastructure/read/product/ProductQueryServiceImpl.java`

### 2.2 Implement Cache Invalidation
**Status:** TODO | **Priority:** P0 - CRITICAL | **Effort:** 2 days

#### 2.2.1 Add @CacheEvict to Command Handlers
- [ ] UpdateCustomerUseCase - evict customer cache
- [ ] CreateCustomerUseCase - evict customer list cache
- [ ] CreateOrderUseCase - evict order cache
- [ ] CreateInvoiceUseCase - evict invoice cache
- [ ] CreatePaymentUseCase - evict payment cache

**Files:**
- `/home/labadmin/projects/droid-spring/backend/src/main/java/com/droid/bss/application/command/customer/UpdateCustomerUseCase.java`
- `/home/labadmin/projects/droid-spring/backend/src/main/java/com/droid/bss/application/command/order/CreateOrderUseCase.java`
- `/home/labadmin/projects/droid-spring/backend/src/main/java/com/droid/bss/application/command/invoice/CreateInvoiceUseCase.java`
- `/home/labadmin/projects/droid-spring/backend/src/main/java/com/droid/bss/application/command/payment/CreatePaymentUseCase.java`

#### 2.2.2 Create Cache Key Strategy
- [ ] Define cache key naming convention
- [ ] Add cache key builder utility
- [ ] Document cache key strategy

**File:** `/home/labadmin/projects/droid-spring/backend/src/main/java/com/droid/bss/infrastructure/cache/CacheKeyStrategy.java`

### 2.3 Add Cache Metrics
**Status:** TODO | **Priority:** P1 - HIGH | **Effort:** 1 day

#### 2.3.1 Add Cache Hit/Miss Metrics
- [ ] Add cache hit counter
- [ ] Add cache miss counter
- [ ] Add cache hit rate gauge
- [ ] Add cache size gauge

#### 2.3.2 Add Cache Eviction Metrics
- [ ] Add eviction counter
- [ ] Add eviction reason tags
- [ ] Add time-to-live metrics

**File:** `/home/labadmin/projects/droid-spring/backend/src/main/java/com/droid/bss/infrastructure/cache/CacheMetrics.java`

### 2.4 Implement Cache Warming
**Status:** TODO | **Priority:** P1 - HIGH | **Effort:** 1-2 days

#### 2.4.1 Create Cache Warming Service
- [ ] Create CacheWarmingService
- [ ] Load hot data on startup
- [ ] Add scheduled warming (every 10 minutes)
- [ ] Add hot key detection

**File:** `/home/labadmin/projects/droid-spring/backend/src/main/java/com/droid/bss/infrastructure/cache/CacheWarmingService.java`

#### 2.4.2 Configure Cache Warming
- [ ] Add configuration properties
- [ ] Define hot keys list
- [ ] Add warm-up interval settings

**File:** `/home/labadmin/projects/droid-spring/backend/src/main/resources/application.yaml`

---

## Phase 3: Missing Backend APIs (Priority 2 - HIGH)

### 3.1 Product Management APIs
**Status:** TODO | **Priority:** P1 - HIGH | **Effort:** 2 days

#### 3.1.1 Add Search and Filtering
- [ ] Add search endpoint (query param)
- [ ] Add category filter
- [ ] Add status filter
- [ ] Add pagination support

**File:** `/home/labadmin/projects/droid-spring/backend/src/main/java/com/droid/bss/api/product/ProductController.java`

#### 3.1.2 Add Bulk Operations
- [ ] Add bulk create endpoint
- [ ] Add bulk update endpoint
- [ ] Add bulk delete endpoint

### 3.2 Service Activation APIs
**Status:** TODO | **Priority:** P1 - HIGH | **Effort:** 3 days

#### 3.2.1 Add Activation Progress Endpoint
- [ ] Get activation progress by ID
- [ ] Get activation steps status
- [ ] Add retry activation endpoint

**File:** `/home/labadmin/projects/droid-spring/backend/src/main/java/com/droid/bss/api/service/ServiceActivationController.java`

#### 3.2.2 Add Activation Management
- [ ] Cancel activation
- [ ] Schedule activation
- [ ] Get activation history

### 3.3 Asset Management APIs
**Status:** TODO | **Priority:** P1 - HIGH | **Effort:** 2-3 days

#### 3.3.1 Add Asset Assignment API
- [ ] Assign asset to customer
- [ ] Unassign asset
- [ ] Get asset assignment history

**File:** `/home/labadmin/projects/droid-spring/backend/src/main/java/com/droid/bss/api/asset/AssetController.java`

#### 3.3.2 Add Network Element Heartbeat
- [ ] Update heartbeat endpoint
- [ ] Get network element status
- [ ] Get offline elements

### 3.4 Monitoring API Integration
**Status:** TODO | **Priority:** P2 - MEDIUM | **Effort:** 2 days

#### 3.4.1 Connect Backend Monitoring to Frontend
- [ ] Verify metrics endpoint works
- [ ] Verify alerts endpoint works
- [ ] Add CORS support if needed
- [ ] Test from frontend

**Files:**
- `/home/labadmin/projects/droid-spring/backend/src/main/java/com/droid/bss/api/monitoring/AlertsController.java`
- `/home/labadmin/projects/droid-spring/backend/src/main/java/com/droid/bss/api/monitoring/MetricsController.java`

### 3.5 Fraud Detection UI
**Status:** TODO | **Priority:** P2 - MEDIUM | **Effort:** 3-4 days

#### 3.5.1 Create Fraud Management API
- [ ] Get fraud alerts
- [ ] Acknowledge fraud alert
- [ ] Get fraud rules
- [ ] Update fraud rule
- [ ] Create fraud rule

**File:** `/home/labadmin/projects/droid-spring/backend/src/main/java/com/droid/bss/api/fraud/FraudController.java`

#### 3.5.2 Create Fraud Management Frontend
- [ ] Create fraud dashboard page
- [ ] Create fraud alerts list component
- [ ] Create fraud rules management component

**Files:**
- `/home/labadmin/projects/droid-spring/frontend/app/pages/fraud/`
- `/home/labadmin/projects/droid-spring/frontend/app/stores/fraud.ts`
- `/home/labadmin/projects/droid-spring/frontend/app/components/fraud/`

### 3.6 Partner Management UI
**Status:** TODO | **Priority:** P3 - LOW | **Effort:** 3-4 days

#### 3.6.1 Create Partner Management API
- [ ] CRUD operations for partners
- [ ] Settlement tracking
- [ ] Partner reporting

**File:** `/home/labadmin/projects/droid-spring/backend/src/main/java/com/droid/bss/api/partner/PartnerController.java`

#### 3.6.2 Create Partner Management Frontend
- [ ] Create partner list page
- [ ] Create partner detail page
- [ ] Create settlement tracking page

**Files:**
- `/home/labadmin/projects/droid-spring/frontend/app/pages/partners/`
- `/home/labadmin/projects/droid-spring/frontend/app/stores/partner.ts`

### 3.7 Workforce Management UI
**Status:** TODO | **Priority:** P3 - LOW | **Effort:** 4-5 days

#### 3.7.1 Create Workforce Management API
- [ ] Employee CRUD
- [ ] Work order management
- [ ] Scheduling

**File:** `/home/labadmin/projects/droid-spring/backend/src/main/java/com/droid/bss/api/workforce/WorkforceController.java`

#### 3.7.2 Create Workforce Management Frontend
- [ ] Create employee management page
- [ ] Create work order page
- [ ] Create scheduling page

**Files:**
- `/home/labadmin/projects/droid-spring/frontend/app/pages/workforce/`
- `/home/labadmin/projects/droid-spring/frontend/app/stores/employee.ts`

---

## Phase 4: Frontend Enhancements (Priority 2 - HIGH)

### 4.1 Add Bulk Operations UI
**Status:** TODO | **Priority:** P1 - HIGH | **Effort:** 2-3 days

#### 4.1.1 Customer Bulk Operations
- [ ] Add bulk select to customer list
- [ ] Add bulk status change
- [ ] Add bulk delete

**File:** `/home/labadmin/projects/droid-spring/frontend/app/pages/customers/index.vue`

#### 4.1.2 Product Bulk Operations
- [ ] Add bulk select to product list
- [ ] Add bulk activate/deactivate
- [ ] Add bulk delete

### 4.2 Add Export Functionality
**Status:** TODO | **Priority:** P2 - MEDIUM | **Effort:** 2 days

#### 4.2.1 CSV Export
- [ ] Add CSV export to all list pages
- [ ] Add filtered export
- [ ] Add pagination handling

#### 4.2.2 PDF Export
- [ ] Add PDF export for invoices
- [ ] Add PDF export for reports

### 4.3 Add Offline Support
**Status:** TODO | **Priority:** P2 - MEDIUM | **Effort:** 3-4 days

#### 4.3.1 Service Worker
- [ ] Register service worker
- [ ] Cache static assets
- [ ] Cache API responses
- [ ] Add offline fallback

**File:** `/home/labadmin/projects/droid-spring/frontend/app/plugins/sw.client.ts`

#### 4.3.2 Optimistic Updates
- [ ] Add optimistic updates for create
- [ ] Add optimistic updates for update
- [ ] Handle rollback on error

### 4.4 Add User Preferences
**Status:** TODO | **Priority:** P3 - LOW | **Effort:** 2 days

#### 4.4.1 Preferences API
- [ ] Save user preferences
- [ ] Load user preferences
- [ ] Default preferences

**File:** `/home/labadmin/projects/droid-spring/backend/src/main/java/com/droid/bss/api/user/UserPreferencesController.java`

#### 4.4.2 Preferences UI
- [ ] Create preferences page
- [ ] Add theme selection
- [ ] Add table preferences
- [ ] Add notification preferences

**File:** `/home/labadmin/projects/droid-spring/frontend/app/pages/settings/preferences.vue`

---

## Phase 5: Event Infrastructure Enhancements (Priority 2 - HIGH)

### 5.1 Implement Event Replay Service
**Status:** TODO | **Priority:** P1 - HIGH | **Effort:** 2-3 days

#### 5.1.1 Create Event Replay Controller
- [ ] Replay by date range
- [ ] Replay by event type
- [ ] Replay by aggregate ID
- [ ] Add progress tracking

**File:** `/home/labadmin/projects/droid-spring/backend/src/main/java/com/droid/bss/infrastructure/event/EventReplayController.java`

#### 5.1.2 Implement Event Store
- [ ] Create event store table
- [ ] Add event persistence
- [ ] Add event query methods
- [ ] Add event archive

**File:** `/home/labadmin/projects/droid-spring/backend/src/main/java/com/droid/bss/infrastructure/event/EventStore.java`

### 5.2 Add Event Deduplication
**Status:** TODO | **Priority:** P2 - MEDIUM | **Effort:** 1-2 days

#### 5.2.1 Create Event Deduplication Service
- [ ] Track processed event IDs
- [ ] Add TTL for event ID cache
- [ ] Check before processing

**File:** `/home/labadmin/projects/droid-spring/backend/src/main/java/com/droid/bss/infrastructure/event/EventDeduplicationService.java`

### 5.3 Improve Event Error Handling
**Status:** TODO | **Priority:** P1 - HIGH | **Effort:** 1-2 days

#### 5.3.1 Add Retry Logic
- [ ] Implement exponential backoff
- [ ] Add max retry attempts
- [ ] Add retry metrics
- [ ] Add dead letter queue

#### 5.3.2 Add Circuit Breaker
- [ ] Add circuit breaker for Kafka
- [ ] Add fallback mechanism
- [ ] Add circuit breaker metrics

**File:** `/home/labadmin/projects/droid-spring/backend/src/main/java/com/droid/bss/infrastructure/resilience/EventCircuitBreaker.java`

---

## Phase 6: Testing (Priority 1 - CRITICAL)

### 6.1 Event Consumer Tests
**Status:** TODO | **Priority:** P0 - CRITICAL | **Effort:** 3-4 days

#### 6.1.1 CustomerEventConsumer Tests
- [ ] Test event deserialization
- [ ] Test event processing
- [ ] Test error handling
- [ ] Test retry logic
- [ ] Test integration with Kafka

#### 6.1.2 OrderEventConsumer Tests
- [ ] Test event deserialization
- [ ] Test event processing
- [ ] Test error handling
- [ ] Test retry logic

#### 6.1.3 InvoiceEventConsumer Tests
- [ ] Test event deserialization
- [ ] Test event processing
- [ ] Test error handling

#### 6.1.4 PaymentEventConsumer Tests
- [ ] Test event deserialization
- [ ] Test event processing
- [ ] Test error handling

#### 6.1.5 SubscriptionEventConsumer Tests
- [ ] Test event deserialization
- [ ] Test event processing
- [ ] Test error handling

#### 6.1.6 ServiceEventConsumer Tests
- [ ] Test event deserialization
- [ ] Test event processing
- [ ] Test error handling

### 6.2 Frontend Event Tests
**Status:** TODO | **Priority:** P0 - CRITICAL | **Effort:** 2-3 days

#### 6.2.1 Event Listener Tests
- [ ] Test customer event listeners
- [ ] Test payment event listeners
- [ ] Test invoice event listeners
- [ ] Test order event listeners
- [ ] Test service event listeners

#### 6.2.2 Event Store Tests
- [ ] Test invoice.events.ts
- [ ] Test order.events.ts
- [ ] Test service.events.ts

### 6.3 Cache Tests
**Status:** TODO | **Priority:** P0 - CRITICAL | **Effort:** 2-3 days

#### 6.3.1 Cache Integration Tests
- [ ] Test @Cacheable
- [ ] Test @CacheEvict
- [ ] Test cache invalidation
- [ ] Test cache warming

#### 6.3.2 Cache Performance Tests
- [ ] Test cache hit rate
- [ ] Test cache performance
- [ ] Test memory usage

### 6.4 API Tests
**Status:** TODO | **Priority:** P1 - HIGH | **Effort:** 3-4 days

#### 6.4.1 New Endpoint Tests
- [ ] Test product search API
- [ ] Test service activation API
- [ ] Test asset assignment API
- [ ] Test monitoring API

#### 6.4.2 Integration Tests
- [ ] Test end-to-end event flow
- [ ] Test cache consistency
- [ ] Test real-time updates

---

## Implementation Order

### Week 1: Core Event Infrastructure
1. CustomerEventConsumer (1 day)
2. OrderEventConsumer (1 day)
3. InvoiceEventConsumer (1 day)
4. Enable frontend event listeners (1 day)
5. Add DLQ and metrics (1 day)

### Week 2: Caching Layer
1. Add @Cacheable to query services (2 days)
2. Add @CacheEvict to command handlers (1 day)
3. Add cache metrics (1 day)
4. Add cache warming (1 day)

### Week 3: Missing APIs
1. Product management APIs (2 days)
2. Service activation APIs (3 days)
3. Asset management APIs (2 days)

### Week 4: Frontend Enhancements
1. Bulk operations UI (2 days)
2. Export functionality (2 days)
3. Fraud management UI (3 days)

### Week 5: Event Infrastructure
1. Event replay service (3 days)
2. Event deduplication (2 days)
3. Improved error handling (2 days)

### Week 6: Testing & Polish
1. Event consumer tests (4 days)
2. Frontend event tests (2 days)
3. Cache tests (2 days)
4. API tests (2 days)

---

## Definition of Done

### For Each Event Consumer:
- [ ] Code implemented
- [ ] Unit tests written
- [ ] Integration tests passed
- [ ] Error handling tested
- [ ] Retry logic tested
- [ ] Metrics added
- [ ] Documentation updated
- [ ] Deployed to dev environment
- [ ] Frontend integration tested
- [ ] Load tested

### For Each Cache Implementation:
- [ ] @Cacheable added
- [ ] Cache eviction tested
- [ ] Cache invalidation tested
- [ ] Performance improvement measured
- [ ] Memory usage acceptable
- [ ] Metrics added
- [ ] Documentation updated

### For Each API:
- [ ] Endpoint implemented
- [ ] Validation added
- [ ] Unit tests written
- [ ] Integration tests passed
- [ ] OpenAPI spec updated
- [ ] Frontend integration completed
- [ ] Documentation updated

---

## Success Metrics

### Event Infrastructure:
- [ ] 100% of event types have consumers
- [ ] 0% event loss
- [ ] < 1 second event propagation time
- [ ] 99.9% event delivery success rate
- [ ] < 1% DLQ growth rate

### Caching:
- [ ] 90%+ cache hit rate
- [ ] 50%+ reduction in DB queries
- [ ] < 100ms average response time
- [ ] 0% stale data incidents
- [ ] Memory usage < 1GB

### APIs:
- [ ] 100% API coverage
- [ ] 95%+ test coverage
- [ ] 0% breaking changes
- [ ] < 200ms average response time
- [ ] 99.9% uptime

---

## Risk Mitigation

### Risk: Event Consumers Corrupt Data
**Mitigation:**
- Comprehensive testing
- Event idempotency
- Rollback mechanism
- DLQ for failed events

### Risk: Cache Invalidation Bugs
**Mitigation:**
- Thorough testing
- Cache key strategy
- Monitoring and alerts
- Gradual rollout

### Risk: Performance Impact
**Mitigation:**
- Load testing
- Performance monitoring
- Gradual rollout
- Rollback plan

### Risk: Breaking Changes
**Mitigation:**
- API versioning
- Backward compatibility
- Comprehensive testing
- Communication plan

---

**Total Estimated Effort: 5-6 weeks**
**Priority Focus: Phase 1 & Phase 2 (4 weeks)**
**Ready to Start: YES**

---

## Next Action Items

1. ✅ Gap analysis complete
2. ✅ Todo list created
3. 🔄 **Start Phase 1.1 - Implement CustomerEventConsumer**
4. Then: Phase 1.2 - Enable Frontend Event Listeners
5. Then: Phase 1.3 - Add DLQ
6. Then: Phase 2.1 - Add @Cacheable
7. Continue systematically through all phases
