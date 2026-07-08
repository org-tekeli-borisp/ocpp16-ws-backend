--liquibase formatted sql

--changeset borisp:004-message-log
CREATE SEQUENCE ocpp_message_logs_seq START WITH 1 INCREMENT BY 50;

CREATE TABLE ocpp_message_logs (
    id BIGINT PRIMARY KEY NOT NULL,
    charge_point_id VARCHAR(255) NOT NULL,
    direction VARCHAR(10) NOT NULL,
    message_type VARCHAR(15) NOT NULL,
    action VARCHAR(100),
    message_id VARCHAR(255),
    payload TEXT,
    timestamp TIMESTAMP NOT NULL
);

CREATE INDEX idx_msg_log_cp_ts ON ocpp_message_logs(charge_point_id, timestamp DESC);
