#!/bin/bash
################################################################################
# Load Test Data into BSS Database
#
# This script generates and loads test data into the BSS PostgreSQL database
# for development, testing, and performance testing purposes.
################################################################################

set -e

# Colors
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

# Configuration
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../../../" && pwd)"
DATA_GENERATOR="$SCRIPT_DIR/generate_test_data.py"

# Default values
COUNT=${COUNT:-1000}
ENVIRONMENT=${ENVIRONMENT:-dev}
DB_HOST=${DB_HOST:-localhost}
DB_PORT=${DB_PORT:-5432}
DB_NAME=${DB_NAME:-bss}
DB_USER=${DB_USER:-bss_app}
DB_PASSWORD=${DB_PASSWORD:-}

# Get password from .env if not provided
if [ -z "$DB_PASSWORD" ] && [ -f "$PROJECT_ROOT/.env" ]; then
    source "$PROJECT_ROOT/.env"
fi

log_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Check dependencies
check_dependencies() {
    log_info "Checking dependencies..."

    if ! command -v python3 &> /dev/null; then
        log_error "Python 3 is not installed"
        exit 1
    fi

    if ! command -v psql &> /dev/null; then
        log_error "psql is not installed (postgresql-client)"
        exit 1
    fi

    log_info "Dependencies OK"
}

# Check database connection
check_db_connection() {
    log_info "Checking database connection..."

    if [ -z "$DB_PASSWORD" ]; then
        log_error "DB_PASSWORD not set (check .env file)"
        exit 1
    fi

    export PGPASSWORD="$DB_PASSWORD"

    if psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" -c "SELECT 1" &> /dev/null; then
        log_info "Database connection successful"
    else
        log_error "Cannot connect to database"
        log_error "Check: host=$DB_HOST, port=$DB_PORT, db=$DB_NAME, user=$DB_USER"
        exit 1
    fi
}

# Generate test data
generate_data() {
    local entity_type=$1
    local count=$2

    log_info "Generating $count $entity_type records..."

    python3 "$DATA_GENERATOR" \
        --generate "$entity_type" \
        --count "$count" \
        --output sql \
        --output-file "/tmp/test_data_${entity_type}_${count}.sql"

    if [ $? -eq 0 ]; then
        log_info "Data generated successfully"
        return 0
    else
        log_error "Failed to generate data"
        return 1
    fi
}

