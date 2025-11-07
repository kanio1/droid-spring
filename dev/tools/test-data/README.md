# BSS Test Data Generator

This directory contains tools and scripts for generating realistic test data for the BSS (Business Support System).

## Overview

The test data generator creates realistic, consistent data for:
- **Customers** (Individual and Business)
- **Products** (Telecom and IT services)
- **Orders** (Service requests and modifications)
- **Payments** (Financial transactions)
- **Invoices** (Billing documents)
- **Subscriptions** (Recurring services)
- **Addresses** (Customer locations)
- **Order Items** (Products within orders)

## Quick Start

### 1. Generate SQL Test Data

```bash
# Generate 1000 customers
python3 generators/generate_test_data.py --generate customers --count 1000 --output sql

# Generate 5000 orders (includes customers)
python3 generators/generate_test_data.py --generate orders --count 5000 --output sql

# Generate all entities
python3 generators/generate_test_data.py --generate all --count 10000 --output sql
```

### 2. Load Data into Database

```bash
# Load 1000 customers into database
./generators/load-test-data.sh customers --count 1000

# Load 10000 orders into database
./generators/load-test-data.sh orders --count 10000

# Load all test data
./generators/load-test-data.sh all --count 5000
```

### 3. View Database Statistics

```bash
./generators/load-test-data.sh stats
```

## Python Test Data Generator

### Usage

```bash
python3 generators/generate_test_data.py [OPTIONS]

Options:
  --count N            Number of records to generate (default: 1000)
  --output FORMAT      Output format: sql, json, db (default: sql)
  --generate TYPE      Entity type: customers, products, orders, payments, all
  --output-file PATH   Output file path
  --db-url URL         Database connection URL
  --seed N             Random seed for reproducibility
  --help               Show this help
```

### Examples

```bash
# Generate customers as SQL
python3 generators/generate_test_data.py \
    --generate customers \
    --count 5000 \
    --output sql \
    --output-file customers.sql

# Generate products as JSON
python3 generators/generate_test_data.py \
    --generate products \
    --count 100 \
    --output json \
    --output-file products.json

# Generate directly to database
python3 generators/generate_test_data.py \
    --generate all \
    --count 1000 \
    --output db \
    --db-url postgresql://user:pass@localhost:5432/bss
```

### Entity Types

#### Customers
Generates individual and business customers with:
- Personal/business information
- Contact details (email, phone)
- Identification numbers (PESEL, NIP, REGON)
- Status and timestamps

```sql
Example output:
INSERT INTO customers (
    id, type, first_name, last_name, company_name,
    email, phone, pesel, nip, status, created_at
) VALUES (
    'uuid-123...',
    'INDIVIDUAL',
    'John',
    'Doe',
    NULL,
    'john.doe@example.com',
    '+48123456789',
    '12345678901',
    NULL,
    'ACTIVE',
    '2024-01-15T10:30:00'
);
```

#### Products
Generates telecom and IT products with:
- Product codes and names
- Categories (TELECOM, INTERNET, VOICE, etc.)
- Pricing in PLN
- Status and descriptions

#### Orders
Generates customer orders with:
- Order types (NEW_SERVICE, MODIFY, CANCEL, etc.)
- Priorities (LOW, MEDIUM, HIGH, URGENT)
- Status flow (PENDING → IN_PROGRESS → COMPLETED)
- Amounts and dates
- Channel information (WEB, MOBILE, PHONE, etc.)

#### Payments
Generates payment transactions with:
- Payment methods (CREDIT_CARD, BANK_TRANSFER, PAYPAL, etc.)
- Status flow (PENDING → COMPLETED/FAILED)
- Transaction IDs
- Amounts and currencies

#### Invoices
Generates invoices with:
- Invoice numbers
- Amounts and currency
- Status (DRAFT, SENT, PAID, OVERDUE)
- Due dates and payment dates

#### Subscriptions
Generates recurring subscriptions with:
- Service types (VOICE, DATA, SMS, etc.)
- Status (ACTIVE, INACTIVE, SUSPENDED, etc.)
- Start/end dates
- Monthly fees

## Shell Script Loader

The `load-test-data.sh` script provides a convenient way to load test data into the PostgreSQL database.

### Usage

```bash
./generators/load-test-data.sh [COMMAND] [OPTIONS]
```

### Commands

| Command | Description |
|---------|-------------|
| `all` | Load all test data (default) |
| `customers` | Load only customers |
| `products` | Load only products |
| `orders` | Load only orders (includes customers) |
| `payments` | Load only payments (includes customers) |
| `clean` | Remove all test data from database |
| `stats` | Show database statistics |

### Options

| Option | Description | Default |
|--------|-------------|---------|
| `--count N` | Number of records | 1000 |
| `--env ENV` | Environment: dev, stage, prod | dev |
| `--db-host HOST` | Database host | localhost |
| `--db-port PORT` | Database port | 5432 |
| `--db-name NAME` | Database name | bss |
| `--db-user USER` | Database user | bss_app |

### Examples

