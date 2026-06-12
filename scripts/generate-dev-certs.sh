#!/usr/bin/env bash
# Generate self-signed CA + server + client certificates for local development.
# Usage: ./scripts/generate-dev-certs.sh [output_dir]
#
# This creates:
#   certs/ca.crt           – Self-signed CA certificate
#   certs/ca.key           – CA private key
#   certs/server.crt       – Server certificate (signed by CA)
#   certs/server.key       – Server private key
#   certs/client.crt       – Client certificate (signed by CA)
#   certs/client.key       – Client private key
#
# The server cert has SANs for localhost and 127.0.0.1.
# The client cert has CN=CP-DEV for testing charge point connections.
set -euo pipefail

CERTS_DIR="${1:-certs}"
DAYS=3650

mkdir -p "$CERTS_DIR"

echo "==> Generating CA key and certificate..."
openssl req -x509 -newkey rsa:4096 -nodes \
    -keyout "$CERTS_DIR/ca.key" \
    -out "$CERTS_DIR/ca.crt" \
    -days "$DAYS" \
    -subj "/C=AT/ST=Vienna/L=Vienna/O=OCPP Dev CA/CN=OCPP Development CA"

echo "==> Generating server key and CSR..."
openssl req -newkey rsa:2048 -nodes \
    -keyout "$CERTS_DIR/server.key" \
    -out "$CERTS_DIR/server.csr" \
    -subj "/C=AT/ST=Vienna/L=Vienna/O=OCPP Dev Server/CN=localhost" \
    -addext "subjectAltName=DNS:localhost,IP:127.0.0.1,IP:::1"

echo "==> Signing server certificate with CA..."
openssl x509 -req \
    -in "$CERTS_DIR/server.csr" \
    -CA "$CERTS_DIR/ca.crt" \
    -CAkey "$CERTS_DIR/ca.key" \
    -CAcreateserial \
    -out "$CERTS_DIR/server.crt" \
    -days "$DAYS" \
    -copy_extensions copyall

echo "==> Generating client key and CSR..."
openssl req -newkey rsa:2048 -nodes \
    -keyout "$CERTS_DIR/client.key" \
    -out "$CERTS_DIR/client.csr" \
    -subj "/C=AT/ST=Vienna/L=Vienna/O=OCPP Dev Client/CN=CP-DEV"

echo "==> Signing client certificate with CA..."
openssl x509 -req \
    -in "$CERTS_DIR/client.csr" \
    -CA "$CERTS_DIR/ca.crt" \
    -CAkey "$CERTS_DIR/ca.key" \
    -CAcreateserial \
    -out "$CERTS_DIR/client.crt" \
    -days "$DAYS"

# Clean up CSR and serial files
rm -f "$CERTS_DIR"/*.csr "$CERTS_DIR"/*.srl

echo ""
echo "==> Certificates generated in $CERTS_DIR/"
echo ""
echo "To enable TLS in docker-compose:"
echo "  QUARKUS_HTTP_SSL_ENABLED=true \\"
echo "  QUARKUS_HTTP_SSL_CLIENT_CERT=REQUEST \\"
echo "  docker compose up -d"
echo ""
echo "To test the server certificate:"
echo "  openssl s_client -connect localhost:8443 -CAfile $CERTS_DIR/ca.crt"
echo ""
echo "To test WSS connection with client certificate:"
echo "  wscat -connect wss://localhost:8443/ocpp/CP-001 \\"
echo "    --cert $CERTS_DIR/client.crt --key $CERTS_DIR/client.key \\"
echo "    --ca $CERTS_DIR/ca.crt"
