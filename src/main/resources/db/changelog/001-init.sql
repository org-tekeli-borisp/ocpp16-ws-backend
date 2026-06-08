-- liquibase formatted sql

-- changeset borisp:001-create-sequences
CREATE SEQUENCE transactions_seq START WITH 1 INCREMENT BY 50;
CREATE SEQUENCE charge_points_seq START WITH 1 INCREMENT BY 50;

-- changeset borisp:002-create-charge-points
CREATE TABLE charge_points (
    id BIGINT PRIMARY KEY NOT NULL,
    chargePointId VARCHAR(255) NOT NULL,
    vendor VARCHAR(20),
    model VARCHAR(20),
    firmwareVersion VARCHAR(50),
    status VARCHAR(20) NOT NULL,
    sessionId VARCHAR(255) NOT NULL,
    lastSeenAt TIMESTAMP NOT NULL,
    createdAt TIMESTAMP NOT NULL
);

CREATE INDEX idx_chargepoint_id ON charge_points (chargePointId);
CREATE INDEX idx_status ON charge_points (status);

-- changeset borisp:003-create-transactions
CREATE TABLE transactions (
    id BIGINT PRIMARY KEY NOT NULL,
    chargePointId VARCHAR(255) NOT NULL,
    connectorId INT NOT NULL,
    idTag VARCHAR(20) NOT NULL,
    meterStart INT NOT NULL,
    startTime TIMESTAMP NOT NULL,
    stopTime TIMESTAMP,
    meterStop INT,
    stopReason VARCHAR(20),
    idTagEnd VARCHAR(20)
);

CREATE INDEX idx_txn_chargepoint ON transactions (chargePointId);
CREATE INDEX idx_txn_idtag ON transactions (idTag);
CREATE INDEX idx_txn_started ON transactions (startTime);