```bash
# Load 1000 customers
./generators/load-test-data.sh customers --count 1000

# Load 10000 orders
./generators/load-test-data.sh orders --count 10000

# Load all data (5000 records each)
./generators/load-test-data.sh all --count 5000

# Load from custom database
./generators/load-test-data.sh all --count 1000 \
    --db-host postgres.internal \
    --db-user bss_admin

# View current statistics
./generators/load-test-data.sh stats

# Clean all test data
./generators/load-test-data.sh clean
```

### Database Connection

The script reads database credentials from:
1. Command-line arguments
2. Environment variables (DB_PASSWORD, POSTGRES_PASSWORD)
3. `.env` file in project root

Example `.env` file:
```bash
POSTGRES_DB=bss
POSTGRES_USER=bss_app
POSTGRES_PASSWORD=your_password
POSTGRES_HOST=localhost
POSTGRES_PORT=5432
```

## Dependencies

### Python Packages
Install required packages:
```bash
pip3 install faker psycopg2-binary
```

### System Packages
- PostgreSQL client (`psql`)
- Python 3.6+

### Installation

```bash
# Install Python dependencies
pip3 install faker psycopg2-binary

# Or add to requirements.txt
echo "faker" >> requirements.txt
echo "psycopg2-binary" >> requirements.txt
pip3 install -r requirements.txt
```

## Test Data Specifications

### Data Characteristics

**Realism:**
- Realistic names, addresses, phone numbers
- Valid email formats
- Consistent timestamps
- Proper relationships between entities

**Distribution:**
- Mixed customer types (80% individual, 20% business)
- Various order statuses (PENDING 20%, IN_PROGRESS 10%, COMPLETED 60%, CANCELLED 10%)
- Realistic amount distributions
- Time-based patterns (more recent data)

**Referential Integrity:**
- Customer IDs referenced in orders, payments, invoices
- Order IDs in invoices
- Product IDs in order items
- Proper foreign key relationships

### Volume Recommendations

| Environment | Customers | Orders | Payments | Invoices | Subscriptions |
|-------------|-----------|--------|----------|----------|---------------|
| **Dev** | 1K | 5K | 2K | 3K | 1K |
| **Stage** | 10K | 50K | 20K | 30K | 10K |
| **Prod** | 100K | 500K | 200K | 300K | 100K |
| **Perf Test** | 1M | 5M | 2M | 3M | 1M |

## Performance Tips

### Generating Large Datasets

For large datasets (100K+ records):

1. **Use JSON output first** to verify structure
2. **Use database output** to load directly (faster)
3. **Batch loading** - Generator uses 1000-record pages
4. **Monitor progress** - Script shows progress every 1000 records

```bash
# Generate to JSON first
python3 generators/generate_test_data.py \
    --generate all \
    --count 100000 \
    --output json \
    --output-file test_data.json

# Then load via psql
psql -h localhost -U bss_app -d bss -f test_data.json
```

### Database Optimization

Before loading large datasets:

```sql
-- Disable triggers for faster loading
ALTER TABLE customers DISABLE TRIGGER ALL;

-- Load data...

-- Re-enable triggers
ALTER TABLE customers ENABLE TRIGGER ALL;

-- Update statistics
ANALYZE customers;
```

## Troubleshooting

### Connection Errors

```
ERROR: password authentication failed
```
**Solution:** Check DB_PASSWORD in `.env` file or set environment variable

```
psql: could not connect to server
```
**Solution:** Check if PostgreSQL is running and DB_HOST/DB_PORT are correct

### Python Module Errors

```
ModuleNotFoundError: No module named 'faker'
```
**Solution:** Install dependencies
```bash
pip3 install faker psycopg2-binary
```

### Out of Memory

For very large datasets (1M+ records):
- Generate in chunks (`--count 100000`)
- Use streaming output
- Monitor RAM usage

## Advanced Usage

### Custom Data Generation

Extend the generator for custom needs:

```python
# Add custom fields
def generate_customer(self):
    customer = self.generate_customer()
    customer['segment'] = random.choice(['GOLD', 'SILVER', 'BRONZE'])
    customer['vip_status'] = random.random() < 0.1
    return customer

# Add new entity type
def generate_service_activation(self, customer_id):
    return {
        'id': str(uuid.uuid4()),
        'customer_id': customer_id,
        'service_code': fake.bothify(text='SRV-####'),
        'status': random.choice(['PENDING', 'ACTIVE', 'FAILED']),
        # ... more fields
    }
```

### Integration with Docker

```yaml
# docker-compose.yml
test-data-loader:
  build: .
  command: python3 generators/generate_test_data.py --generate all --count 10000 --output db
  environment:
    - DB_HOST=postgres
    - DB_USER=bss_app
    - DB_PASSWORD=${POSTGRES_PASSWORD}
  depends_on:
    - postgres
```

## Data Privacy

**Important:** This tool generates **fake** test data only.

- Names are randomly generated
- Emails use example.com domain
- No real personal information
- PESEL numbers are synthetic
- Safe for development and testing

## License

Proprietary - See LICENSE file in project root

## Support

- **Email:** bss-dev@company.com
- **Slack:** #bss-support
- **Jira:** BSS project
