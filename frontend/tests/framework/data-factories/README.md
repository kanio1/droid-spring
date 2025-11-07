# Enhanced Data Factories - Phase 2 Implementation

## Overview

This document describes the Phase 2 enhancement of the Playwright E2E Testing Framework's data factories. The enhanced system provides advanced features for test data generation, management, and automation.

## Architecture

### Core Components

#### 1. **Data Correlator** (`data-correlator.ts`)
Manages entity relationships and ensures data consistency across the test dataset.

**Key Features:**
- Entity registration and linking
- Foreign key management
- Relationship validation
- Dataset export

**Usage:**
```typescript
import { dataCorrelator } from './data-factories'

// Register entities
dataCorrelator.registerEntity('customer', customer)
dataCorrelator.registerEntity('order', order)

// Link entities
dataCorrelator.linkEntities('customer', customer.id, 'order', order.id)

// Validate relationships
const validation = dataCorrelator.validateRelationships()
console.log(validation.valid) // true or false

// Export dataset
const dataset = dataCorrelator.exportDataset()
```

#### 2. **Scenario Builder** (`scenario-builder.ts`)
Provides predefined test scenarios for common use cases.

**Available Scenarios:**

- **Full Customer Journey** - Complete lifecycle from customer to payment
- **Enterprise Scenario** - Large orders and premium customers
- **Failed Payment Scenario** - Payment failures and retries
- **Trial Conversion Scenario** - Trial to paid subscription flow
- **Bulk Data Scenario** - Large volumes for performance testing

**Usage:**
```typescript
import { ScenarioBuilder } from './data-factories'

// Full customer journey
const data = ScenarioBuilder.fullCustomerJourney({
  customerCount: 10,
  ordersPerCustomer: 2,
  includeFailedPayments: true
})

// Enterprise scenario
const enterpriseData = ScenarioBuilder.enterpriseScenario({
  customerCount: 5
})

// Failed payment scenario
const failedPayments = ScenarioBuilder.failedPaymentScenario({
  customerCount: 3
})
```

#### 3. **Unique Data Pool** (`unique-data-pool.ts`)
Prevents test data collisions by tracking used values.

**Key Features:**
- Unique email generation
- Unique phone numbers
- Unique invoice numbers
- Unique transaction IDs
- Custom ID tracking

**Usage:**
```typescript
import { uniqueDataPool, DataGenerators } from './data-factories'

// Generate unique data
const email = DataGenerators.uniqueCustomerEmail()
const phone = DataGenerators.uniqueCustomerPhone()
const invoiceNumber = DataGenerators.uniqueInvoiceNumber()

// Check uniqueness
const isUnique = uniqueDataPool.isEmailUnique('test@example.com')

// Get statistics
const stats = uniqueDataPool.getStatistics()
```

#### 4. **Bulk Generator** (`bulk-generator.ts`)
Efficiently generates large volumes of test data (1000+ entities).

**Key Features:**
- Batch processing to prevent memory issues
- Async/await support for non-blocking generation
- Configurable generation parameters
- Performance statistics

**Usage:**
```typescript
import { BulkGenerator } from './data-factories'

// Generate bulk data
const generator = new BulkGenerator({
  customerCount: 1000,
  ordersPerCustomer: 5,
  invoicesPerOrder: 1,
  paymentsPerInvoice: 1,
  subscriptionsPerCustomer: 1,
  batchSize: 100
})

const data = await generator.generateAll()
console.log(`Generated ${data.statistics.customersCreated} customers`)

// Get generation statistics
const stats = generator.getStatistics()
```

#### 5. **Database Seeding** (`database-seeding.ts`)
Automates environment setup and teardown for tests.

**Key Features:**
- Multiple seeding scenarios
- Active seed management
- Automatic cleanup
- Test type-specific seeding

