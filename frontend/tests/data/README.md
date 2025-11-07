# Test Data Directory

This directory contains static test data files used across all test suites.

## 📁 File Structure

### JSON Data Files

- **`customers.json`** - Customer test data with various statuses
- **`products.json`** - Service plan products with different tiers
- **`orders.json`** - Order records linking customers to products
- **`invoices.json`** - Invoice records with different statuses
- **`payments.json`** - Payment records including successful, failed, and pending
- **`subscriptions.json`** - Subscription records with different states
- **`addresses.json`** - Customer address data
- **`test-scenarios.json`** - Complete customer journey scenarios

### CSV Data Files

- **`customer-import.csv`** - Sample data for customer import testing
- **`product-import.csv`** - Sample data for product import testing

## 🚀 Usage

### In Tests

```typescript
import customers from './data/customers.json'
import * as fs from 'fs'
import * as path from 'path'

// Load static data
const testData = JSON.parse(
  fs.readFileSync(
    path.join(__dirname, './data/customers.json'),
    'utf-8'
  )
)

// Use in test
test('should create customer', async ({ page }) => {
  const customer = testData[0] // John Doe
  await page.goto('/customers/create')
  await page.fill('[name="firstName"]', customer.firstName)
  await page.fill('[name="lastName"]', customer.lastName)
  await page.fill('[name="email"]', customer.email)
})
```

### With Data Factories

```typescript
import { CustomerFactory } from '../framework/data-factories'
import testData from './data/test-scenarios.json'

test('should handle enterprise customer', async ({ page }) => {
  // Use pre-defined scenario
  const scenario = testData.enterprise_customer

  // Or merge with factory
  const customer = CustomerFactory.create()
    .withEmail(scenario.customer.email)
    .withFirstName(scenario.customer.firstName)
    .active()
    .build()
})
```

## 📊 Data Schema

### Customer
```typescript
{
  id: string
  firstName: string
  lastName: string
  email: string
  phone: string
  status: 'active' | 'inactive' | 'pending' | 'suspended'
  createdAt: string (ISO 8601)
  metadata?: Record<string, any>
}
```

### Product
```typescript
{
  id: string
  name: string
  description: string
  price: number
  currency: string (e.g., 'USD')
  billingCycle: 'monthly' | 'yearly'
  status: 'active' | 'inactive'
  features: string[]
}
```

### Order
```typescript
{
  id: string
  customerId: string
  items: Array<{
    productId: string
    productName: string
    quantity: number
    unitPrice: number
    totalPrice: number
  }>
  totalAmount: number
  currency: string
  status: 'pending' | 'processing' | 'shipped' | 'delivered' | 'cancelled'
  createdAt: string
}
```

## 🎭 Test Scenarios

### Enterprise Customer
Complete journey for enterprise customer:
- Customer created with enterprise metadata
- Large order with multiple items
- Invoice generated
- Payment processed
- Subscription activated

### Failed Payment
Error handling scenario:
- Customer with valid order
- Invoice generated
- Payment fails (insufficient funds)
- Payment retry logic

## 💡 Best Practices

1. **Use realistic data** - Make test data as close to production as possible
2. **Avoid hard-coded values** - Use test data files instead of embedding data in tests
3. **Keep data consistent** - Use data factories to generate related data
4. **Document edge cases** - Use test-scenarios.json for complex scenarios
5. **Version control** - Keep test data in git for easy collaboration

## 🔄 Generating Test Data

You can generate additional test data using the data factories:

```typescript
// Generate single customer
const customer = CustomerFactory.create().active().build()

// Generate multiple customers
const customers = CustomerFactory.create().buildMany(10)

// Generate full customer journey
const journey = TestDataGenerator.fullCustomerJourney({
  customerStatus: 'active',
  orderStatus: 'delivered',
  invoiceStatus: 'paid',
  paymentStatus: 'completed'
})
```

## 📝 Adding New Test Data

1. Create new JSON or CSV file in this directory
2. Follow existing schema patterns
3. Update this README.md with file description
4. Use the data in your tests
5. Add to version control

## 🧪 Running Tests with Test Data

```bash
# Run tests with specific data
pnpm test:e2e

# Run visual tests
pnpm test:visual

# Run accessibility tests
pnpm test:accessibility

# Run performance tests
pnpm test:performance
```

## 📚 Related Files

- **`framework/data-factories/`** - Dynamic test data generation
- **`framework/api-testing/`** - API client utilities
- **`global-setup.ts`** - Test environment setup
- **`playwright.config.ts`** - Test configuration
