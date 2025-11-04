# BSS System - Architecture Specification & Extension Plan
## Tech Lead & Scrum Master Analysis

**Status:** SPEC Mode
**Version:** 1.0
**Date:** 2025-10-29
**Author:** Tech Lead / Scrum Master

---

## 1. EXECUTIVE SUMMARY

### Current State Analysis

System BSS (Business Support System) currently implements **Customer Management Core** with the following capabilities:

**Implemented:**
- ✅ Customer CRUD operations
- ✅ Customer status management (ACTIVE, INACTIVE, SUSPENDED, TERMINATED)
- ✅ Customer search and pagination
- ✅ OIDC Authentication (Keycloak 26)
- ✅ PostgreSQL persistence with JPA
- ✅ Hexagonal Architecture (Ports & Adapters)
- ✅ CQRS patterns
- ✅ CloudEvents v1.0 on Kafka
- ✅ OpenAPI documentation
- ✅ Comprehensive test suite

**Current Entities:**
- `customers` - 1 table (basic customer information)

**Current Frontend Pages:**
- Customer management UI (list, create, edit)
- Hello world page
- Addresses index
- Settings page
- Coverage nodes page

**Current Backend Endpoints:**
- `/api/customers` (full CRUD)
- `/api/hello`

### Gap Analysis for Full BSS System

Telecommunications BSS systems require comprehensive functionality covering:

1. **Product Catalog Management** - Services, tariffs, bundles
2. **Order Management** - Order capture, orchestration, fulfillment
3. **Service Activation** - Active services per customer
4. **Usage & Billing** - CDR processing, invoicing, payments
5. **Network Inventory** - Infrastructure elements
6. **Trouble Ticketing** - Incident management
7. **Contract Management** - Agreements and terms
8. **Asset Management** - Equipment, SIM cards, devices

---

## 2. PROPOSED SYSTEM EXTENSIONS

### 2.1 Database Schema - 10 New Tables

#### Table #1: `products`
**Purpose:** Product catalog - services, tariffs, bundles

```sql
CREATE TABLE products (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    product_code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(200) NOT NULL,
    description TEXT,
    product_type VARCHAR(50) NOT NULL, -- 'SERVICE', 'TARIFF', 'BUNDLE', 'ADDON'
    category VARCHAR(100), -- 'MOBILE', 'BROADBAND', 'TV', 'CLOUD'
    price DECIMAL(10,2) NOT NULL,
    currency VARCHAR(3) DEFAULT 'PLN',
    billing_period VARCHAR(20) NOT NULL, -- 'MONTHLY', 'YEARLY', 'ONE_TIME'
    status VARCHAR(20) DEFAULT 'ACTIVE', -- 'ACTIVE', 'INACTIVE', 'DEPRECATED'
    validity_start DATE,
    validity_end DATE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version INTEGER NOT NULL DEFAULT 1
);

CREATE INDEX idx_products_type ON products(product_type);
CREATE INDEX idx_products_category ON products(category);
CREATE INDEX idx_products_status ON products(status);
```

**Relationships:**
- M:N with `customers` via `subscriptions`
- 1:N with `product_features` (features/parameters)

---

#### Table #2: `product_features`
**Purpose:** Configurable features/parameters for products

```sql
CREATE TABLE product_features (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    product_id UUID NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    feature_key VARCHAR(100) NOT NULL,
    feature_value TEXT NOT NULL,
    data_type VARCHAR(20) NOT NULL, -- 'STRING', 'NUMBER', 'BOOLEAN', 'JSON'
    is_configurable BOOLEAN DEFAULT true,
    display_order INTEGER DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version INTEGER NOT NULL DEFAULT 1,
    UNIQUE(product_id, feature_key)
);

CREATE INDEX idx_product_features_product ON product_features(product_id);
```

**Examples:**
- Data limit: 50GB
- Speed: 100Mbps
- Voice minutes: 500
- SMS count: unlimited
- Roaming: enabled

---

#### Table #3: `orders`
**Purpose:** Customer order management (new orders, changes, cancellations)

