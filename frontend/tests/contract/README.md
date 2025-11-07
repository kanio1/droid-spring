# Contract Testing with Pact

This directory contains the contract testing implementation using the [Pact](https://pact.io/) framework to ensure API contracts between the frontend (consumer) and backend (provider) are properly maintained.

## Overview

Contract testing ensures that:
- **Frontend consumers** define expected API contracts
- **Backend providers** implement APIs that match the contracts
- **Breaking changes** are detected before deployment
- **Teams can work independently** with confidence

## Architecture

```
Frontend (Consumer)           Backend (Provider)
┌──────────────┐             ┌──────────────┐
│ 1. Write     │             │ 4. Implement │
│  consumer    │             │  API         │
│  contracts   │             │              │
└──────────────┘             └──────────────┘
        │                            │
        │ 2. Publish                 │ 5. Verify
        │  pacts                     │  contracts
        ↓                            ↓
┌─────────────────────────────────────────────┐
│           Pact Broker                        │
│  - Stores contract files                     │
│  - Coordinates verification                  │
│  - Tracks version compatibility              │
└─────────────────────────────────────────────┘
        ↓                            ↑
        │ 3. Publish                 │ 6. Verify
        │  pacts                     │  compatibility
        ↓                            │
┌──────────────┐             ┌──────────────┐
│  CI/CD       │             │  CI/CD       │
│  Pipeline    │             │  Pipeline    │
└──────────────┘             └──────────────┘
```

## Directory Structure

```
tests/contract/
├── README.md                          # This file
├── pact-config.ts                     # Pact configuration
├── consumers/                          # Consumer contract tests
│   ├── customer-consumer.pact.test.ts
│   ├── order-consumer.pact.test.ts
│   ├── invoice-consumer.pact.test.ts
│   ├── payment-consumer.pact.test.ts
│   └── subscription-consumer.pact.test.ts
└── providers/                          # Provider verification tests
    ├── customer-provider.pact.test.ts
    ├── order-provider.pact.test.ts
    ├── invoice-provider.pact.test.ts
    ├── payment-provider.pact.test.ts
    └── subscription-provider.pact.test.ts
```

## API Contracts Covered

### 1. Customer API (`/api/customers`)

- **GET /api/customers** - List customers with pagination and filtering
- **GET /api/customers/:id** - Get customer by ID
- **POST /api/customers** - Create new customer
- **PUT /api/customers/:id** - Update customer
- **DELETE /api/customers/:id** - Delete customer

Features tested:
- ✅ Pagination (page, limit)
- ✅ Filtering (status, search)
- ✅ Error handling (404, validation, duplicate email)
- ✅ Response structure (data, pagination metadata)

### 2. Order API (`/api/orders`)

- **GET /api/orders** - List orders with filtering
- **GET /api/orders/:id** - Get order by ID
- **POST /api/orders** - Create new order
- **PUT /api/orders/:id/status** - Update order status
- **DELETE /api/orders/:id** - Delete order

Features tested:
- ✅ Order listing with customer data
- ✅ Order status transitions
- ✅ Line items structure
- ✅ Validation (empty items array)
- ✅ CRUD operations

### 3. Invoice API (`/api/invoices`)

- **GET /api/invoices** - List invoices
- **GET /api/invoices/:id** - Get invoice by ID
- **POST /api/invoices** - Create new invoice
- **POST /api/invoices/:id/send** - Send invoice via email
- **PUT /api/invoices/:id/status** - Update invoice status

Features tested:
- ✅ Invoice creation with line items
- ✅ Tax and total calculations
- ✅ Invoice status tracking
- ✅ Email sending functionality
- ✅ Payment association

### 4. Payment API (`/api/payments`)

- **GET /api/payments** - List payments
- **GET /api/payments/:id** - Get payment by ID
- **POST /api/payments** - Process payment
- **POST /api/payments/:id/refund** - Refund payment
- **GET /api/payments/:id/history** - Payment history

Features tested:
- ✅ Payment processing
- ✅ Payment method data (card, token)
- ✅ Refund processing
- ✅ Payment history/audit trail
- ✅ Transaction IDs and status tracking

### 5. Subscription API (`/api/subscriptions`)

- **GET /api/subscriptions** - List subscriptions
- **GET /api/subscriptions/:id** - Get subscription by ID
- **POST /api/subscriptions** - Create subscription
- **PUT /api/subscriptions/:id/activate** - Activate trial
- **PUT /api/subscriptions/:id/cancel** - Cancel subscription
- **PUT /api/subscriptions/:id/change-plan** - Change plan
- **GET /api/subscriptions/:id/usage** - Usage statistics

Features tested:
- ✅ Subscription lifecycle (trial → active → cancelled)
- ✅ Plan changes and upgrades
- ✅ Trial management
- ✅ Usage tracking
- ✅ Billing cycle management

## Running Contract Tests

### Prerequisites

```bash
# Install dependencies
pnpm install

# Start Pact broker (optional, for local development)
docker pull pactfoundation/pact-broker:latest
docker run -d -p 9292:9292 pactfoundation/pact-broker
```

### Consumer Tests (Generate Contracts)

Run consumer tests to generate contract files:

```bash
# Run all consumer tests
pnpm test:contract

# Run specific consumer test
pnpm test:unit -- customer-consumer.pact.test.ts

# Run with watch mode
pnpm test:contract:watch
```

Generated pact files are saved to:
- `pacts/frontend-customer-bss-backend-api.json`
- `pacts/frontend-order-bss-backend-api.json`
- `pacts/frontend-invoice-bss-backend-api.json`
- `pacts/frontend-payment-bss-backend-api.json`
- `pacts/frontend-subscription-bss-backend-api.json`

### Provider Verification

Verify provider implementation against contracts:

```bash
# Start backend
./mvnw spring-boot:run

# Run all provider tests
pnpm test:unit -- providers/

# Run specific provider verification
pnpm test:unit -- customer-provider.pact.test.ts
```

### Publishing Contracts

Publish contracts to Pact broker:

```bash
# Set environment variables
export PACT_BROKER_BASE_URL=https://your-broker.com
export PACT_BROKER_TOKEN=your-token

# Publish contracts
pnpm test:contract:publish
```

### Verify Deployment Compatibility

Check if you can deploy safely:

```bash
# Check if this version is safe to deploy
pnpm test:contract:can-i-deploy
```

### Generate Contract Documentation

Generate human-readable contract documentation:

```bash
# Generate docs
pnpm test:contract:docs

# View docs (will be in ./docs/contracts/)
```

## Environment Variables

Create a `.env` file with the following variables:

```bash
# Pact Broker
PACT_BROKER_BASE_URL=https://broker.yourcompany.com
PACT_BROKER_TOKEN=your-broker-token

# Provider (Backend API)
API_BASE_URL=http://localhost:8080

# CI/CD
GITHUB_SHA=commit-hash
GITHUB_REF_NAME=branch-name
```

## CI/CD Integration

### GitHub Actions Example

```yaml
name: Contract Testing

on:
  push:
    branches: [ main, develop ]
  pull_request:
    branches: [ main ]

jobs:
  contract-test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3

      - name: Setup Node.js
        uses: actions/setup-node@v3
        with:
          node-version: 20
          cache: 'pnpm'

      - name: Install dependencies
        run: pnpm install --frozen-lockfile

      - name: Run consumer tests
        run: pnpm test:contract

      - name: Publish contracts
        env:
          PACT_BROKER_BASE_URL: ${{ secrets.PACT_BROKER_BASE_URL }}
          PACT_BROKER_TOKEN: ${{ secrets.PACT_BROKER_TOKEN }}
        run: pnpm test:contract:publish

      - name: Verify deployment
        env:
          PACT_BROKER_BASE_URL: ${{ secrets.PACT_BROKER_BASE_URL }}
          PACT_BROKER_TOKEN: ${{ secrets.PACT_BROKER_TOKEN }}
        run: pnpm test:contract:can-i-deploy
```

## Best Practices

### 1. Contract Definition

```typescript
// ✅ Good: Specific, matchers
await pact
  .given('a customer exists')
  .uponReceiving('a request for customer details')
  .withRequest({
    method: 'GET',
    path: '/api/customers/123',
    headers: {
      'Authorization': Matchers.like('Bearer token')
    }
  })
  .willRespondWith({
    status: 200,
    body: {
      id: Matchers.uuid('123'),
      firstName: Matchers.like('John'),
      email: Matchers.email()
    }
  })
```

### 2. State Management

Use meaningful state descriptions:

```typescript
.stateHandlers({
  'a customer with email already exists': async () => {
    // Setup: Create customer with specific email
  },
  'no customers exist': async () => {
    // Setup: Clean database
  }
})
```

### 3. Matcher Usage

Use appropriate matchers:

- `Matchers.like()` - For exact match with flexibility
- `Matchers.uuid()` - For UUID validation
- `Matchers.email()` - For email format
- `Matchers.iso8601DateTime()` - For date/time
- `Matchers.eachLike()` - For arrays
- `Matchers.regex()` - For pattern matching

### 4. Test Isolation

Each test should be independent:

```typescript
beforeAll(async () => {
  await pact.setup()
})

afterAll(async () => {
  await pact.writePact()  // Write once after all tests
  await pact.finalize()
})
```

## Troubleshooting

### Common Issues

1. **Pact file not found**
   - Ensure `afterAll` writes the pact file
   - Check `dir` path in config

2. **Provider verification fails**
   - Check API_BASE_URL is correct
   - Ensure backend is running
   - Verify authentication headers

3. **Publishing fails**
   - Verify PACT_BROKER_TOKEN is set
   - Check broker URL is accessible
   - Ensure network connectivity

### Debug Mode

Enable verbose logging:

```typescript
const pact = createConsumerPact(consumerName, providerName, {
  logLevel: 'debug'
})
```

Or set environment variable:
```bash
LOG_LEVEL=debug pnpm test:contract
```

## Resources

- [Pact Documentation](https://docs.pact.io/)
- [Pact Broker](https://github.com/pact-foundation/pact_broker)
- [PactJS Documentation](https://github.com/pact-foundation/pact-js)
- [Best Practices](https://docs.pact.io/consumer/contract_tests)

## Support

For issues and questions:
- Create an issue in the repository
- Check existing issues in the backlog
- Review the logs for specific error messages
