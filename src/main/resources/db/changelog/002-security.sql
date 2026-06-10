-- liquibase formatted sql

-- changeset borisp:007-create-security-logs
CREATE SEQUENCE security_logs_seq START WITH 1 INCREMENT BY 50;

CREATE TABLE security_logs (
    id BIGINT PRIMARY KEY NOT NULL,
    charge_point_id VARCHAR(255) NOT NULL,
    type VARCHAR(50) NOT NULL,
    timestamp TIMESTAMP NOT NULL,
    tech_info VARCHAR(255),
    created_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_security_logs_chargepoint ON security_logs (charge_point_id);
CREATE INDEX idx_security_logs_type ON security_logs (type);
CREATE INDEX idx_security_logs_timestamp ON security_logs (timestamp);

-- changeset borisp:008-create-signed-firmware
CREATE SEQUENCE signed_firmware_seq START WITH 1 INCREMENT BY 50;

CREATE TABLE signed_firmware (
    id BIGINT PRIMARY KEY NOT NULL,
    charge_point_id VARCHAR(255) NOT NULL,
    request_id INT NOT NULL,
    location VARCHAR(512) NOT NULL,
    retrieve_date_time TIMESTAMP NOT NULL,
    install_date_time TIMESTAMP,
    signing_certificate VARCHAR(5500) NOT NULL,
    signature VARCHAR(800) NOT NULL,
    status VARCHAR(30) NOT NULL,
    created_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_signed_firmware_chargepoint ON signed_firmware (charge_point_id);
CREATE INDEX idx_signed_firmware_request_id ON signed_firmware (request_id);
CREATE INDEX idx_signed_firmware_status ON signed_firmware (status);