```sql
CREATE TABLE orders (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_number VARCHAR(50) NOT NULL UNIQUE,
    customer_id UUID NOT NULL REFERENCES customers(id),
    order_type VARCHAR(30) NOT NULL, -- 'NEW', 'CHANGE', 'CANCEL', 'SUSPEND', 'RESUME'
    status VARCHAR(30) NOT NULL, -- 'DRAFT', 'PENDING', 'APPROVED', 'IN_PROGRESS', 'COMPLETED', 'REJECTED', 'CANCELLED'
    priority VARCHAR(10) DEFAULT 'NORMAL', -- 'LOW', 'NORMAL', 'HIGH', 'URGENT'
    total_amount DECIMAL(12,2),
    currency VARCHAR(3) DEFAULT 'PLN',
    requested_date DATE,
    promised_date DATE,
    completed_date DATE,
    order_channel VARCHAR(50), -- 'WEB', 'MOBILE_APP', 'PHONE', 'STORE', 'BACKOFFICE'
    sales_rep_id VARCHAR(100),
    notes TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    version INTEGER NOT NULL DEFAULT 1
);

CREATE INDEX idx_orders_customer ON orders(customer_id);
CREATE INDEX idx_orders_status ON orders(status);
CREATE INDEX idx_orders_type ON orders(order_type);
CREATE INDEX idx_orders_number ON orders(order_number);
```

**Relationships:**
- 1:N with `order_items`
- N:1 with `customers`

---

#### Table #4: `order_items`
**Purpose:** Individual items within an order

```sql
CREATE TABLE order_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id UUID NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    product_id UUID REFERENCES products(id),
    item_type VARCHAR(30) NOT NULL, -- 'PRODUCT', 'SERVICE', 'DEVICE', 'DISCOUNT', 'CHARGE'
    item_code VARCHAR(50),
    item_name VARCHAR(200) NOT NULL,
    quantity INTEGER NOT NULL DEFAULT 1,
    unit_price DECIMAL(10,2),
    total_price DECIMAL(12,2),
    discount_amount DECIMAL(10,2) DEFAULT 0,
    tax_rate DECIMAL(5,2) DEFAULT 23.00,
    tax_amount DECIMAL(10,2),
    net_amount DECIMAL(12,2),
    status VARCHAR(30) DEFAULT 'PENDING', -- 'PENDING', 'ACTIVE', 'FAILED', 'SKIPPED'
    activation_date DATE,
    expiry_date DATE,
    configuration JSONB, -- Product-specific settings
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version INTEGER NOT NULL DEFAULT 1
);

CREATE INDEX idx_order_items_order ON order_items(order_id);
CREATE INDEX idx_order_items_product ON order_items(product_id);
CREATE INDEX idx_order_items_status ON order_items(status);
```

**Examples:**
- Order: 1x Mobile tariff + 1x Mobile data pack + 1x SIM card
- Each as separate order_item

---

#### Table #5: `subscriptions`
**Purpose:** Active customer subscriptions/services

```sql
CREATE TABLE subscriptions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    subscription_number VARCHAR(50) NOT NULL UNIQUE,
    customer_id UUID NOT NULL REFERENCES customers(id),
    product_id UUID NOT NULL REFERENCES products(id),
    order_id UUID REFERENCES orders(id),
    status VARCHAR(30) NOT NULL, -- 'ACTIVE', 'SUSPENDED', 'CANCELLED', 'EXPIRED'
    start_date DATE NOT NULL,
    end_date DATE,
    billing_start DATE NOT NULL,
    next_billing_date DATE,
    billing_period VARCHAR(20) NOT NULL,
    price DECIMAL(10,2) NOT NULL,
    currency VARCHAR(3) DEFAULT 'PLN',
    discount_amount DECIMAL(10,2) DEFAULT 0,
    net_amount DECIMAL(10,2),
    configuration JSONB, -- Service-specific settings
    auto_renew BOOLEAN DEFAULT true,
    renewal_notice_sent BOOLEAN DEFAULT false,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    version INTEGER NOT NULL DEFAULT 1
);

CREATE INDEX idx_subscriptions_customer ON subscriptions(customer_id);
CREATE INDEX idx_subscriptions_product ON subscriptions(product_id);
CREATE INDEX idx_subscriptions_status ON subscriptions(status);
CREATE INDEX idx_subscriptions_billing ON subscriptions(next_billing_date);
```

**Relationships:**
- N:1 with `customers`
- N:1 with `products`
- N:1 with `orders`
- 1:N with `usage_records`

---

#### Table #6: `usage_records`
**Purpose:** Service usage tracking (voice, data, SMS, etc.)

