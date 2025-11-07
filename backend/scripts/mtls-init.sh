#!/bin/bash

# mTLS Certificate Management Script
# Initializes and manages TLS certificates for PostgreSQL, Redis, and Kafka
# Part of BSS Security Infrastructure

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"
CERT_DIR="${PROJECT_ROOT}/dev/certs"

echo "=========================================="
echo "BSS mTLS Certificate Management"
echo "=========================================="
echo ""

# Color codes for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Function to print colored output
print_status() {
    echo -e "${GREEN}✓${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}⚠${NC} $1"
}

print_error() {
    echo -e "${RED}✗${NC} $1"
}

# Check if certificates exist
check_certificates() {
    echo "Checking existing certificates..."

    if [ ! -d "${CERT_DIR}" ]; then
        print_warning "Certificate directory not found: ${CERT_DIR}"
        return 1
    fi

    local missing=0

    # Check CA certificate
    if [ ! -f "${CERT_DIR}/ca/ca-cert.pem" ]; then
        print_error "CA certificate not found"
        missing=1
    else
        print_status "CA certificate found"
    fi

    # Check PostgreSQL certificates
    if [ ! -f "${CERT_DIR}/postgres/postgres-cert.pem" ]; then
        print_error "PostgreSQL certificate not found"
        missing=1
    else
        print_status "PostgreSQL certificate found"
    fi

    # Check Redis certificates
    if [ ! -f "${CERT_DIR}/redis/redis-cert.pem" ]; then
        print_error "Redis certificate not found"
        missing=1
    else
        print_status "Redis certificate found"
    fi

    # Check Kafka certificates
    if [ ! -f "${CERT_DIR}/kafka/kafka-cert.pem" ]; then
        print_error "Kafka certificate not found"
        missing=1
    else
        print_status "Kafka certificate found"
    fi

    # Check truststore
    if [ ! -f "${CERT_DIR}/truststore.jks" ]; then
        print_error "Java truststore not found"
        missing=1
    else
        print_status "Java truststore found"
    fi

    if [ ${missing} -eq 0 ]; then
        print_status "All certificates present"
        return 0
    else
        print_warning "Some certificates are missing"
        return 1
    fi
}

# Generate certificates
generate_certificates() {
    echo ""
    echo "Generating mTLS certificates..."
    echo ""

    if [ -f "${PROJECT_ROOT}/dev/scripts/generate-certs.sh" ]; then
        bash "${PROJECT_ROOT}/dev/scripts/generate-certs.sh"
        print_status "Certificates generated successfully"
    else
        print_error "Certificate generation script not found: ${PROJECT_ROOT}/dev/scripts/generate-certs.sh"
        exit 1
    fi
}

# Validate certificates
validate_certificates() {
    echo ""
    echo "Validating certificate integrity..."
    echo ""

    local valid=0

    # Validate CA certificate
    if openssl x509 -in "${CERT_DIR}/ca/ca-cert.pem" -noout -text > /dev/null 2>&1; then
        print_status "CA certificate is valid"
        ((valid++))
    else
        print_error "CA certificate is invalid"
    fi

    # Validate PostgreSQL certificate
    if openssl x509 -in "${CERT_DIR}/postgres/postgres-cert.pem" -noout -text > /dev/null 2>&1; then
        print_status "PostgreSQL certificate is valid"
        ((valid++))
    else
        print_error "PostgreSQL certificate is invalid"
    fi

    # Validate Redis certificate
    if openssl x509 -in "${CERT_DIR}/redis/redis-cert.pem" -noout -text > /dev/null 2>&1; then
        print_status "Redis certificate is valid"
        ((valid++))
    else
        print_error "Redis certificate is invalid"
    fi

    # Validate Kafka certificate
    if openssl x509 -in "${CERT_DIR}/kafka/kafka-cert.pem" -noout -text > /dev/null 2>&1; then
        print_status "Kafka certificate is valid"
        ((valid++))
    else
        print_error "Kafka certificate is invalid"
    fi

    # Validate truststore
    if keytool -list -keystore "${CERT_DIR}/truststore.jks" -storepass changeit > /dev/null 2>&1; then
        print_status "Java truststore is valid"
        ((valid++))
    else
        print_error "Java truststore is invalid"
    fi

    if [ ${valid} -eq 5 ]; then
        print_status "All certificates are valid"
        return 0
    else
        print_error "Some certificates are invalid (${valid}/5)"
        return 1
    fi
}

