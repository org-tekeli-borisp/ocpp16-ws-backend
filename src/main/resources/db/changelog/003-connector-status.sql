--liquibase formatted sql

--changeset borisp:003-connector-status
CREATE SEQUENCE connector_status_seq START WITH 1 INCREMENT BY 50;

CREATE TABLE connector_status (
    id BIGINT PRIMARY KEY NOT NULL,
    charge_point_id VARCHAR(255) NOT NULL,
    connector_id INT NOT NULL,
    status VARCHAR(20) NOT NULL,
    error_code VARCHAR(20) NOT NULL,
    info VARCHAR(50),
    timestamp TIMESTAMP NOT NULL
);

CREATE INDEX idx_connector_status_cp ON connector_status(charge_point_id);
CREATE UNIQUE INDEX idx_connector_status_cp_connector ON connector_status(charge_point_id, connector_id);