```sql
CREATE TABLE usage_records (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    subscription_id UUID NOT NULL REFERENCES subscriptions(id),
    usage_type VARCHAR(30) NOT NULL, -- 'VOICE', 'SMS', 'DATA', 'VIDEO'
    usage_unit VARCHAR(10) NOT NULL, -- 'SECONDS', 'COUNT', 'MB', 'GB'
    usage_amount DECIMAL(15,3) NOT NULL,
    usage_date DATE NOT NULL,
    usage_time TIME NOT NULL,
    destination_type VARCHAR(50), -- 'NATIONAL', 'INTERNATIONAL', 'MOBILE', 'FIXED', 'SPECIAL'
    destination_number VARCHAR(50),
    destination_country VARCHAR(2),
    network_id VARCHAR(50),
    rate_period VARCHAR(20), -- 'PEAK', 'OFF_PEAK', 'WEEKEND'
    unit_rate DECIMAL(8,4),
    charge_amount DECIMAL(10,2),
    currency VARCHAR(3) DEFAULT 'PLN',
    tax_rate DECIMAL(5,2) DEFAULT 23.00,
    tax_amount DECIMAL(10,2),
    total_amount DECIMAL(10,2),
    rated BOOLEAN DEFAULT false,
    rating_date TIMESTAMP,
    source VARCHAR(50), -- 'CDR', 'MANUAL', 'BULK_UPLOAD'
    source_file VARCHAR(200),
    processed BOOLEAN DEFAULT false,
    invoice_id UUID, -- Will reference invoices table
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_usage_records_subscription ON usage_records(subscription_id);
CREATE INDEX idx_usage_records_date ON usage_records(usage_date);
CREATE INDEX idx_usage_records_type ON usage_records(usage_type);
CREATE INDEX idx_usage_records_rated ON usage_records(rated);
```

**CDR Example:**
- Call: 120 seconds, to mobile, peak time, rate 0.50/min = 1.00 PLN

---

#### Table #7: `invoices`
**Purpose:** Invoice generation and management

```sql
CREATE TABLE invoices (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    invoice_number VARCHAR(50) NOT NULL UNIQUE,
    customer_id UUID NOT NULL REFERENCES customers(id),
    invoice_type VARCHAR(20) NOT NULL, -- 'RECURRING', 'ONE_TIME', 'USAGE', 'ADJUSTMENT'
    status VARCHAR(30) NOT NULL, -- 'DRAFT', 'ISSUED', 'SENT', 'PAID', 'OVERDUE', 'CANCELLED'
    issue_date DATE NOT NULL,
    due_date DATE NOT NULL,
    paid_date DATE,
    billing_period_start DATE,
    billing_period_end DATE,
    subtotal DECIMAL(12,2) NOT NULL,
    discount_amount DECIMAL(10,2) DEFAULT 0,
    tax_amount DECIMAL(10,2) NOT NULL,
    total_amount DECIMAL(12,2) NOT NULL,
    currency VARCHAR(3) DEFAULT 'PLN',
    payment_terms INTEGER DEFAULT 14, -- days
    late_fee DECIMAL(10,2) DEFAULT 0,
    notes TEXT,
    pdf_url VARCHAR(500),
    sent_to_email VARCHAR(200),
    sent_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version INTEGER NOT NULL DEFAULT 1
);

CREATE INDEX idx_invoices_customer ON invoices(customer_id);
CREATE INDEX idx_invoices_status ON invoices(status);
CREATE INDEX idx_invoices_dates ON invoices(issue_date, due_date);
```

**Relationships:**
- N:1 with `customers`
- 1:N with `invoice_items`
- 1:N with `payments`

---

#### Table #8: `invoice_items`
**Purpose:** Line items within invoices

```sql
CREATE TABLE invoice_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    invoice_id UUID NOT NULL REFERENCES invoices(id) ON DELETE CASCADE,
    subscription_id UUID REFERENCES subscriptions(id),
    usage_record_id UUID REFERENCES usage_records(id),
    item_type VARCHAR(30) NOT NULL, -- 'SUBSCRIPTION', 'USAGE', 'DISCOUNT', 'ADJUSTMENT', 'TAX'
    description TEXT NOT NULL,
    quantity DECIMAL(10,3) NOT NULL DEFAULT 1,
    unit_price DECIMAL(10,2) NOT NULL,
    unit VARCHAR(20), -- 'MONTH', 'MB', 'MINUTE', 'SMS', 'PIECE'
    discount_rate DECIMAL(5,2) DEFAULT 0,
    discount_amount DECIMAL(10,2) DEFAULT 0,
    tax_rate DECIMAL(5,2) DEFAULT 23.00,
    tax_amount DECIMAL(10,2),
    net_amount DECIMAL(12,2),
    total_amount DECIMAL(12,2) NOT NULL,
    period_start DATE,
    period_end DATE,
    configuration JSONB,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_invoice_items_invoice ON invoice_items(invoice_id);
CREATE INDEX idx_invoice_items_subscription ON invoice_items(subscription_id);
```

