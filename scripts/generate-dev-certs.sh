#!/usr/bin/env bash
set -euo pipefail

require_command() {
  if ! command -v "$1" >/dev/null 2>&1; then
    echo "Missing required command: $1" >&2
    exit 1
  fi
}

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
CERTS_DIR="$ROOT_DIR/dev/certs"
CA_KEY="dev-rootCA.key.pem"
CA_CERT="dev-rootCA.crt.pem"
LEAF_KEY="dev-localhost.key.pem"
LEAF_CERT="dev-localhost.crt.pem"
LEAF_CSR="dev-localhost.csr.pem"
SERIAL_FILE="dev-rootCA.srl"

USE_DOCKER_OPENSSL=false
OPENSSL_IMAGE="frapsoft/openssl:latest"

if command -v openssl >/dev/null 2>&1; then
  run_openssl() {
    openssl "$@"
  }
else
  require_command docker
  USE_DOCKER_OPENSSL=true
  run_openssl() {
    docker run --rm \
      -u "$(id -u):$(id -g)" \
      -v "$CERTS_DIR":/work \
      -w /work \
      "$OPENSSL_IMAGE" \
      "$@"
  }
fi

mkdir -p "$CERTS_DIR"
cd "$CERTS_DIR"

if [ ! -f "$CA_KEY" ] || [ ! -f "$CA_CERT" ]; then
  run_openssl genrsa -out "$CA_KEY" 4096 >/dev/null 2>&1

  run_openssl req \
    -x509 \
    -new \
    -nodes \
    -key "$CA_KEY" \
    -sha256 \
    -days 3650 \
    -subj "/C=PL/ST=Development/L=Local/O=BSS Dev/CN=BSS Dev Root CA" \
    -out "$CA_CERT" >/dev/null 2>&1

  echo "Created development root CA at $CERTS_DIR/$CA_CERT"
fi

CONF_FILE="openssl-req.cnf"

cat >"$CONF_FILE" <<'EOF'
[req]
default_bits = 2048
prompt = no
default_md = sha256
req_extensions = v3_req
distinguished_name = dn

[dn]
C = PL
ST = Development
L = Local
O = BSS Dev
CN = localhost

[v3_req]
subjectAltName = @alt_names
keyUsage = digitalSignature, keyEncipherment
extendedKeyUsage = serverAuth

[alt_names]
DNS.1 = localhost
IP.1 = 127.0.0.1
IP.2 = ::1
EOF

run_openssl genrsa -out "$LEAF_KEY" 2048 >/dev/null 2>&1

run_openssl req \
  -new \
  -key "$LEAF_KEY" \
  -out "$LEAF_CSR" \
  -config "$CONF_FILE" >/dev/null 2>&1

run_openssl x509 \
  -req \
  -in "$LEAF_CSR" \
  -CA "$CA_CERT" \
  -CAkey "$CA_KEY" \
  -CAcreateserial \
  -CAserial "$SERIAL_FILE" \
  -out "$LEAF_CERT" \
  -days 825 \
  -sha256 \
  -extfile "$CONF_FILE" \
  -extensions v3_req >/dev/null 2>&1

rm -f "$LEAF_CSR"
rm -f "$CONF_FILE"

echo "Generated development TLS certificate at $CERTS_DIR/$LEAF_CERT"
echo "Trust the root certificate ($CERTS_DIR/$CA_CERT) in your system store if not already trusted."
