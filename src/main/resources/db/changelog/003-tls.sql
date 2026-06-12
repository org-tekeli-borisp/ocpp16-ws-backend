-- liquibase formatted sql
-- changeset ocpp16:003-tls-cert-fingerprint
-- comment: Add certificate fingerprint column for mTLS authentication

ALTER TABLE charge_points ADD COLUMN cert_fingerprint VARCHAR(128);