**Example Invoice Items:**
- Mobile Plan - Monthly: 49.00 PLN
- Data Usage - 5.2GB: 15.60 PLN
- Discount - Loyalty 10%: -6.46 PLN
- Tax (23%): 13.43 PLN

---

#### Table #9: `payments`
**Purpose:** Payment tracking

```sql
CREATE TABLE payments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    payment_number VARCHAR(50) NOT NULL UNIQUE,
    customer_id UUID NOT NULL REFERENCES customers(id),
    invoice_id UUID REFERENCES invoices(id),
    amount DECIMAL(12,2) NOT NULL,
    currency VARCHAR(3) DEFAULT 'PLN',
    payment_method VARCHAR(30) NOT NULL, -- 'CARD', 'BANK_TRANSFER', 'CASH', 'DIRECT_DEBIT', 'MOBILE_PAY'
    payment_status VARCHAR(30) NOT NULL, -- 'PENDING', 'PROCESSING', 'COMPLETED', 'FAILED', 'REFUNDED'
    transaction_id VARCHAR(100), -- External payment gateway ID
    gateway VARCHAR(50), -- 'STRIPE', 'PAYPAL', 'BLIK', 'PRZELEWY24'
    payment_date DATE NOT NULL,
    received_date DATE,
    reference_number VARCHAR(100),
    notes TEXT,
    reversal_reason TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    version INTEGER NOT NULL DEFAULT 1
);

CREATE INDEX idx_payments_customer ON payments(customer_id);
CREATE INDEX idx_payments_invoice ON payments(invoice_id);
CREATE INDEX idx_payments_status ON payments(payment_status);
CREATE INDEX idx_payments_date ON payments(payment_date);
```

**Relationships:**
- N:1 with `customers`
- N:1 with `invoices`

---

#### Table #10: `network_elements`
**Purpose:** Network infrastructure inventory

```sql
CREATE TABLE network_elements (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    element_code VARCHAR(50) NOT NULL UNIQUE,
    element_type VARCHAR(50) NOT NULL, -- 'CELL_TOWER', 'BASE_STATION', 'SWITCH', 'ROUTER', 'SERVER', 'CABLE'
    name VARCHAR(200) NOT NULL,
    description TEXT,
    location_id UUID, -- Will reference addresses table
    status VARCHAR(30) DEFAULT 'ACTIVE', -- 'ACTIVE', 'INACTIVE', 'MAINTENANCE', 'FAULT', 'DECOMMISSIONED'
    vendor VARCHAR(100),
    model VARCHAR(100),
    serial_number VARCHAR(100),
    firmware_version VARCHAR(50),
    ip_address INET,
    mac_address VARCHAR(17),
    capacity_value DECIMAL(15,3),
    capacity_unit VARCHAR(20), -- 'USERS', 'MBPS', 'GB', 'PORTS'
    longitude DECIMAL(10, 6),
    latitude DECIMAL(10, 6),
    installation_date DATE,
    warranty_expiry DATE,
    last_maintenance_date DATE,
    next_maintenance_date DATE,
    attributes JSONB, -- Flexible key-value store for type-specific attributes
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    version INTEGER NOT NULL DEFAULT 1
);

CREATE INDEX idx_network_elements_type ON network_elements(element_type);
CREATE INDEX idx_network_elements_status ON network_elements(status);
CREATE INDEX idx_network_elements_location ON network_elements(location_id);
CREATE INDEX idx_network_elements_coordinates ON network_elements(longitude, latitude);
```

**Relationships:**
- N:1 with `addresses` (location)
- 1:N with `coverage_areas`

---

### 2.2 Frontend Pages - Nuxt.js

#### New Pages to Create:

1. **`/products`**
   - Product catalog listing
   - Filter by type (service, tariff, bundle)
   - Filter by category (mobile, broadband, TV)
   - View product details and features
   - Pricing calculator
   - Compare products feature

2. **`/products/[id]`**
   - Detailed product view
   - Feature list with explanations
   - Pricing breakdown
   - Add to order functionality
   - Related products

3. **`/orders`**
   - Order history for customer
   - Track order status
   - View order details
   - Order timeline/status progression
   - Download order confirmation

4. **`/orders/create`**
   - Multi-step order wizard
   - Step 1: Select products
   - Step 2: Configure services
   - Step 3: Add-ons
   - Step 4: Review and confirm
   - Step 5: Payment method
   - Order summary and submission

