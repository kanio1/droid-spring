#!/bin/bash

# TLS Certificate Generation Script
# Generates CA and certificates for PostgreSQL, Redis, and Kafka

set -e

CERT_DIR="/home/labadmin/projects/droid-spring/dev/certs"
CA_DIR="${CERT_DIR}/ca"
POSTGRES_DIR="${CERT_DIR}/postgres"
REDIS_DIR="${CERT_DIR}/redis"
KAFKA_DIR="${CERT_DIR}/kafka"

echo "=========================================="
echo "Generating TLS Certificates"
echo "=========================================="
echo ""

# Create directories
mkdir -p "${CA_DIR}" "${POSTGRES_DIR}" "${REDIS_DIR}" "${KAFKA_DIR}"

# Generate CA private key and certificate
echo "1. Generating CA certificate..."
openssl genrsa -out "${CA_DIR}/ca-key.pem" 4096
openssl req -new -x509 -key "${CA_DIR}/ca-key.pem" -out "${CA_DIR}/ca-cert.pem" -days 3650 \
    -subj "/C=US/ST=CA/L=San Francisco/O=BSS/OU=IT/CN=ca.bss.local"

echo "✅ CA certificate generated"
echo ""

# Function to generate certificate for a service
generate_cert() {
    local service_name=$1
    local service_dir=$2
    local cn=$3

    echo "Generating certificate for ${service_name}..."

    # Generate private key
    openssl genrsa -out "${service_dir}/${service_name}-key.pem" 2048

    # Generate certificate signing request
    openssl req -new -key "${service_dir}/${service_name}-key.pem" \
        -out "${service_dir}/${service_name}.csr" \
        -subj "/C=US/ST=CA/L=San Francisco/O=BSS/OU=IT/CN=${cn}"

    # Sign certificate with CA
    openssl x509 -req -in "${service_dir}/${service_name}.csr" \
        -CA "${CA_DIR}/ca-cert.pem" -CAkey "${CA_DIR}/ca-key.pem" \
        -CAcreateserial -out "${service_dir}/${service_name}-cert.pem" \
        -days 3650 -sha256

    # Create PKCS12 bundle (for Java keystores)
    openssl pkcs12 -export -in "${service_dir}/${service_name}-cert.pem" \
        -inkey "${service_dir}/${service_name}-key.pem" \
        -out "${service_dir}/${service_name}.p12" -name "${service_name}" \
        -passout pass:changeit

    # Create client certificate bundle (for PostgreSQL client auth)
    if [ "${service_name}" = "postgres" ]; then
        openssl pkcs12 -export -in "${service_dir}/${service_name}-cert.pem" \
            -inkey "${service_dir}/${service_name}-key.pem" \
            -out "${service_dir}/client-${service_name}.p12" -name "client" \
            -passout pass:changeit
    fi

    # Clean up CSR file
    rm "${service_dir}/${service_name}.csr"

    echo "✅ ${service_name} certificate generated"
}

# Generate PostgreSQL certificates
generate_cert "postgres" "${POSTGRES_DIR}" "postgres.bss.local"

# Generate Redis certificates
generate_cert "redis" "${REDIS_DIR}" "redis.bss.local"

# Generate Kafka certificates
generate_cert "kafka" "${KAFKA_DIR}" "kafka.bss.local"

# Create truststore for Java applications
echo ""
echo "Creating Java truststore..."
keytool -importcert -noprompt -alias bss-ca -file "${CA_DIR}/ca-cert.pem" \
    -keystore "${CERT_DIR}/truststore.jks" -storepass changeit

echo "✅ Truststore created"
echo ""

# Set proper permissions
chmod 600 "${CA_DIR}"/*.pem "${POSTGRES_DIR}"/*.pem "${REDIS_DIR}"/*.pem "${KAFKA_DIR}"/*.pem
chmod 644 "${CA_DIR}"/*.pem "${POSTGRES_DIR}"/*.p12 "${REDIS_DIR}"/*.p12 "${KAFKA_DIR}"/*.p12

echo "=========================================="
echo "Certificate Generation Complete!"
echo "=========================================="
echo ""
echo "Certificates generated in: ${CERT_DIR}"
echo ""
echo "Structure:"
echo "  ${CA_DIR}/"
echo "    ca-cert.pem (CA certificate)"
echo "    ca-key.pem (CA private key)"
echo "  ${POSTGRES_DIR}/"
echo "    postgres-cert.pem"
echo "    postgres-key.pem"
echo "    postgres.p12 (keystore)"
echo "    client-postgres.p12 (client keystore)"
echo "  ${REDIS_DIR}/"
echo "    redis-cert.pem"
echo "    redis-key.pem"
echo "    redis.p12 (keystore)"
echo "  ${KAFKA_DIR}/"
echo "    kafka-cert.pem"
echo "    kafka-key.pem"
echo "    kafka.p12 (keystore)"
echo "  ${CERT_DIR}/"
echo "    truststore.jks (Java truststore for CA)"
echo ""
echo "To use these certificates:"
echo "  - CA certificate: Import to client truststores"
echo "  - Service certs: Use in service configurations"
echo "  - P12 bundles: Use in Java applications"
echo ""
