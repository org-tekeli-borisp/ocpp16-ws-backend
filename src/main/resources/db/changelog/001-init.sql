-- liquibase formatted sql

-- changeset borisp:004-create-sequences-v2
CREATE SEQUENCE transactions_seq START WITH 1 INCREMENT BY 50;
CREATE SEQUENCE charge_points_seq START WITH 1 INCREMENT BY 50;

-- changeset borisp:005-create-charge-points-v2
CREATE TABLE charge_points (
    id BIGINT PRIMARY KEY NOT NULL,
    charge_point_id VARCHAR(255) NOT NULL,
    vendor VARCHAR(20),
    model VARCHAR(20),
    firmware_version VARCHAR(50),
    status VARCHAR(20) NOT NULL,
    session_id VARCHAR(255) NOT NULL,
    last_seen_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_chargepoint_id ON charge_points (charge_point_id);
CREATE INDEX idx_status ON charge_points (status);

-- changeset borisp:006-create-transactions-v2
CREATE TABLE transactions (
    id BIGINT PRIMARY KEY NOT NULL,
    charge_point_id VARCHAR(255) NOT NULL,
    connector_id INT NOT NULL,
    id_tag VARCHAR(20) NOT NULL,
    meter_start INT NOT NULL,
    start_time TIMESTAMP NOT NULL,
    stop_time TIMESTAMP,
    meter_stop INT,
    stop_reason VARCHAR(20),
    id_tag_end VARCHAR(20)
);

CREATE INDEX idx_txn_chargepoint ON transactions (charge_point_id);
CREATE INDEX idx_txn_idtag ON transactions (id_tag);
CREATE INDEX idx_txn_started ON transactions (start_time);