5. **`/subscriptions`**
   - Active subscriptions dashboard
   - Subscription cards with status
   - Quick actions (suspend, resume, modify)
   - Billing information
   - Usage statistics
   - Renewal dates

6. **`/subscriptions/[id]`**
   - Subscription details
   - Service configuration
   - Usage history (daily, weekly, monthly)
   - Billing history
   - Change plan options
   - Service features

7. **`/billing`**
   - Invoice list
   - Payment history
   - Outstanding invoices
   - Payment methods
   - Billing address
   - Download invoices (PDF)

8. **`/billing/invoices/[id]`**
   - Invoice details
   - Line items breakdown
   - Usage details
   - Download PDF
   - Pay now button
   - Payment status

9. **`/usage`**
   - Usage dashboard
   - Visual usage charts
   - Usage by service type
   - Daily/weekly/monthly views
   - Usage trends
   - Usage alerts/notifications

10. **`/network/coverage`**
    - Coverage map
    - Signal strength visualization
    - Network technology filter (4G, 5G)
    - Address/location checker
    - Coverage predictions

---

### 2.3 Backend API Endpoints - Spring Boot

#### Product Management API

```
GET    /api/products                    - List all products (paginated)
GET    /api/products/{id}               - Get product details
GET    /api/products/search             - Search products
GET    /api/products/{id}/features      - Get product features
GET    /api/products/types              - Get product types
GET    /api/products/categories         - Get product categories

POST   /api/products                    - Create product (admin)
PUT    /api/products/{id}               - Update product (admin)
DELETE /api/products/{id}               - Delete product (admin)
```

#### Order Management API

```
GET    /api/orders                      - List customer orders (paginated)
GET    /api/orders/{id}                 - Get order details
GET    /api/orders/{id}/items           - Get order items
POST   /api/orders                      - Create new order
PUT    /api/orders/{id}                 - Update order
PUT    /api/orders/{id}/approve         - Approve order
PUT    /api/orders/{id}/cancel          - Cancel order
PUT    /api/orders/{id}/status          - Update order status
GET    /api/orders/{id}/timeline        - Get order timeline
GET    /api/orders/search               - Search orders
```

#### Subscription Management API

```
GET    /api/subscriptions               - List customer subscriptions (paginated)
GET    /api/subscriptions/{id}          - Get subscription details
GET    /api/subscriptions/{id}/usage    - Get subscription usage
GET    /api/subscriptions/{id}/invoices - Get subscription invoices
PUT    /api/subscriptions/{id}          - Update subscription
PUT    /api/subscriptions/{id}/suspend  - Suspend subscription
PUT    /api/subscriptions/{id}/resume   - Resume subscription
PUT    /api/subscriptions/{id}/cancel   - Cancel subscription
POST   /api/subscriptions/{id}/upgrade  - Upgrade subscription
POST   /api/subscriptions/{id}/downgrade - Downgrade subscription
```

#### Billing & Invoices API

```
GET    /api/invoices                    - List customer invoices (paginated)
GET    /api/invoices/{id}               - Get invoice details
GET    /api/invoices/{id}/items         - Get invoice items
GET    /api/invoices/{id}/pdf           - Download invoice PDF
PUT    /api/invoices/{id}/status        - Update invoice status
POST   /api/invoices/{id}/send          - Send invoice via email
GET    /api/invoices/outstanding        - Get outstanding invoices
GET    /api/invoices/overdue            - Get overdue invoices

GET    /api/payments                    - List payments (paginated)
GET    /api/payments/{id}               - Get payment details
POST   /api/payments                    - Record payment
PUT    /api/payments/{id}/status        - Update payment status
GET    /api/payments/methods            - Get customer payment methods
POST   /api/payments/methods            - Add payment method
DELETE /api/payments/methods/{id}       - Remove payment method
```

#### Usage & Rating API

```
GET    /api/usage                       - List usage records (paginated)
GET    /api/usage/subscriptions/{id}    - Get subscription usage
GET    /api/usage/summary               - Get usage summary
GET    /api/usage/analytics             - Get usage analytics
GET    /api/usage/alerts                - Get usage alerts
POST   /api/usage/import                - Import usage records (CDR)
PUT    /api/usage/{id}/rate             - Rate usage record
GET    /api/usage/reports/daily         - Get daily usage report
GET    /api/usage/reports/monthly       - Get monthly usage report
```

#### Network Elements API

