package com.netcracker.parkinglotmanagement.api.consts;

public class ParkingLotConstants {

    private ParkingLotConstants(){

    }
    public static class TableColumnNames {
        public static final String ID = "id";
        public static final String NUMBER_OF_BLOCKS = "number_of_blocks";
        public static final String ADDRESS = "address";
        public static final String LONGITUDE = "longitude";
        public static final String LATITUDE = "latitude";
        public static final String TYPE = "@type";
        public static final String PARKING_LOT_ID = "parking_lot_id";
        public static final String BLOCK_CODE = "block_code";
        public static final String NUMBER_OF_FLOORS = "number_of_floors";
        public static final String FLOOR_NO = "floor_no";
        public static final String NUMBER_OF_SLOTS = "number_of_slots";
        public static final String VEHICLE_NUMBER = "vehicle_number";
        public static final String CONTACT_NUMBER = "contact_number";
        public static final String NAME = "name";
        public static final String EMAIL = "email";
        public static final String CUSTOMER_ID = "customer_id";
        public static final String START_TIMESTAMP = "start_timestamp";
        public static final String DURATION_IN_HOURS = "duration_in_hours";
        public static final String BOOKING_DATE = "booking_date";
        public static final String PARKING_SLOT_ID ="parking_slot_id" ;
        public static final String PARKING_SLOT_RESERVATION_ID = "parking_slot_reservation_id";
        public static final String ACTUAL_ENTRY_TIME = "actual_entry_time";
        public static final String ACTUAL_EXIT_TIME = "actual_exit_time";
        public static final String BASIC_COST = "basic_cost";
        public static final String PENALTY = "penalty";
        public static final String TOTAL_COST = "total_cost";
        public static final String FLOOR_ID = "floor_id";
        public static final String SLOT_NUMBER = "slot_number";
        public static final String CHARGES = "charges";
        public static final String VEHICLE_TYPE = "vehicle_type";


        private TableColumnNames() {
        }
    }

    public static class TableNames {
        public static final String PARKING_LOT = "parking_lot";
        public static final String BLOCK = "block";
        public static final String FLOOR = "floor";
        public static final String CUSTOMER = "customer";
        public static final String RESERVATION = "reservation";
        public static final String PARKING_SLIP = "parking_slip";
        public static final String PARKING_SLOT = "parking_slot";


        private TableNames() {
        }
    }

    public static final class TypeNames {
        public static final String UUID_TYPE = "uuid";

        private TypeNames() {
        }
    }
}