**Usage:**
```typescript
import { databaseSeeder, SeedHelper } from './data-factories'

// Manual seeding
const seed = await databaseSeeder.seed({
  scenario: 'fullJourney',
  customerCount: 10,
  seedId: 'test-run-001'
})

// Convenience methods for different test types
const unitTestSeed = await SeedHelper.forUnitTest()
const e2eSeed = await SeedHelper.forE2ETest()
const performanceSeed = await SeedHelper.forPerformanceTest()

// Cleanup specific seed
await databaseSeeder.cleanup(seed.seedId)

// Cleanup all seeds
await databaseSeeder.cleanupAll()
```

## TestDataGenerator - Enhanced API

The `TestDataGenerator` class now provides both legacy and enhanced APIs.

### Legacy API (Backward Compatible)

```typescript
import { TestDataGenerator } from './data-factories'

// Old way - still works
const { customer, order } = TestDataGenerator.fullCustomerJourney()
const batch = TestDataGenerator.generateBatch(5)
const enterprise = TestDataGenerator.enterpriseScenario()
```

### Enhanced API

```typescript
import { TestDataGenerator } from './data-factories'

// Scenario builder access
const data = TestDataGenerator.scenario.fullCustomerJourney({
  customerCount: 10
})

// Bulk generation
const bulkData = await TestDataGenerator.generateBulk({
  customerCount: 100,
  ordersPerCustomer: 3
})

// Database seeding
const seed = await TestDataGenerator.seed({
  scenario: 'enterprise',
  customerCount: 5
})

// Test type-specific seeding
const e2eSeed = await TestDataGenerator.seedForE2ETest()
const perfSeed = await TestDataGenerator.seedForPerformanceTest()
```

## Complete Example

Here's a comprehensive example showing how to use all features together:

```typescript
import {
  TestDataGenerator,
  ScenarioBuilder,
  BulkGenerator,
  databaseSeeder,
  dataCorrelator
} from './data-factories'

test.describe('Enhanced Data Factories Demo', () => {
  let seed: SeedResult

  test.beforeAll(async () => {
    // Seed database for all tests
    seed = await TestDataGenerator.seedForE2ETest()
  })

  test.afterAll(async () => {
    // Cleanup after all tests
    await databaseSeeder.cleanup(seed.seedId)
  })

  test('Use scenario builder', async ({ page }) => {
    // Generate data for this specific test
    const data = ScenarioBuilder.fullCustomerJourney({
      customerCount: 5,
      includeFailedPayments: true
    })

    // Use the data in tests
    for (const customer of data.customers) {
      await page.goto(`/customers/${customer.id}`)
      await expect(page.locator('[data-testid="customer-name"]'))
        .toContainText(`${customer.firstName} ${customer.lastName}`)
    }
  })

  test('Use unique data pool', async ({ page }) => {
    // Generate unique test data
    const customer = CustomerFactory.create()
      .withEmail(DataGenerators.uniqueCustomerEmail())
      .withPhone(DataGenerators.uniqueCustomerPhone())
      .active()
      .build()

    // Data is guaranteed to be unique
    expect(customer.email).toBeTruthy()
    expect(customer.phone).toBeTruthy()
  })

  test('Use bulk generator for performance testing', async () => {
    // Generate large dataset
    const bulkData = await TestDataGenerator.generateBulk({
      customerCount: 100,
      ordersPerCustomer: 5,
      batchSize: 50
    })

    // Verify data was generated
    expect(bulkData.customers.length).toBe(100)
    expect(bulkData.orders.length).toBe(500)
    expect(bulkData.statistics.customersCreated).toBe(100)
  })

  test('Validate entity relationships', async () => {
    // Generate data
    const data = ScenarioBuilder.fullCustomerJourney({ customerCount: 3 })

    // Validate relationships
    const validation = dataCorrelator.validateRelationships()

    // Check statistics
    const stats = dataCorrelator.getStatistics()
    console.log('Entity counts:', stats)

    // All relationships should be valid
    expect(validation.valid).toBe(true)
  })
})
```