```
GET    /api/network/elements            - List network elements (paginated)
GET    /api/network/elements/{id}       - Get element details
GET    /api/network/elements/search     - Search elements
GET    /api/network/elements/{id}/capacity - Get element capacity
GET    /api/network/coverage/areas      - Get coverage areas
GET    /api/network/coverage/check      - Check coverage for location
GET    /api/network/map/coverage        - Get coverage map data
POST   /api/network/elements            - Create network element (admin)
PUT    /api/network/elements/{id}       - Update network element (admin)
PUT    /api/network/elements/{id}/status - Update element status
GET    /api/network/elements/statistics - Get network statistics
```

---

### 2.4 Kafka Topics - CloudEvents v1.0

#### Product Events

**Topic:** `bss.products.events`

```
CloudEvent: product.created.v1
{
  "specversion": "1.0",
  "type": "com.bss.products.product.created.v1",
  "source": "urn:bss:products",
  "id": "uuid-123",
  "time": "2025-10-29T10:00:00Z",
  "subject": "product/{productId}",
  "datacontenttype": "application/json",
  "data": {
    "productId": "uuid",
    "productCode": "MOBILE_PREMIUM",
    "name": "Mobile Premium Plan",
    "productType": "TARIFF",
    "category": "MOBILE",
    "price": 49.99,
    "currency": "PLN"
  }
}

CloudEvent: product.updated.v1
CloudEvent: product.deprecated.v1
```

#### Order Events

**Topic:** `bss.orders.events`

```
CloudEvent: order.created.v1
{
  "specversion": "1.0",
  "type": "com.bss.orders.order.created.v1",
  "source": "urn:bss:orders",
  "id": "uuid-123",
  "time": "2025-10-29T10:00:00Z",
  "subject": "order/{orderId}",
  "datacontenttype": "application/json",
  "data": {
    "orderId": "uuid",
    "customerId": "uuid",
    "orderNumber": "ORD-2025-001",
    "orderType": "NEW",
    "totalAmount": 149.97,
    "currency": "PLN"
  }
}

CloudEvent: order.approved.v1
CloudEvent: order.inProgress.v1
CloudEvent: order.completed.v1
CloudEvent: order.cancelled.v1
CloudEvent: order.failed.v1
```

#### Subscription Events

**Topic:** `bss.subscriptions.events`

```
CloudEvent: subscription.created.v1
{
  "specversion": "1.0",
  "type": "com.bss.subscriptions.subscription.created.v1",
  "source": "urn:bss:subscriptions",
  "id": "uuid-123",
  "time": "2025-10-29T10:00:00Z",
  "subject": "subscription/{subscriptionId}",
  "datacontenttype": "application/json",
  "data": {
    "subscriptionId": "uuid",
    "customerId": "uuid",
    "productId": "uuid",
    "subscriptionNumber": "SUB-2025-001",
    "status": "ACTIVE",
    "startDate": "2025-10-29",
    "billingPeriod": "MONTHLY",
    "price": 49.99
  }
}

CloudEvent: subscription.updated.v1
CloudEvent: subscription.suspended.v1
CloudEvent: subscription.resumed.v1
CloudEvent: subscription.cancelled.v1
CloudEvent: subscription.renewed.v1
```

#### Billing Events

**Topic:** `bss.billing.events`

```
CloudEvent: invoice.created.v1
{
  "specversion": "1.0",
  "type": "com.bss.billing.invoice.created.v1",
  "source": "urn:bss:billing",
  "id": "uuid-123",
  "time": "2025-10-29T10:00:00Z",
  "subject": "invoice/{invoiceId}",
  "datacontenttype": "application/json",
  "data": {
    "invoiceId": "uuid",
    "customerId": "uuid",
    "invoiceNumber": "INV-2025-001",
    "invoiceType": "RECURRING",
    "totalAmount": 149.97,
    "currency": "PLN",
    "issueDate": "2025-10-29",
    "dueDate": "2025-11-12"
  }
}

CloudEvent: invoice.sent.v1
CloudEvent: invoice.paid.v1
CloudEvent: invoice.overdue.v1
CloudEvent: payment.received.v1
CloudEvent: payment.failed.v1
```

#### Usage Events

**Topic:** `bss.usage.events`