# Load data into database
load_data() {
    local sql_file=$1
    local table_name=$2

    log_info "Loading data from $sql_file into $table_name..."

    export PGPASSWORD="$DB_PASSWORD"

    # Count current records
    local current_count=$(psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" -t -c "SELECT COUNT(*) FROM $table_name" 2>/dev/null | xargs)

    # Execute SQL file
    psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" -f "$sql_file" > /dev/null

    if [ $? -eq 0 ]; then
        # Count new records
        local new_count=$(psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" -t -c "SELECT COUNT(*) FROM $table_name" 2>/dev/null | xargs)
        local added=$((new_count - current_count))
        log_info "Loaded $added records into $table_name"
        return 0
    else
        log_error "Failed to load data into $table_name"
        return 1
    fi
}

# Generate and load customers
load_customers() {
    log_info "=== Loading Customers ==="

    generate_data "customers" "$COUNT"

    if [ $? -eq 0 ]; then
        load_data "/tmp/test_data_customers_${COUNT}.sql" "customers"
        return $?
    else
        return 1
    fi
}

# Generate and load products
load_products() {
    local product_count=$((COUNT / 10))
    log_info "=== Loading Products (${product_count} records) ==="

    generate_data "products" "$product_count"

    if [ $? -eq 0 ]; then
        load_data "/tmp/test_data_products_${product_count}.sql" "products"
        return $?
    else
        return 1
    fi
}

# Generate and load orders
load_orders() {
    log_info "=== Loading Orders ==="

    generate_data "orders" "$COUNT"

    if [ $? -eq 0 ]; then
        # Load customers first
        load_data "/tmp/test_data_customers_${COUNT}.sql" "customers" 2>/dev/null || true
        # Load orders
        load_data "/tmp/test_data_orders_${COUNT}.sql" "orders"
        return $?
    else
        return 1
    fi
}

# Generate and load payments
load_payments() {
    log_info "=== Loading Payments ==="

    generate_data "payments" "$COUNT"

    if [ $? -eq 0 ]; then
        # Load customers first
        load_data "/tmp/test_data_customers_${COUNT}.sql" "customers" 2>/dev/null || true
        # Load payments
        load_data "/tmp/test_data_payments_${COUNT}.sql" "payments"
        return $?
    else
        return 1
    fi
}

# Load all data
load_all() {
    log_info "=== Loading All Test Data ==="

    # Load in dependency order
    load_customers || return 1
    load_products || return 1
    load_orders || return 1
    load_payments || return 1

    log_info "All test data loaded successfully!"
}

# Clean test data
clean_data() {
    log_warn "=== Cleaning Test Data ==="
    log_warn "This will delete all test data from the database!"

    read -p "Are you sure? (yes/no): " -r
    if [[ $REPLY =~ ^[Yy][Ee][Ss]$ ]]; then
        export PGPASSWORD="$DB_PASSWORD"

        log_info "Cleaning tables in reverse dependency order..."
        psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" -c "
            TRUNCATE TABLE order_items, orders, payments, invoices, subscriptions, addresses, customers CASCADE;
            TRUNCATE TABLE products CASCADE;
        " > /dev/null

        log_info "Test data cleaned successfully"
    else
        log_info "Clean cancelled"
    fi
}

# Show statistics
show_stats() {
    export PGPASSWORD="$DB_PASSWORD"

    log_info "=== Database Statistics ==="

    psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" -c "
        SELECT
            'customers' as table_name, COUNT(*) as record_count
        FROM customers
        UNION ALL
        SELECT
            'products' as table_name, COUNT(*) as record_count
        FROM products
        UNION ALL
        SELECT
            'orders' as table_name, COUNT(*) as record_count
        FROM orders
        UNION ALL
        SELECT
            'payments' as table_name, COUNT(*) as record_count
        FROM payments
        UNION ALL
        SELECT
            'invoices' as table_name, COUNT(*) as record_count
        FROM invoices
        UNION ALL
        SELECT
            'subscriptions' as table_name, COUNT(*) as record_count
        FROM subscriptions
        ORDER BY table_name;
    "
}

# Show usage
show_usage() {
    cat << EOF
BSS Test Data Loader

USAGE:
    $0 [COMMAND] [OPTIONS]

COMMANDS:
    all         Load all test data (default)
    customers   Load only customers
    products    Load only products
    orders      Load only orders
    payments    Load only payments
    clean       Remove all test data
    stats       Show database statistics

OPTIONS:
    --count N       Number of records (default: $COUNT)
    --env ENV       Environment: dev, stage, prod (default: $ENVIRONMENT)
    --db-host HOST  Database host (default: $DB_HOST)
    --db-port PORT  Database port (default: $DB_PORT)
    --db-name NAME  Database name (default: $DB_NAME)
    --db-user USER  Database user (default: $DB_USER)

ENVIRONMENT VARIABLES:
    DB_PASSWORD     Database password (from .env file)
    POSTGRES_PASSWORD   Alternative variable name

EXAMPLES:
    # Load 1000 customers
    $0 customers --count 1000

    # Load 10000 orders
    $0 orders --count 10000

    # Load all data
    $0 all --count 5000

    # Load data from custom database
    $0 all --count 1000 --db-host postgres.internal --db-user bss_admin

EOF
}

# Main execution
main() {
    local command=${1:-all}
    shift || true

    # Parse arguments
    while [[ $# -gt 0 ]]; do
        case $1 in
            --count)
                COUNT="$2"
                shift 2
                ;;
            --env)
                ENVIRONMENT="$2"
                shift 2
                ;;
            --db-host)
                DB_HOST="$2"
                shift 2
                ;;
            --db-port)
                DB_PORT="$2"
                shift 2
                ;;
            --db-name)
                DB_NAME="$2"
                shift 2
                ;;
            --db-user)
                DB_USER="$2"
                shift 2
                ;;
            --help)
                show_usage
                exit 0
                ;;
            *)
                log_error "Unknown option: $1"
                show_usage
                exit 1
                ;;
        esac
    done

    log_info "BSS Test Data Loader"
    log_info "Environment: $ENVIRONMENT"
    log_info "Record count: $COUNT"
    log_info "Database: $DB_USER@$DB_HOST:$DB_PORT/$DB_NAME"
    echo ""

    # Check dependencies
    check_dependencies

    # Check database connection
    check_db_connection

    # Execute command
    case $command in
        all)
            load_all
            ;;
        customers)
            load_customers
            ;;
        products)
            load_products
            ;;
        orders)
            load_orders
            ;;
        payments)
            load_payments
            ;;
        clean)
            clean_data
            ;;
        stats)
            show_stats
            ;;
        help|--help|-h)
            show_usage
            exit 0
            ;;
        *)
            log_error "Unknown command: $command"
            show_usage
            exit 1
            ;;
    esac

    # Show final statistics
    echo ""
    show_stats

    log_info "Done!"
}

# Run main
main "$@"
