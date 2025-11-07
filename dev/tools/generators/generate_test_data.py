#!/usr/bin/env python3
"""
BSS Test Data Generator

This script generates realistic test data for the BSS (Business Support System).
It creates customers, orders, payments, invoices, subscriptions, and other entities
for development, testing, and performance testing.

Usage:
    python generate_test_data.py --count 1000 --output sql
    python generate_test_data.py --count 10000 --output json
    python generate_test_data.py --generate customers --count 5000 --db-url postgresql://...
"""

import argparse
import random
import uuid
from datetime import datetime, timedelta
from decimal import Decimal
from faker import Faker
import json
import psycopg2
from psycopg2.extras import execute_batch

# Initialize Faker
fake = Faker('en_US')
Faker.seed(42)  # For reproducible data
random.seed(42)

# Data templates
CUSTOMER_TYPES = ['INDIVIDUAL', 'BUSINESS', 'GOVERNMENT']
CUSTOMER_STATUSES = ['ACTIVE', 'INACTIVE', 'PENDING', 'SUSPENDED']
ORDER_TYPES = ['NEW_SERVICE', 'MODIFY_SERVICE', 'CANCEL_SERVICE', 'SUSPEND_SERVICE', 'RESTORE_SERVICE']
ORDER_PRIORITIES = ['LOW', 'MEDIUM', 'HIGH', 'URGENT']
ORDER_STATUSES = ['PENDING', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED', 'FAILED']
PAYMENT_METHODS = ['CREDIT_CARD', 'BANK_TRANSFER', 'PAYPAL', 'CASH', 'CHECK']
PAYMENT_STATUSES = ['PENDING', 'COMPLETED', 'FAILED', 'REFUNDED', 'PARTIALLY_REFUNDED']
INVOICE_STATUSES = ['DRAFT', 'SENT', 'PAID', 'OVERDUE', 'CANCELLED']
SUBSCRIPTION_STATUSES = ['ACTIVE', 'INACTIVE', 'PENDING', 'SUSPENDED', 'CANCELLED']
PRODUCT_CATEGORIES = ['TELECOM', 'INTERNET', 'VOICE', 'SMS', 'DATA', 'CLOUD', 'SECURITY']
SERVICE_TYPES = ['VOICE', 'DATA', 'SMS', 'MMS', 'INTERNET', 'CLOUD_STORAGE', 'VPN']

class BSSDataGenerator:
    """Generate realistic test data for BSS system"""

    def __init__(self, db_url=None):
        self.db_url = db_url
        self.conn = None
        if db_url:
            self.connect_db()

    def connect_db(self):
        """Connect to database"""
        try:
            self.conn = psycopg2.connect(self.db_url)
            print(f"✓ Connected to database")
        except Exception as e:
            print(f"✗ Failed to connect to database: {e}")
            raise

    def generate_customer(self):
        """Generate a single customer"""
        customer_type = random.choice(CUSTOMER_TYPES)
        is_business = customer_type == 'BUSINESS'

        if is_business:
            # Business customer
            company = fake.company()
            return {
                'id': str(uuid.uuid4()),
                'type': customer_type,
                'first_name': None,
                'last_name': None,
                'company_name': company,
                'email': fake.company_email(),
                'phone': fake.phone_number(),
                'pesel': None,
                'nip': fake.bothify(text='##########'),
                'regon': fake.bothify(text='#########'),
                'status': random.choice(CUSTOMER_STATUSES),
                'created_at': fake.date_time_between(start_date='-2y', end_date='now'),
                'updated_at': fake.date_time_between(start_date='-1y', end_date='now'),
            }
        else:
            # Individual customer
            return {
                'id': str(uuid.uuid4()),
                'type': customer_type,
                'first_name': fake.first_name(),
                'last_name': fake.last_name(),
                'company_name': None,
                'email': fake.email(),
                'phone': fake.phone_number(),
                'pesel': fake.bothify(text='##########'),
                'nip': None,
                'regon': None,
                'status': random.choice(CUSTOMER_STATUSES),
                'created_at': fake.date_time_between(start_date='-2y', end_date='now'),
                'updated_at': fake.date_time_between(start_date='-1y', end_date='now'),
            }

    def generate_address(self, customer_id):
        """Generate an address for a customer"""
        return {
            'id': str(uuid.uuid4()),
            'customer_id': customer_id,
            'type': random.choice(['BILLING', 'SHIPPING', 'PRIMARY']),
            'street': fake.street_address(),
            'city': fake.city(),
            'state': fake.state(),
            'postal_code': fake.postcode(),
            'country': 'PL',
            'is_primary': random.choice([True, False]),
            'created_at': fake.date_time_between(start_date='-1y', end_date='now'),
        }

    def generate_order(self, customer_id):
        """Generate a single order"""
        status = random.choice(ORDER_STATUSES)
        created_at = fake.date_time_between(start_date='-1y', end_date='now')

        # Ensure completed orders have a completion date
        completed_at = None
        if status in ['COMPLETED', 'CANCELLED', 'FAILED']:
            completed_at = created_at + timedelta(days=random.randint(0, 30))

        return {
            'id': str(uuid.uuid4()),
            'customer_id': customer_id,
            'order_number': f"ORD-{fake.bothify(text='########')}",
            'order_type': random.choice(ORDER_TYPES),
            'priority': random.choice(ORDER_PRIORITIES),
            'status': status,
            'total_amount': Decimal(str(random.uniform(10, 10000))).quantize(Decimal('0.01')),
            'currency': 'PLN',
            'requested_date': fake.date_between(start_date='-1y', end_date='+30d'),
            'promised_date': fake.date_between(start_date='-1y', end_date='+60d'),
            'completed_date': completed_at,
            'order_channel': random.choice(['WEB', 'MOBILE', 'PHONE', 'EMAIL', 'PORTAL']),
            'notes': fake.sentence(nb_words=10) if random.random() < 0.3 else None,
            'created_at': created_at,
            'updated_at': fake.date_time_between(start_date=created_at, end_date='now'),
        }

    def generate_order_item(self, order_id, product_id):
        """Generate an order item"""
        quantity = random.randint(1, 10)
        unit_price = Decimal(str(random.uniform(10, 1000))).quantize(Decimal('0.01'))

        return {
            'id': str(uuid.uuid4()),
            'order_id': order_id,
            'product_id': product_id,
            'quantity': quantity,
            'unit_price': unit_price,
            'total_price': (unit_price * quantity).quantize(Decimal('0.01')),
            'created_at': fake.date_time_between(start_date='-1y', end_date='now'),
        }

    def generate_product(self):
        """Generate a product"""
        return {
            'id': str(uuid.uuid4()),
            'product_code': f"PRD-{fake.bothify(text='####')}",
            'name': f"{random.choice(['Premium', 'Standard', 'Basic', 'Enterprise'])} {random.choice(['Service', 'Plan', 'Package'])}",
            'description': fake.text(max_nb_chars=200),
            'category': random.choice(PRODUCT_CATEGORIES),
            'price': Decimal(str(random.uniform(10, 1000))).quantize(Decimal('0.01')),
            'currency': 'PLN',
            'status': random.choice(['ACTIVE', 'INACTIVE', 'DRAFT']),
            'created_at': fake.date_time_between(start_date='-2y', end_date='now'),
            'updated_at': fake.date_time_between(start_date='-1y', end_date='now'),
        }

    def generate_payment(self, customer_id, amount):
        """Generate a payment"""
        status = random.choice(PAYMENT_STATUSES)
        created_at = fake.date_time_between(start_date='-1y', end_date='now')

        completed_at = None
        if status in ['COMPLETED', 'REFUNDED']:
            completed_at = created_at + timedelta(hours=random.randint(1, 72))

        return {
            'id': str(uuid.uuid4()),
            'customer_id': customer_id,
            'amount': amount,
            'currency': 'PLN',
            'payment_method': random.choice(PAYMENT_METHODS),
            'payment_status': status,
            'transaction_id': fake.bothify(text='TXN-################'),
            'payment_date': completed_at if completed_at else created_at,
            'created_at': created_at,
            'updated_at': fake.date_time_between(start_date=created_at, end_date='now'),
        }

    def generate_invoice(self, customer_id, order_id, amount):
        """Generate an invoice"""
        status = random.choice(INVOICE_STATUSES)
        created_at = fake.date_time_between(start_date='-1y', end_date='now')

        due_date = created_at + timedelta(days=14)
        paid_date = None
        if status == 'PAID':
            paid_date = due_date - timedelta(days=random.randint(0, 7))

        return {
            'id': str(uuid.uuid4()),
            'customer_id': customer_id,
            'order_id': order_id,
            'invoice_number': f"INV-{fake.bothify(text='########')}",
            'amount': amount,
            'currency': 'PLN',
            'status': status,
            'invoice_date': created_at,
            'due_date': due_date,
            'paid_date': paid_date,
            'created_at': created_at,
            'updated_at': fake.date_time_between(start_date=created_at, end_date='now'),
        }

    def generate_subscription(self, customer_id, product_id):
        """Generate a subscription"""
        status = random.choice(SUBSCRIPTION_STATUSES)
        start_date = fake.date_between(start_date='-1y', end_date='now')
        end_date = None
        if status in ['INACTIVE', 'CANCELLED', 'SUSPENDED']:
            end_date = start_date + timedelta(days=random.randint(30, 365))

        return {
            'id': str(uuid.uuid4()),
            'customer_id': customer_id,
            'product_id': product_id,
            'service_type': random.choice(SERVICE_TYPES),
            'status': status,
            'start_date': start_date,
            'end_date': end_date,
            'monthly_fee': Decimal(str(random.uniform(20, 500))).quantize(Decimal('0.01')),
            'currency': 'PLN',
            'created_at': fake.date_time_between(start_date='-1y', end_date='now'),
            'updated_at': fake.date_time_between(start_date='-1y', end_date='now'),
        }

    def generate_data(self, count, entity_type):
        """Generate test data"""
        data = []

        if entity_type == 'customers':
            print(f"Generating {count} customers...")
            for _ in range(count):
                data.append(self.generate_customer())
                if _ % 1000 == 0:
                    print(f"  Generated {_}/{count} customers")

        elif entity_type == 'products':
            print(f"Generating {count} products...")
            for _ in range(count):
                data.append(self.generate_product())
                if _ % 100 == 0:
                    print(f"  Generated {_}/{count} products")

        elif entity_type == 'orders':
            print(f"Generating {count} orders...")
            # First generate customers if not in DB
            for _ in range(count):
                customer = self.generate_customer()
                customer_id = customer['id']
                data.append({'type': 'customer', 'data': customer})
                data.append({'type': 'order', 'data': self.generate_order(customer_id)})
                if _ % 1000 == 0:
                    print(f"  Generated {_}/{count} orders")

        elif entity_type == 'payments':
            print(f"Generating {count} payments...")
            for _ in range(count):
                customer = self.generate_customer()
                amount = Decimal(str(random.uniform(10, 1000))).quantize(Decimal('0.01'))
                customer_id = customer['id']
                data.append({'type': 'customer', 'data': customer})
                data.append({'type': 'payment', 'data': self.generate_payment(customer_id, amount)})
                if _ % 1000 == 0:
                    print(f"  Generated {_}/{count} payments")

        print(f"✓ Generated {len(data)} records")
        return data

    def save_to_sql(self, data, filename):
        """Save data as SQL insert statements"""
        print(f"Saving to SQL file: {filename}")

        with open(filename, 'w') as f:
            f.write("-- BSS Test Data\n")
            f.write(f"-- Generated on {datetime.now().isoformat()}\n\n")

            for item in data:
                if item['type'] == 'customer':
                    f.write(self._customer_to_sql(item['data']))
                elif item['type'] == 'order':
                    f.write(self._order_to_sql(item['data']))
                elif item['type'] == 'payment':
                    f.write(self._payment_to_sql(item['data']))
                elif item['type'] == 'product':
                    f.write(self._product_to_sql(item['data']))
                elif item['type'] == 'subscription':
                    f.write(self._subscription_to_sql(item['data']))
                elif item['type'] == 'address':
                    f.write(self._address_to_sql(item['data']))
                elif item['type'] == 'order_item':
                    f.write(self._order_item_to_sql(item['data']))

        print(f"✓ Saved to {filename}")

    def _customer_to_sql(self, customer):
        return f"""INSERT INTO customers (
    id, type, first_name, last_name, company_name, email, phone, pesel, nip, regon, status, created_at, updated_at
) VALUES (
    '{customer['id']}',
    '{customer['type']}',
    {f"'{customer['first_name']}'" if customer['first_name'] else 'NULL'},
    {f"'{customer['last_name']}'" if customer['last_name'] else 'NULL'},
    {f"'{customer['company_name']}'" if customer['company_name'] else 'NULL'},
    '{customer['email']}',
    '{customer['phone']}',
    {f"'{customer['pesel']}'" if customer['pesel'] else 'NULL'},
    {f"'{customer['nip']}'" if customer['nip'] else 'NULL'},
    {f"'{customer['regon']}'" if customer['regon'] else 'NULL'},
    '{customer['status']}',
    '{customer['created_at'].isoformat()}',
    '{customer['updated_at'].isoformat()}'
);

"""

    def _order_to_sql(self, order):
        return f"""INSERT INTO orders (
    id, customer_id, order_number, order_type, priority, status, total_amount, currency, requested_date, promised_date, completed_date, order_channel, notes, created_at, updated_at
) VALUES (
    '{order['id']}',
    '{order['customer_id']}',
    '{order['order_number']}',
    '{order['order_type']}',
    '{order['priority']}',
    '{order['status']}',
    {float(order['total_amount'])},
    '{order['currency']}',
    '{order['requested_date'].isoformat()}',
    '{order['promised_date'].isoformat()}',
    {f"'{order['completed_date'].isoformat()}'" if order['completed_date'] else 'NULL'},
    '{order['order_channel']}',
    {f"'{order['notes']}'" if order['notes'] else 'NULL'},
    '{order['created_at'].isoformat()}',
    '{order['updated_at'].isoformat()}'
);

"""

    def _payment_to_sql(self, payment):
        return f"""INSERT INTO payments (
    id, customer_id, amount, currency, payment_method, payment_status, transaction_id, payment_date, created_at, updated_at
) VALUES (
    '{payment['id']}',
    '{payment['customer_id']}',
    {float(payment['amount'])},
    '{payment['currency']}',
    '{payment['payment_method']}',
    '{payment['payment_status']}',
    '{payment['transaction_id']}',
    '{payment['payment_date'].isoformat()}',
    '{payment['created_at'].isoformat()}',
    '{payment['updated_at'].isoformat()}'
);

"""

    def _product_to_sql(self, product):
        return f"""INSERT INTO products (
    id, product_code, name, description, category, price, currency, status, created_at, updated_at
) VALUES (
    '{product['id']}',
    '{product['product_code']}',
    '{product['name']}',
    '{product['description']}',
    '{product['category']}',
    {float(product['price'])},
    '{product['currency']}',
    '{product['status']}',
    '{product['created_at'].isoformat()}',
    '{product['updated_at'].isoformat()}'
);

"""

    def _subscription_to_sql(self, sub):
        return f"""INSERT INTO subscriptions (
    id, customer_id, product_id, service_type, status, start_date, end_date, monthly_fee, currency, created_at, updated_at
) VALUES (
    '{sub['id']}',
    '{sub['customer_id']}',
    '{sub['product_id']}',
    '{sub['service_type']}',
    '{sub['status']}',
    '{sub['start_date'].isoformat()}',
    {f"'{sub['end_date'].isoformat()}'" if sub['end_date'] else 'NULL'},
    {float(sub['monthly_fee'])},
    '{sub['currency']}',
    '{sub['created_at'].isoformat()}',
    '{sub['updated_at'].isoformat()}'
);

"""

    def _address_to_sql(self, address):
        return f"""INSERT INTO addresses (
    id, customer_id, type, street, city, state, postal_code, country, is_primary, created_at
) VALUES (
    '{address['id']}',
    '{address['customer_id']}',
    '{address['type']}',
    '{address['street']}',
    '{address['city']}',
    '{address['state']}',
    '{address['postal_code']}',
    '{address['country']}',
    {str(address['is_primary']).lower()},
    '{address['created_at'].isoformat()}'
);

"""

    def _order_item_to_sql(self, item):
        return f"""INSERT INTO order_items (
    id, order_id, product_id, quantity, unit_price, total_price, created_at
) VALUES (
    '{item['id']}',
    '{item['order_id']}',
    '{item['product_id']}',
    {item['quantity']},
    {float(item['unit_price'])},
    {float(item['total_price'])},
    '{item['created_at'].isoformat()}'
);

"""

    def save_to_json(self, data, filename):
        """Save data as JSON"""
        print(f"Saving to JSON file: {filename}")

        with open(filename, 'w') as f:
            json.dump(data, f, indent=2, default=str)

        print(f"✓ Saved to {filename}")

    def insert_to_db(self, data):
        """Insert data directly to database"""
        if not self.conn:
            print("✗ No database connection")
            return

        print("Inserting data to database...")

        # Insert customers
        customers = [item['data'] for item in data if item['type'] == 'customer']
        if customers:
            print(f"  Inserting {len(customers)} customers...")
            self._insert_customers(customers)

        # Insert other entities...
        print("✓ Data inserted successfully")

    def _insert_customers(self, customers):
        """Insert customers to database"""
        cursor = self.conn.cursor()
        query = """
        INSERT INTO customers (id, type, first_name, last_name, company_name, email, phone, pesel, nip, regon, status, created_at, updated_at)
        VALUES (%(id)s, %(type)s, %(first_name)s, %(last_name)s, %(company_name)s, %(email)s, %(phone)s, %(pesel)s, %(nip)s, %(regon)s, %(status)s, %(created_at)s, %(updated_at)s)
        ON CONFLICT (id) DO NOTHING
        """
        execute_batch(cursor, query, customers, page_size=1000)
        self.conn.commit()
        cursor.close()

    def close(self):
        """Close database connection"""
        if self.conn:
            self.conn.close()
            print("✓ Database connection closed")


def main():
    parser = argparse.ArgumentParser(description='Generate BSS test data')
    parser.add_argument('--count', type=int, default=1000, help='Number of records to generate')
    parser.add_argument('--output', choices=['sql', 'json', 'db'], default='sql', help='Output format')
    parser.add_argument('--generate', choices=['customers', 'products', 'orders', 'payments', 'all'], default='all', help='Entity type to generate')
    parser.add_argument('--output-file', type=str, help='Output file path')
    parser.add_argument('--db-url', type=str, help='Database connection URL')
    parser.add_argument('--seed', type=int, default=42, help='Random seed for reproducibility')

    args = parser.parse_args()

    # Set seed
    random.seed(args.seed)
    Faker.seed(args.seed)

    # Initialize generator
    generator = BSSDataGenerator(args.db_url)

    try:
        if args.generate == 'all':
            # Generate all entities
            customers = generator.generate_data(args.count, 'customers')
            products = generator.generate_data(min(100, args.count // 10), 'products')

            data = customers + [{'type': 'product', 'data': p} for p in products]
        else:
            data = generator.generate_data(args.count, args.generate)

        # Save data
        if args.output == 'sql':
            filename = args.output_file or f'test_data_{args.generate}_{args.count}.sql'
            generator.save_to_sql(data, filename)
        elif args.output == 'json':
            filename = args.output_file or f'test_data_{args.generate}_{args.count}.json'
            generator.save_to_json(data, filename)
        elif args.output == 'db':
            generator.insert_to_db(data)

        print("\n✓ Test data generation completed successfully!")

    finally:
        generator.close()


if __name__ == '__main__':
    main()