```
CloudEvent: usage.recorded.v1
{
  "specversion": "1.0",
  "type": "com.bss.usage.usage.recorded.v1",
  "source": "urn:bss:usage",
  "id": "uuid-123",
  "time": "2025-10-29T10:00:00Z",
  "subject": "usage/{usageId}",
  "datacontenttype": "application/json",
  "data": {
    "usageId": "uuid",
    "subscriptionId": "uuid",
    "usageType": "DATA",
    "usageAmount": 1024.5,
    "usageUnit": "MB",
    "usageDate": "2025-10-29",
    "usageTime": "10:15:30"
  }
}

CloudEvent: usage.rated.v1
CloudEvent: usage.aggregated.v1
CloudEvent: usage.alert.v1 (when threshold exceeded)
```

---

## 3. IMPLEMENTATION PRIORITY

### Phase 1: Core Order-to-Cash (Sprint 1-3)
**Priority: HIGH**

1. Products table + API + pages
2. Orders table + API + pages
3. Subscriptions table + API + pages
4. Basic invoice generation
5. Kafka events for orders

**Business Value:** Complete order-to-cash process
**Dependencies:** Products → Orders → Subscriptions → Invoices

### Phase 2: Usage & Billing (Sprint 4-5)
**Priority: HIGH**

1. Usage records table + rating engine
2. Invoice items & payments tables
3. Billing API + pages
4. Payment processing
5. Kafka events for billing

**Business Value:** Generate and collect revenue

### Phase 3: Network & Operations (Sprint 6-7)
**Priority: MEDIUM**

1. Network elements table + coverage map
2. Network elements API
3. Coverage visualization
4. Network monitoring integration

**Business Value:** Network operations and customer experience

---

## 4. ARCHITECTURAL CONSIDERATIONS

### 4.1 Domain-Driven Design (DDD)

Each new module follows DDD patterns:

**Bounded Contexts:**
- Product Catalog Context
- Order Management Context
- Subscription Context
- Billing Context
- Usage Context
- Network Context

**Aggregates:**
- Product (root: Product, children: ProductFeatures)
- Order (root: Order, children: OrderItems)
- Subscription (root: Subscription)
- Invoice (root: Invoice, children: InvoiceItems)
- NetworkElement (root)

### 4.2 CQRS Implementation

**Command Side:**
- CreateOrderUseCase
- UpdateSubscriptionUseCase
- GenerateInvoiceUseCase
- RecordPaymentUseCase
- ProcessUsageUseCase

**Query Side:**
- OrderQueryService
- SubscriptionQueryService
- InvoiceQueryService
- UsageQueryService
- NetworkQueryService

### 4.3 Event Sourcing Considerations

All state changes published as CloudEvents:
- `*created.v1`
- `*updated.v1`
- `*statusChanged.v1`
- `*cancelled.v1`

### 4.4 Performance & Scalability

**Database Optimization:**
- Indexes on all foreign keys
- Composite indexes for common queries
- Partitioning for usage_records by month/quarter
- Archival strategy for historical data

**Caching Strategy:**
- Redis for product catalog
- Redis for subscription status
- Redis for network elements
- Cache invalidation via events

**API Optimization:**
- Pagination on all list endpoints
- Field selection via `?fields=`
- ETag support for caching
- Rate limiting per customer

### 4.5 Data Consistency

**Transactional Boundaries:**
- Order creation (order + items) in single transaction
- Invoice generation (invoice + items + payments) in single transaction
- Usage rating in batch transactions

**Outbox Pattern:**
- Database changes → Outbox table → Kafka consumer → Publish events
- Ensures eventual consistency

---

## 5. SECURITY CONSIDERATIONS

### 5.1 Authentication & Authorization

**OIDC Integration:**
- All endpoints require JWT token
- Role-based access control (RBAC)
- Resource-based permissions

**Roles:**
- `CUSTOMER` - View own data
- `AGENT` - Manage customer orders/subscriptions
- `ADMIN` - Full access, manage products/network
- `BILLING` - Billing operations
- `SUPPORT` - View data, create tickets

### 5.2 Data Protection

**PII Handling:**
- Personal data encrypted at rest
- PCI compliance for payment data
- GDPR compliance (data retention, deletion)
- Audit logging for sensitive operations

**API Security:**
- Input validation on all endpoints
- SQL injection prevention via JPA
- XSS protection in frontend
- CSRF protection for state-changing operations

---

## 6. TESTING STRATEGY

### 6.1 Test Pyramid

**Unit Tests (70%):**
- Domain entities validation
- Use cases business logic
- Service layer logic
- Utilities and helpers

**Integration Tests (20%):**
- Repository layer with Testcontainers
- API endpoints with MockMvc
- Kafka event publishing/consumption
- Payment gateway integration (mock)