# Show certificate information
show_certificate_info() {
    echo ""
    echo "Certificate Information:"
    echo "========================"
    echo ""

    # CA certificate info
    echo "CA Certificate:"
    openssl x509 -in "${CERT_DIR}/ca/ca-cert.pem" -noout -subject -issuer -dates
    echo ""

    # PostgreSQL certificate info
    echo "PostgreSQL Certificate:"
    openssl x509 -in "${CERT_DIR}/postgres/postgres-cert.pem" -noout -subject -issuer -dates
    echo ""

    # Redis certificate info
    echo "Redis Certificate:"
    openssl x509 -in "${CERT_DIR}/redis/redis-cert.pem" -noout -subject -issuer -dates
    echo ""

    # Kafka certificate info
    echo "Kafka Certificate:"
    openssl x509 -in "${CERT_DIR}/kafka/kafka-cert.pem" -noout -subject -issuer -dates
    echo ""

    # Truststore info
    echo "Java Truststore:"
    keytool -list -keystore "${CERT_DIR}/truststore.jks" -storepass changeit | grep -A 5 "Keystore type"
    echo ""
}

# Clean certificates
clean_certificates() {
    echo ""
    echo "Cleaning certificate directory..."
    echo ""

    if [ -d "${CERT_DIR}" ]; then
        rm -rf "${CERT_DIR}"
        print_status "Certificate directory removed"
    fi

    print_status "Certificates cleaned"
}

# Set permissions
set_permissions() {
    echo ""
    echo "Setting certificate permissions..."
    echo ""

    # Set proper permissions on certificate directory
    if [ -d "${CERT_DIR}" ]; then
        find "${CERT_DIR}" -type f -name "*.pem" -exec chmod 600 {} \;
        find "${CERT_DIR}" -type f -name "*.p12" -exec chmod 644 {} \;
        find "${CERT_DIR}" -type f -name "*.jks" -exec chmod 644 {} \;
        find "${CERT_DIR}" -type d -exec chmod 755 {} \;
        print_status "Permissions set correctly"
    fi
}

# Check dependencies
check_dependencies() {
    echo "Checking dependencies..."

    local missing=0

    if ! command -v openssl &> /dev/null; then
        print_error "openssl is not installed"
        missing=1
    fi

    if ! command -v keytool &> /dev/null; then
        print_error "keytool is not installed (Java JDK required)"
        missing=1
    fi

    if [ ${missing} -eq 0 ]; then
        print_status "All dependencies available"
        return 0
    else
        print_error "Some dependencies are missing"
        return 1
    fi
}

# Main script logic
case "${1:-check}" in
    "check")
        echo "Checking mTLS certificate status..."
        check_certificates
        ;;

    "generate")
        check_dependencies
        generate_certificates
        set_permissions
        validate_certificates
        ;;

    "validate")
        check_certificates
        validate_certificates
        ;;

    "info")
        check_certificates
        show_certificate_info
        ;;

    "clean")
        clean_certificates
        ;;

    "renew")
        clean_certificates
        check_dependencies
        generate_certificates
        set_permissions
        validate_certificates
        ;;

    "help"|"-h"|"--help")
        echo "Usage: $0 [check|generate|validate|info|clean|renew|help]"
        echo ""
        echo "Commands:"
        echo "  check    - Check if certificates exist"
        echo "  generate - Generate new certificates"
        echo "  validate - Validate certificate integrity"
        echo "  info     - Show certificate information"
        echo "  clean    - Remove all certificates"
        echo "  renew    - Clean and regenerate certificates"
        echo "  help     - Show this help message"
        echo ""
        ;;

    *)
        print_error "Unknown command: $1"
        echo "Use '$0 help' for usage information"
        exit 1
        ;;
esac

echo ""
echo "=========================================="
echo "mTLS Certificate Management Complete"
echo "=========================================="
