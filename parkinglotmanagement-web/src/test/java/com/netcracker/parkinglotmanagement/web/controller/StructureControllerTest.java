package com.netcracker.parkinglotmanagement.web.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.netcracker.parkinglotmanagement.api.dto.BlockDTO;
import com.netcracker.parkinglotmanagement.api.dto.CustomerDTO;
import com.netcracker.parkinglotmanagement.api.dto.FloorDTO;
import com.netcracker.parkinglotmanagement.api.dto.ParkingLotDTO;
import com.netcracker.parkinglotmanagement.api.exception.InvalidRequestException;
import com.netcracker.parkinglotmanagement.api.exception.ResourceNotFoundException;
import com.netcracker.parkinglotmanagement.api.service.BlockService;
import com.netcracker.parkinglotmanagement.api.service.CustomerService;
import com.netcracker.parkinglotmanagement.api.service.FloorService;
import com.netcracker.parkinglotmanagement.api.service.ParkingLotService;
import com.netcracker.parkinglotmanagement.web.error.GlobalExceptionHandler;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The four controllers that already existed, covering the behaviour that used to
 * be wrong: request bodies that could not be deserialised, an unknown id
 * answering 200 with an empty body, and an .xlsx upload that threw on a short row.
 */
class StructureControllerTest {

    @Nested
    @WebMvcTest(ParkingLotController.class)
    @Import(GlobalExceptionHandler.class)
    @DisplayName("ParkingLotController")
    class ParkingLots {

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private ObjectMapper objectMapper;

        @MockBean
        private ParkingLotService parkingLotService;

        private ParkingLotDTO lot() {
            return new ParkingLotDTO(UUID.randomUUID(), 3, "12 MG Road, Bengaluru", 77.5946, 12.9716);
        }

        @Test
        @DisplayName("a JSON body deserialises - the DTO used to have no no-arg constructor")
        void createAcceptsAJsonBody() throws Exception {
            ParkingLotDTO lot = lot();
            when(parkingLotService.createParkingLot(any())).thenReturn(lot);

            mockMvc.perform(post("/parking-lot-management/parkingLot/create-parkingLot")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"numberOfBlocks\":3,\"address\":\"12 MG Road, Bengaluru\","
                                    + "\"longitude\":77.5946,\"latitude\":12.9716}"))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.address").value("12 MG Road, Bengaluru"))
                    .andExpect(jsonPath("$.@type").value("ParkingLot"));
        }

        @Test
        @DisplayName("an unknown id is a 404, not a 200 with an empty body")
        void unknownLotIsNotFound() throws Exception {
            UUID id = UUID.randomUUID();
            when(parkingLotService.getParkingLotById(id))
                    .thenThrow(new ResourceNotFoundException("Parking lot", id));

            mockMvc.perform(get("/parking-lot-management/parkingLot/" + id))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
        }

