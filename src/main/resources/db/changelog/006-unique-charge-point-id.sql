-- liquibase formatted sql

-- changeset borisp:007-unique-charge-point-id
ALTER TABLE charge_points ADD CONSTRAINT uq_charge_point_id UNIQUE (charge_point_id);
