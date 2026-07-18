-- liquibase formatted sql

-- changeset borisp:007-add-last-connected-at
ALTER TABLE charge_points ADD COLUMN last_connected_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;
