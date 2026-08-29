-- V2: corrects type drift left by V1 and adds everything the parking operations
-- (entry, slot allocation, exit/billing, reservations, reporting) need.
--
-- Portability note: every statement below is restricted to the DDL subset that both
-- PostgreSQL and H2 accept, so the same file runs in production and in the test
-- profile. That is why the column retypes go through a temporary column and a
-- CAST instead of `ALTER COLUMN ... TYPE ... USING`, which is PostgreSQL-only.

-- ---------------------------------------------------------------------------
-- 1. parking_lot: numeric attributes were stored as text.
--    number_of_blocks was CHAR(1) - it could not hold a lot with 10+ blocks, and
--    the JPA entity/DTO already declared it as an Integer, so every insert of a
--    lot through ParkingLotRepository failed against a real PostgreSQL.
-- ---------------------------------------------------------------------------
ALTER TABLE parking_lot ADD COLUMN number_of_blocks_tmp INTEGER;
UPDATE parking_lot SET number_of_blocks_tmp = CAST(number_of_blocks AS INTEGER);
ALTER TABLE parking_lot DROP COLUMN number_of_blocks;
ALTER TABLE parking_lot ADD COLUMN number_of_blocks INTEGER;
UPDATE parking_lot SET number_of_blocks = number_of_blocks_tmp;
ALTER TABLE parking_lot DROP COLUMN number_of_blocks_tmp;
UPDATE parking_lot SET number_of_blocks = 0 WHERE number_of_blocks IS NULL;
ALTER TABLE parking_lot ALTER COLUMN number_of_blocks SET NOT NULL;

-- longitude / latitude were VARCHAR(50); the entity and DTO declare Double.
ALTER TABLE parking_lot ADD COLUMN longitude_tmp DOUBLE PRECISION;
ALTER TABLE parking_lot ADD COLUMN latitude_tmp DOUBLE PRECISION;
UPDATE parking_lot SET longitude_tmp = CAST(longitude AS DOUBLE PRECISION)
    WHERE longitude IS NOT NULL AND longitude <> '';
UPDATE parking_lot SET latitude_tmp = CAST(latitude AS DOUBLE PRECISION)
    WHERE latitude IS NOT NULL AND latitude <> '';
ALTER TABLE parking_lot DROP COLUMN longitude;
ALTER TABLE parking_lot DROP COLUMN latitude;
ALTER TABLE parking_lot ADD COLUMN longitude DOUBLE PRECISION;
ALTER TABLE parking_lot ADD COLUMN latitude DOUBLE PRECISION;
UPDATE parking_lot SET longitude = longitude_tmp, latitude = latitude_tmp;
ALTER TABLE parking_lot DROP COLUMN longitude_tmp;
ALTER TABLE parking_lot DROP COLUMN latitude_tmp;

-- ---------------------------------------------------------------------------
-- 2. customer.contact_number was INTEGER. A ten-digit number such as 9876543210
--    overflows a 32-bit integer (max 2147483647), and phone numbers are not
--    arithmetic values anyway.
-- ---------------------------------------------------------------------------
ALTER TABLE customer ADD COLUMN contact_number_tmp VARCHAR(20);
UPDATE customer SET contact_number_tmp = CAST(contact_number AS VARCHAR(20));
ALTER TABLE customer DROP COLUMN contact_number;
ALTER TABLE customer ADD COLUMN contact_number VARCHAR(20);
UPDATE customer SET contact_number = contact_number_tmp;
ALTER TABLE customer DROP COLUMN contact_number_tmp;
UPDATE customer SET contact_number = '' WHERE contact_number IS NULL;
ALTER TABLE customer ALTER COLUMN contact_number SET NOT NULL;

-- ---------------------------------------------------------------------------
-- 3. parking_slot: occupancy state plus an optimistic-locking version column.
--    Allocation is a guarded UPDATE on (id, status, version), so two concurrent
--    check-ins can never be handed the same slot.
-- ---------------------------------------------------------------------------
ALTER TABLE parking_slot ADD COLUMN status VARCHAR(20);
UPDATE parking_slot SET status = 'FREE';
ALTER TABLE parking_slot ALTER COLUMN status SET DEFAULT 'FREE';
ALTER TABLE parking_slot ALTER COLUMN status SET NOT NULL;

ALTER TABLE parking_slot ADD COLUMN version BIGINT;
UPDATE parking_slot SET version = 0;
ALTER TABLE parking_slot ALTER COLUMN version SET DEFAULT 0;
ALTER TABLE parking_slot ALTER COLUMN version SET NOT NULL;