**E2E Tests (10%):**
- Complete order flow
- Subscription lifecycle
- Billing cycle
- Usage recording and rating

### 6.2 Test Data Management

**TestContainers:**
- PostgreSQL for each test class
- Kafka for event testing
- Redis for caching tests

**Fixtures:**
- ProductDataFactory
- OrderDataFactory
- CustomerDataFactory
- UsageDataFactory

---

## 7. DEPLOYMENT & OPERATIONS

### 7.1 Infrastructure Requirements

**Database:**
- PostgreSQL 18 with partitioning for usage_records
- Connection pooling (HikariCP)
- Read replicas for reporting queries

**Kafka:**
- 3 broker cluster for HA
- Topic replication factor: 3
- Retention: 7 days for events, 90 days for usage

**Redis:**
- Cluster mode for high availability
- Persistence enabled (AOF)
- Memory optimization policies

### 7.2 Monitoring & Observability

**Metrics:**
- Order processing time
- Invoice generation time
- Usage processing throughput
- Payment success rate
- API response times

**Logging:**
- Structured logging (JSON)
- Correlation IDs for request tracing
- Audit logs for sensitive operations
- Error tracking (Sentry)

**Alerts:**
- Failed order processing
- Invoice generation failures
- Payment gateway errors
- High usage/load alerts
- Database performance issues

---

## 8. RISK ANALYSIS

### 8.1 Technical Risks

| Risk | Impact | Probability | Mitigation |
|------|--------|-------------|------------|
| Data migration complexity | HIGH | MEDIUM | Incremental migration, rollback plans |
| Performance degradation | HIGH | MEDIUM | Load testing, database optimization |
| Event processing latency | MEDIUM | MEDIUM | Async processing, monitoring |
| Payment gateway integration | HIGH | LOW | Multiple gateways, fallback |
| Network element data quality | MEDIUM | LOW | Validation, data enrichment |

### 8.2 Business Risks

| Risk | Impact | Probability | Mitigation |
|------|--------|-------------|------------|
| Scope creep | HIGH | HIGH | Clear requirements, phase gates |
| Resource constraints | MEDIUM | MEDIUM | Prioritization, agile planning |
| Integration delays | MEDIUM | MEDIUM | Early prototyping, mock services |
| Compliance issues | HIGH | LOW | Legal review, compliance testing |

---

## 9. SUCCESS METRICS

### 9.1 Technical KPIs

- API response time < 200ms (95th percentile)
- Database query time < 50ms (average)
- Order processing time < 5 minutes
- Invoice generation time < 2 minutes
- System availability > 99.9%

### 9.2 Business KPIs

- Order completion rate > 95%
- Invoice payment success rate > 90%
- Customer self-service rate > 80%
- Time to activate new service < 24 hours
- Invoice accuracy > 99.5%

---

## 10. NEXT STEPS

### Immediate Actions (Sprint Planning)

1. **Product Backlog Refinement:**
   - Break down epics into user stories
   - Define acceptance criteria
   - Estimate story points
   - Identify dependencies

2. **Architecture Review:**
   - Present spec to team
   - Address questions and concerns
   - Refine technical approach
   - Update ADRs if needed

3. **Team Assignments:**
   - Backend dev team: Products + Orders
   - Frontend dev team: Product catalog + Order flow
   - DevOps: Infrastructure setup (Kafka topics, DB)

4. **Technical Spike:**
   - Payment gateway integration research
   - CDR processing validation
   - Map visualization technology selection
   - Performance testing approach

### Acceptance Criteria

**Phase 1 DoD:**
- [ ] All database tables created with migrations
- [ ] All CRUD APIs implemented with tests
- [ ] All frontend pages created with E2E tests
- [ ] CloudEvents published for all state changes
- [ ] OpenAPI documentation updated
- [ ] Performance benchmarks met
- [ ] Security review completed

---

## CONCLUSION

This specification provides a comprehensive roadmap for evolving the BSS system from a customer management module to a full-featured telecommunications business support system. The proposed architecture follows DDD, CQRS, and event-driven principles while maintaining the hexagonal architecture already established.

The phased approach ensures business value delivery at each stage, with clear priorities based on order-to-cash business processes. The technical foundation supports scalability, maintainability, and observability required for a production-grade BSS platform.

**Recommended Next Action:** Schedule architecture review session with full team and begin Sprint 1 planning for Phase 1 implementation.

---

**Document Control:**
- Classification: Internal
- Review Cycle: Monthly
- Owner: Tech Lead / Scrum Master
- Approval Required: CTO, Product Owner, Architecture Board
