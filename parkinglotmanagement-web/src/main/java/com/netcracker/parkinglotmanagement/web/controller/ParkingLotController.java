package com.netcracker.parkinglotmanagement.web.controller;

import com.netcracker.parkinglotmanagement.api.dto.ParkingLotDTO;
import com.netcracker.parkinglotmanagement.api.service.ParkingLotService;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.jooq.exception.IOException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("parking-lot-management/parkingLot")
public class ParkingLotController {

    private final ParkingLotService parkingLotService;

    @Autowired
    public ParkingLotController(ParkingLotService parkingLotService) {
        this.parkingLotService = parkingLotService;
    }

    @PostMapping("/create-parkingLot")
    public ResponseEntity<ParkingLotDTO> createParkingLot(@RequestBody ParkingLotDTO parkingLotDTO) {
        parkingLotDTO.setId(UUID.randomUUID());
        ParkingLotDTO createdParkingLot = parkingLotService.createParkingLot(parkingLotDTO);
        return ResponseEntity.ok(createdParkingLot);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ParkingLotDTO> getParkingLotById(@PathVariable UUID id) {
        ParkingLotDTO parkingLotDTO = parkingLotService.getParkingLotById(id);
        return ResponseEntity.ok(parkingLotDTO);
    }

    @GetMapping
    public ResponseEntity<List<ParkingLotDTO>> getAllParkingLots() {
        List<ParkingLotDTO> parkingLots = parkingLotService.getAllParkingLots();
        return ResponseEntity.ok(parkingLots);
    }

    @PutMapping
    public ResponseEntity<ParkingLotDTO> updateParkingLot(@RequestBody ParkingLotDTO parkingLotDTO) {
        ParkingLotDTO updatedParkingLot = parkingLotService.updateParkingLot(parkingLotDTO);
        return ResponseEntity.ok(updatedParkingLot);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteParkingLot(@PathVariable UUID id) {
        parkingLotService.deleteParkingLot(id);
        return ResponseEntity.noContent().build();
    }

    //bulk

    @PostMapping("/upload-parkingLots")
    public ResponseEntity<List<ParkingLotDTO>> uploadParkingLots(@RequestParam("file") MultipartFile file) {
        List<ParkingLotDTO> parkingLots = new ArrayList<>();
        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            for (Row row : sheet) {
                if (row.getRowNum() == 0) {
                    continue;
                }
                ParkingLotDTO parkingLotDTO = new ParkingLotDTO(
                        UUID.randomUUID(),
                        (int) row.getCell(0).getNumericCellValue(),
                        row.getCell(1).getStringCellValue(),
                        row.getCell(2).getNumericCellValue(),
                        row.getCell(3).getNumericCellValue()

                );

                ParkingLotDTO createdParkingLot = parkingLotService.createParkingLot(parkingLotDTO);
                parkingLots.add(createdParkingLot);
            }
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        } catch (java.io.IOException e) {
            throw new RuntimeException(e);
        }
        return ResponseEntity.ok(parkingLots);
    }
}