## Best Practices

### 1. **Use Database Seeding for E2E Tests**
```typescript
test.beforeAll(async () => {
  await TestDataGenerator.seedForE2ETest()
})
```

### 2. **Use Scenario Builder for Integration Tests**
```typescript
test('Integration test', async () => {
  const data = ScenarioBuilder.fullCustomerJourney()
  // Test with data
})
```

### 3. **Use Unique Data Pool for Unit Tests**
```typescript
test('Unit test', () => {
  const data = CustomerFactory.create()
    .withEmail(DataGenerators.uniqueCustomerEmail())
    .build()
  // Test with unique data
})
```

### 4. **Use Bulk Generator for Performance Tests**
```typescript
test('Performance test', async () => {
  const bulkData = await TestDataGenerator.generateBulk({
    customerCount: 1000
  })
  // Performance test with bulk data
})
```

### 5. **Always Clean Up After Tests**
```typescript
test.afterAll(async () => {
  await databaseSeeder.cleanupAll()
})
```

## Migration Guide

### From Legacy API to Enhanced API

**Old Code:**
```typescript
const { customer, order } = TestDataGenerator.fullCustomerJourney()
```

**New Code:**
```typescript
// Option 1: Legacy API (still works)
const { customer, order } = TestDataGenerator.fullCustomerJourney()

// Option 2: Enhanced API
const data = TestDataGenerator.scenario.fullCustomerJourney()
const { customer, order } = data
```

**Benefits of Enhanced API:**
- Automatic entity relationship management
- Unique data generation
- Better performance with bulk operations
- Automatic cleanup
- More scenarios and flexibility

## Configuration

### Environment Variables

```bash
# Test data configuration
TEST_DATA_UNIQUE_DOMAIN=test.example
TEST_DATA_BATCH_SIZE=100
TEST_DATA_CLEANUP=true

# Performance tuning
GENERATION_PARALLELISM=4
MAX_GENERATION_TIME=30000
```

## Statistics and Monitoring

All generators provide statistics:

```typescript
const stats = {
  // DataCorrelator
  entityCounts: dataCorrelator.getStatistics(),

  // UniqueDataPool
  uniqueCounts: uniqueDataPool.getStatistics(),

  // BulkGenerator
  generationStats: generator.getStatistics(),

  // DatabaseSeeder
  activeSeeds: databaseSeeder.getStatistics()
}

console.log('Test Data Statistics:', stats)
```

## Troubleshooting

### Common Issues

1. **Duplicate Data Errors**
   - Solution: Use `DataGenerators` for unique data
   - Or call `uniqueDataPool.clear()` before generation

2. **Memory Issues with Large Datasets**
   - Solution: Use `BulkGenerator` with smaller `batchSize`
   - Default: 100, try 50 for large datasets

3. **Relationship Validation Fails**
   - Solution: Ensure all entities are registered in `DataCorrelator`
   - Use `dataCorrelator.linkEntities()` to establish relationships

4. **Cleanup Failures**
   - Solution: Always call `databaseSeeder.cleanupAll()` in `test.afterAll`
   - Or use `test.teardown()` hook

## Future Enhancements

Planned features for Phase 3:
- [ ] Database synchronization
- [ ] Test data versioning
- [ ] Real-time data validation
- [ ] GraphQL schema generation
- [ ] AI-powered test data generation
- [ ] Advanced data masking for PII
- [ ] Cross-environment data synchronization

## References

- [Playwright Test](https://playwright.dev/docs/test-core)
- [Object Mother Pattern](https://martinfowler.com/bliki/ObjectMother.html)
- [Test Data Builder Pattern](https://www.natpryce.com/articles/000714.html)

---

**Phase 2 Status:** ✅ Complete

**Total Components:** 5 new modules + 1 enhanced module

**Lines of Code:** ~2,800 lines

**Test Coverage:** Ready for 1000+ E2E tests
