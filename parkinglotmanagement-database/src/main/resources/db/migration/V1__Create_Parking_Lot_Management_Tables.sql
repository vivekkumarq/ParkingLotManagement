-- Create parking_lot table
CREATE TABLE parking_lot (
    id UUID PRIMARY KEY,
    number_of_blocks CHAR(1) NOT NULL,
    address VARCHAR(500) NOT NULL,
    longitude VARCHAR(50),
    latitude VARCHAR(50)
);

-- Create block table
CREATE TABLE block (
    id UUID PRIMARY KEY,
    parking_lot_id UUID REFERENCES parking_lot(id),
    block_code VARCHAR(2) NOT NULL,
    number_of_floors INTEGER NOT NULL
);

-- Create floor table
CREATE TABLE floor (
    id UUID PRIMARY KEY,
    block_id UUID REFERENCES block(id),
    floor_no INTEGER NOT NULL,
    number_of_slots INTEGER NOT NULL
);

-- Create parking_slot table
CREATE TABLE parking_slot (
    id UUID PRIMARY KEY,
    floor_id UUID REFERENCES floor(id),
    slot_number INTEGER NOT NULL,
    charges INTEGER NOT NULL,
    vehicle_type VARCHAR(20) NOT NULL
);

-- Create customer table
CREATE TABLE customer (
    id UUID PRIMARY KEY,
    vehicle_number VARCHAR(20) NOT NULL,
    contact_number INTEGER NOT NULL,
    name VARCHAR(50) NOT NULL,
    email VARCHAR(50) NOT NULL
);

-- Create reservation table
CREATE TABLE reservation (
    id UUID PRIMARY KEY,
    customer_id UUID REFERENCES customer(id),
    start_timestamp TIMESTAMP NOT NULL,
    duration_in_hours INTEGER NOT NULL,
    booking_date DATE NOT NULL,
    parking_slot_id UUID REFERENCES parking_slot(id)
);

-- Create parking_slip table
CREATE TABLE parking_slip (
    id UUID PRIMARY KEY,
    parking_slot_reservation_id UUID REFERENCES reservation(id),
    actual_entry_time TIMESTAMP,
    actual_exit_time TIMESTAMP,
    basic_cost DECIMAL(10, 2) NOT NULL,
    penalty DECIMAL(10, 2),
    total_cost DECIMAL(10, 2) NOT NULL
);