        @Test
        @DisplayName("an out-of-range coordinate is rejected")
        void coordinatesAreValidated() throws Exception {
            mockMvc.perform(post("/parking-lot-management/parkingLot/create-parkingLot")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"numberOfBlocks\":3,\"address\":\"x\",\"longitude\":999.0}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.violations[0].field").value("longitude"));
        }

        @Test
        @DisplayName("a blank address is rejected")
        void addressIsRequired() throws Exception {
            mockMvc.perform(post("/parking-lot-management/parkingLot/create-parkingLot")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"numberOfBlocks\":3,\"address\":\"  \"}"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void listReturnsEverything() throws Exception {
            when(parkingLotService.getAllParkingLots()).thenReturn(Arrays.asList(lot(), lot()));

            mockMvc.perform(get("/parking-lot-management/parkingLot"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(2)));
        }

        @Test
        @DisplayName("PUT takes the id from the path")
        void updateUsesThePathId() throws Exception {
            UUID pathId = UUID.randomUUID();
            when(parkingLotService.updateParkingLot(any())).thenReturn(lot());

            mockMvc.perform(put("/parking-lot-management/parkingLot/" + pathId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(lot())))
                    .andExpect(status().isOk());

            verify(parkingLotService).updateParkingLot(argThat(dto -> pathId.equals(dto.getId())));
        }

        @Test
        void deleteReturnsNoContent() throws Exception {
            UUID id = UUID.randomUUID();

            mockMvc.perform(delete("/parking-lot-management/parkingLot/" + id))
                    .andExpect(status().isNoContent());

            verify(parkingLotService).deleteParkingLot(id);
        }

        @Test
        @DisplayName("a well-formed workbook imports every row")
        void uploadImportsRows() throws Exception {
            when(parkingLotService.createParkingLot(any())).thenAnswer(call -> call.getArgument(0));

            byte[] workbook = workbook(new Object[][]{
                    {"numberOfBlocks", "address", "longitude", "latitude"},
                    {3.0, "12 MG Road", 77.5946, 12.9716},
                    {2.0, "42 Residency Road", 77.6033, 12.9698}
            });

            mockMvc.perform(multipart("/parking-lot-management/parkingLot/upload-parkingLots")
                            .file(new MockMultipartFile("file", "lots.xlsx",
                                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", workbook)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$", hasSize(2)))
                    .andExpect(jsonPath("$[0].address").value("12 MG Road"));
        }

        @Test
        @DisplayName("a row with a missing cell is a 400 naming the row, not an NPE")
        void uploadRejectsAMalformedRow() throws Exception {
            byte[] workbook = workbook(new Object[][]{
                    {"numberOfBlocks", "address", "longitude", "latitude"},
                    {3.0, "12 MG Road", 77.5946, 12.9716},
                    {2.0}   // address missing
            });

            mockMvc.perform(multipart("/parking-lot-management/parkingLot/upload-parkingLots")
                            .file(new MockMultipartFile("file", "lots.xlsx",
                                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", workbook)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message")
                            .value(org.hamcrest.Matchers.containsString("Row 3")));

            verify(parkingLotService, org.mockito.Mockito.never()).createParkingLot(any());
        }

        @Test
        @DisplayName("a file that is not a workbook is a 400, not a 500")
        void uploadRejectsGarbage() throws Exception {
            mockMvc.perform(multipart("/parking-lot-management/parkingLot/upload-parkingLots")
                            .file(new MockMultipartFile("file", "lots.xlsx",
                                    "application/octet-stream", "not a workbook".getBytes())))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("an empty upload is a 400")
        void uploadRejectsAnEmptyFile() throws Exception {
            mockMvc.perform(multipart("/parking-lot-management/parkingLot/upload-parkingLots")
                            .file(new MockMultipartFile("file", "lots.xlsx",
                                    "application/octet-stream", new byte[0])))
                    .andExpect(status().isBadRequest());
        }

        private byte[] workbook(Object[][] rows) throws Exception {
            try (XSSFWorkbook workbook = new XSSFWorkbook();
                 ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                Sheet sheet = workbook.createSheet("lots");
                for (int rowIndex = 0; rowIndex < rows.length; rowIndex++) {
                    Row row = sheet.createRow(rowIndex);
                    for (int cellIndex = 0; cellIndex < rows[rowIndex].length; cellIndex++) {
                        Object value = rows[rowIndex][cellIndex];
                        if (value instanceof Double) {
                            row.createCell(cellIndex).setCellValue((Double) value);
                        } else {
                            row.createCell(cellIndex).setCellValue(String.valueOf(value));
                        }
                    }
                }
                workbook.write(out);
                return out.toByteArray();
            }
        }
    }

    @Nested
    @WebMvcTest(CustomerController.class)
    @Import(GlobalExceptionHandler.class)
    @DisplayName("CustomerController")
    class Customers {

        @Autowired
        private MockMvc mockMvc;

        @MockBean
        private CustomerService customerService;

        private CustomerDTO customer() {
            return new CustomerDTO(UUID.randomUUID(), "KA01AB1234", "9876543210",
                    "Asha Rao", "asha.rao@example.com");
        }

        @Test
        @DisplayName("a ten-digit phone number is accepted - it used to be an int")
        void tenDigitPhoneNumberIsAccepted() throws Exception {
            when(customerService.createCustomer(any())).thenAnswer(call -> call.getArgument(0));

            mockMvc.perform(post("/parking-lot-management/customer/create-customer")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"vehicleNumber\":\"KA01AB1234\",\"contactNumber\":\"9876543210\","
                                    + "\"name\":\"Asha Rao\",\"email\":\"asha.rao@example.com\"}"))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.contactNumber").value("9876543210"));
        }

        @Test
        @DisplayName("an invalid email is rejected")
        void emailIsValidated() throws Exception {
            mockMvc.perform(post("/parking-lot-management/customer/create-customer")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"vehicleNumber\":\"KA01AB1234\",\"contactNumber\":\"9876543210\","
                                    + "\"name\":\"Asha Rao\",\"email\":\"not-an-email\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.violations[0].field").value("email"));
        }

        @Test
        @DisplayName("a phone number that is not a phone number is rejected")
        void phoneNumberIsValidated() throws Exception {
            mockMvc.perform(post("/parking-lot-management/customer/create-customer")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"vehicleNumber\":\"KA01AB1234\",\"contactNumber\":\"abc\","
                                    + "\"name\":\"Asha Rao\",\"email\":\"a@b.com\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.violations[0].field").value("contactNumber"));
        }

        @Test
        @DisplayName("the search parameter reaches the service - it used to be discarded")
        void searchIsPassedThrough() throws Exception {
            when(customerService.getAllCustomers("name==Asha*"))
                    .thenReturn(Collections.singletonList(customer()));

            mockMvc.perform(get("/parking-lot-management/customer").param("search", "name==Asha*"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)));

            verify(customerService).getAllCustomers("name==Asha*");
        }

        @Test
        @DisplayName("an unknown filter field is a 400")
        void unknownFilterFieldIsBadRequest() throws Exception {
            when(customerService.getAllCustomers("password==x"))
                    .thenThrow(new InvalidRequestException("Unknown filter field 'password'"));

            mockMvc.perform(get("/parking-lot-management/customer").param("search", "password==x"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
        }

        @Test
        void unknownCustomerIsNotFound() throws Exception {
            UUID id = UUID.randomUUID();
            when(customerService.getCustomerById(id)).thenThrow(new ResourceNotFoundException("Customer", id));

            mockMvc.perform(get("/parking-lot-management/customer/" + id))
                    .andExpect(status().isNotFound());
        }

        @Test
        void deleteReturnsNoContent() throws Exception {
            UUID id = UUID.randomUUID();

            mockMvc.perform(delete("/parking-lot-management/customer/" + id))
                    .andExpect(status().isNoContent());

            verify(customerService).deleteCustomer(id);
        }
    }

    @Nested
    @WebMvcTest(BlockController.class)
    @Import(GlobalExceptionHandler.class)
    @DisplayName("BlockController")
    class Blocks {

        @Autowired
        private MockMvc mockMvc;

        @MockBean
        private BlockService blockService;

        @Test
        @DisplayName("create returns 201 with the assigned id - it used to return an empty body")
        void createReturnsTheBlock() throws Exception {
            BlockDTO block = new BlockDTO(UUID.randomUUID(), UUID.randomUUID(), "A1", 4);
            when(blockService.createBlock(any())).thenReturn(block);

            mockMvc.perform(post("/parking-lot-management/blocks/create-blocks")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"parkingLotId\":\"" + block.getParkingLotId()
                                    + "\",\"blockCode\":\"A1\",\"numberOfFloors\":4}"))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(block.getId().toString()))
                    .andExpect(jsonPath("$.blockCode").value("A1"));
        }

        @Test
        @DisplayName("a block code longer than two characters is rejected")
        void blockCodeIsValidated() throws Exception {
            mockMvc.perform(post("/parking-lot-management/blocks/create-blocks")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"parkingLotId\":\"" + UUID.randomUUID()
                                    + "\",\"blockCode\":\"TOOLONG\",\"numberOfFloors\":4}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.violations[0].field").value("blockCode"));
        }

        @Test
        void unknownBlockIsNotFound() throws Exception {
            UUID blockId = UUID.randomUUID();
            UUID lotId = UUID.randomUUID();
            when(blockService.getBlockById(blockId, lotId))
                    .thenThrow(new ResourceNotFoundException("Block " + blockId + " was not found"));

            mockMvc.perform(get("/parking-lot-management/blocks/" + blockId + "/" + lotId))
                    .andExpect(status().isNotFound());
        }

        @Test
        void deleteReturnsNoContent() throws Exception {
            UUID blockId = UUID.randomUUID();
            UUID lotId = UUID.randomUUID();

            mockMvc.perform(delete("/parking-lot-management/blocks/" + blockId + "/" + lotId))
                    .andExpect(status().isNoContent());

            verify(blockService).deleteBlock(blockId, lotId);
        }
    }

    @Nested
    @WebMvcTest(FloorController.class)
    @Import(GlobalExceptionHandler.class)
    @DisplayName("FloorController")
    class Floors {

        @Autowired
        private MockMvc mockMvc;

        @MockBean
        private FloorService floorService;

        @Test
        void createReturnsCreated() throws Exception {
            FloorDTO floor = new FloorDTO(UUID.randomUUID(), UUID.randomUUID(), 1, 50);
            when(floorService.createFloor(any())).thenReturn(floor);

            mockMvc.perform(post("/parking-lot-management/floors/create-floors")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"blockId\":\"" + floor.getBlockId()
                                    + "\",\"floorNo\":1,\"numberOfSlots\":50}"))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.numberOfSlots").value(50));
        }

        @Test
        @DisplayName("a floor with no slots is rejected")
        void slotCountIsValidated() throws Exception {
            mockMvc.perform(post("/parking-lot-management/floors/create-floors")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"blockId\":\"" + UUID.randomUUID()
                                    + "\",\"floorNo\":1,\"numberOfSlots\":0}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.violations[0].field").value("numberOfSlots"));
        }

        @Test
        void unknownFloorIsNotFound() throws Exception {
            UUID id = UUID.randomUUID();
            when(floorService.getFloorById(id)).thenThrow(new ResourceNotFoundException("Floor", id));

            mockMvc.perform(get("/parking-lot-management/floors/" + id))
                    .andExpect(status().isNotFound());
        }

        @Test
        void deletingAnUnknownFloorIsNotFound() throws Exception {
            UUID id = UUID.randomUUID();
            doThrow(new ResourceNotFoundException("Floor", id)).when(floorService).deleteFloor(id);

            mockMvc.perform(delete("/parking-lot-management/floors/" + id))
                    .andExpect(status().isNotFound());
        }
    }
}
