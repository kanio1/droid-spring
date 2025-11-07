#!/bin/bash

# Migration Validation Script
# Validates all Flyway migrations before deployment

set -e

echo "=================================="
echo "BSS Migration Validation Script"
echo "=================================="
echo ""

# Configuration
BACKEND_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)/backend"
MIGRATION_DIR="$BACKEND_DIR/src/main/resources/db/migration"
POSTGRES_URL="${POSTGRES_URL:-jdbc:postgresql://localhost:5432/validation_bss}"
POSTGRES_USER="${POSTGRES_USER:-postgres}"
POSTGRES_PASSWORD="${POSTGRES_PASSWORD:-postgres}"

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Counters
ERRORS=0
WARNINGS=0

# Function to print status
print_status() {
    local status=$1
    local message=$2
    if [ "$status" == "ERROR" ]; then
        echo -e "${RED}[ERROR]${NC} $message"
        ((ERRORS++))
    elif [ "$status" == "WARNING" ]; then
        echo -e "${YELLOW}[WARNING]${NC} $message"
        ((WARNINGS++))
    elif [ "$status" == "SUCCESS" ]; then
        echo -e "${GREEN}[SUCCESS]${NC} $message"
    else
        echo "[INFO] $message"
    fi
}

# Check if migration directory exists
if [ ! -d "$MIGRATION_DIR" ]; then
    print_status "ERROR" "Migration directory not found: $MIGRATION_DIR"
    exit 1
fi

print_status "INFO" "Validating migrations in: $MIGRATION_DIR"

# 1. Check migration file naming convention
echo ""
echo "1. Checking migration file naming convention..."
for file in "$MIGRATION_DIR"/*.sql; do
    if [ -f "$file" ]; then
        filename=$(basename "$file")
        if [[ ! "$filename" =~ ^V[0-9]+(_[0-9]+)*__.*\.sql$ ]]; then
            print_status "ERROR" "Invalid naming convention: $filename"
            print_status "INFO" "  Expected format: V{VERSION}__{DESCRIPTION}.sql"
        else
            print_status "SUCCESS" "Valid naming: $filename"
        fi
    fi
done

# 2. Check for SQL syntax errors
echo ""
echo "2. Checking for SQL syntax issues..."

# Check for statements without semicolons
for file in "$MIGRATION_DIR"/*.sql; do
    if [ -f "$file" ]; then
        # Check for CREATE TABLE without semicolon
        if grep -q "CREATE TABLE" "$file" && ! grep -q ";\s*$" "$file"; then
            print_status "WARNING" "Possible missing semicolon in: $(basename "$file")"
        fi

        # Check for DROP without IF EXISTS (potentially dangerous)
        if grep -q "^DROP " "$file" && ! grep -q "IF EXISTS" "$file"; then
            print_status "WARNING" "DROP without IF EXISTS in: $(basename "$file")"
        fi

        # Check for potential blocking operations
        if grep -qi "ALTER TABLE.*ADD COLUMN" "$file" && ! grep -qi "NULL" "$file"; then
            print_status "WARNING" "ADD COLUMN without NULL check in: $(basename "$file") - may block table"
        fi
    fi
done

# 3. Check for duplicate versions
echo ""
echo "3. Checking for duplicate versions..."
versions=$(cd "$MIGRATION_DIR" && ls *.sql 2>/dev/null | sed -E 's/^V([0-9_]+).__.*/\1/' | sort | uniq -d)
if [ -n "$versions" ]; then
    print_status "ERROR" "Duplicate migration versions found:"
    echo "$versions" | while read version; do
        print_status "ERROR" "  Version: $version"
    done
else
    print_status "SUCCESS" "No duplicate versions found"
fi

# 4. Check migration order
echo ""
echo "4. Checking migration order..."
prev_version=""
for file in $(cd "$MIGRATION_DIR" && ls *.sql 2>/dev/null | sort -V); do
    version=$(echo "$file" | sed -E 's/^V([0-9_]+).__.*/\1/')
    if [ -n "$prev_version" ]; then
        # Check if current version is greater than previous
        if [ "$(echo -e "$prev_version\n$version" | sort -V | head -n1)" = "$version" ]; then
            print_status "WARNING" "Version ordering issue: $prev_version -> $version"
        fi
    fi
    prev_version="$version"
done

# 5. Check for potentially slow migrations
echo ""
echo "5. Checking for potentially slow migrations..."

# Large table creations
for file in "$MIGRATION_DIR"/*.sql; do
    if [ -f "$file" ]; then
        filename=$(basename "$file")

        # Check for large CREATE TABLE statements
        table_count=$(grep -c "CREATE TABLE" "$file" || true)
        if [ $table_count -gt 3 ]; then
            print_status "WARNING" "Multiple table creation in single migration: $filename ($table_count tables)"
        fi

        # Check for missing indexes on foreign keys
        fk_count=$(grep -c "FOREIGN KEY" "$file" || true)
        idx_count=$(grep -c "CREATE INDEX" "$file" || true)
        if [ $fk_count -gt 0 ] && [ $idx_count -eq 0 ]; then
            print_status "WARNING" "Foreign keys without indexes in: $filename"
        fi
    fi
done

# 6. Validate against database (if available)
echo ""
echo "6. Validating against database..."

if command -v psql &> /dev/null; then
    export PGPASSWORD="$POSTGRES_PASSWORD"

    # Test connection
    if psql -h "${POSTGRES_URL#*@}" -U "$POSTGRES_USER" -d "${POSTGRES_URL##*/}" -c "SELECT 1" &> /dev/null; then
        print_status "SUCCESS" "Database connection successful"

        # Run Flyway validate
        cd "$BACKEND_DIR"
        if mvn -q flyway:validate -Dspring.datasource.url="$POSTGRES_URL" \
               -Dspring.datasource.username="$POSTGRES_USER" \
               -Dspring.datasource.password="$POSTGRES_PASSWORD" 2>&1; then
            print_status "SUCCESS" "Flyway validation passed"
        else
            print_status "ERROR" "Flyway validation failed"
        fi
    else
        print_status "WARNING" "Could not connect to database (skipping DB validation)"
    fi
else
    print_status "WARNING" "psql not available (skipping DB validation)"
fi

# 7. Generate report
echo ""
echo "=================================="
echo "Validation Summary"
echo "=================================="
echo "Total Errors: $ERRORS"
echo "Total Warnings: $WARNINGS"
echo ""

if [ $ERRORS -gt 0 ]; then
    print_status "ERROR" "Validation FAILED - Please fix errors before deployment"
    exit 1
elif [ $WARNINGS -gt 0 ]; then
    print_status "WARNING" "Validation completed with warnings - Review recommended"
    exit 0
else
    print_status "SUCCESS" "Validation PASSED - All checks successful"
    exit 0
fi