-- ---------------------------------------------------------------------------
-- 4. reservation: a booking needs an explicit end of the window (overlap
--    detection cannot be expressed on start + duration portably), a lifecycle
--    status, and the registration of the vehicle the booking is for.
-- ---------------------------------------------------------------------------
ALTER TABLE reservation ADD COLUMN end_timestamp TIMESTAMP;
UPDATE reservation SET end_timestamp = start_timestamp;
ALTER TABLE reservation ALTER COLUMN end_timestamp SET NOT NULL;

ALTER TABLE reservation ADD COLUMN status VARCHAR(20);
UPDATE reservation SET status = 'BOOKED';
ALTER TABLE reservation ALTER COLUMN status SET DEFAULT 'BOOKED';
ALTER TABLE reservation ALTER COLUMN status SET NOT NULL;

ALTER TABLE reservation ADD COLUMN vehicle_number VARCHAR(20);
UPDATE reservation SET vehicle_number = '';
ALTER TABLE reservation ALTER COLUMN vehicle_number SET NOT NULL;

ALTER TABLE reservation ADD COLUMN created_at TIMESTAMP;
UPDATE reservation SET created_at = start_timestamp;
ALTER TABLE reservation ALTER COLUMN created_at SET NOT NULL;

-- ---------------------------------------------------------------------------
-- 5. parking_slip: V1 could only describe a slip that came from a reservation.
--    A walk-in has no reservation, so the slip needs its own slot reference,
--    the vehicle it was issued for, and a lifecycle status.
--
--    basic_cost / total_cost stay NOT NULL (H2 and PostgreSQL disagree on the
--    syntax for dropping a NOT NULL) and are seeded with 0.00 on entry; the
--    real amounts are written at exit.
-- ---------------------------------------------------------------------------
ALTER TABLE parking_slip ADD COLUMN parking_slot_id UUID;
ALTER TABLE parking_slip ADD COLUMN customer_id UUID;
ALTER TABLE parking_slip ADD COLUMN vehicle_number VARCHAR(20);
ALTER TABLE parking_slip ADD COLUMN vehicle_type VARCHAR(20);
ALTER TABLE parking_slip ADD COLUMN status VARCHAR(20);

UPDATE parking_slip SET vehicle_number = '' WHERE vehicle_number IS NULL;
UPDATE parking_slip SET vehicle_type = 'CAR' WHERE vehicle_type IS NULL;
UPDATE parking_slip SET status = 'CLOSED' WHERE actual_exit_time IS NOT NULL;
UPDATE parking_slip SET status = 'ACTIVE' WHERE status IS NULL;

ALTER TABLE parking_slip ALTER COLUMN vehicle_number SET NOT NULL;
ALTER TABLE parking_slip ALTER COLUMN vehicle_type SET NOT NULL;
ALTER TABLE parking_slip ALTER COLUMN status SET DEFAULT 'ACTIVE';
ALTER TABLE parking_slip ALTER COLUMN status SET NOT NULL;
ALTER TABLE parking_slip ALTER COLUMN penalty SET DEFAULT 0.00;

ALTER TABLE parking_slip
    ADD CONSTRAINT fk_parking_slip_slot FOREIGN KEY (parking_slot_id) REFERENCES parking_slot (id);
ALTER TABLE parking_slip
    ADD CONSTRAINT fk_parking_slip_customer FOREIGN KEY (customer_id) REFERENCES customer (id);

-- ---------------------------------------------------------------------------
-- 6. Indexes. Availability lookups walk parking_slot -> floor -> block ->
--    parking_lot on every check-in, and the reports scan parking_slip by exit
--    time; without these each one is a sequential scan.
-- ---------------------------------------------------------------------------
CREATE INDEX idx_block_parking_lot ON block (parking_lot_id);
CREATE INDEX idx_floor_block ON floor (block_id);
CREATE INDEX idx_parking_slot_floor ON parking_slot (floor_id);
CREATE INDEX idx_parking_slot_lookup ON parking_slot (status, vehicle_type);

CREATE INDEX idx_parking_slip_slot ON parking_slip (parking_slot_id, status);
CREATE INDEX idx_parking_slip_vehicle ON parking_slip (vehicle_number, status);
CREATE INDEX idx_parking_slip_entry ON parking_slip (actual_entry_time);
CREATE INDEX idx_parking_slip_exit ON parking_slip (actual_exit_time);

CREATE INDEX idx_reservation_slot_window ON reservation (parking_slot_id, start_timestamp, end_timestamp);
CREATE INDEX idx_reservation_status ON reservation (status);
CREATE INDEX idx_customer_vehicle ON customer (vehicle_number);
