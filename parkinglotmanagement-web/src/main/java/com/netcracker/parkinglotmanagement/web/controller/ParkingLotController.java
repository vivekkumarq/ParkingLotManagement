package com.netcracker.parkinglotmanagement.web.controller;

import com.netcracker.parkinglotmanagement.api.dto.ParkingLotDTO;
import com.netcracker.parkinglotmanagement.api.exception.InvalidRequestException;
import com.netcracker.parkinglotmanagement.api.service.ParkingLotService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.Valid;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/parking-lot-management/parkingLot")
@Tag(name = "Parking lots", description = "The car parks themselves")
public class ParkingLotController {

    /** Column order expected in an uploaded workbook. */
    private static final int COL_NUMBER_OF_BLOCKS = 0;
    private static final int COL_ADDRESS = 1;
    private static final int COL_LONGITUDE = 2;
    private static final int COL_LATITUDE = 3;

    private final ParkingLotService parkingLotService;

    public ParkingLotController(ParkingLotService parkingLotService) {
        this.parkingLotService = parkingLotService;
    }

    @PostMapping("/create-parkingLot")
    @Operation(summary = "Create a parking lot")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Created"),
            @ApiResponse(responseCode = "400", description = "Validation failed")
    })
    public ResponseEntity<ParkingLotDTO> createParkingLot(@Valid @RequestBody ParkingLotDTO parkingLotDTO) {
        parkingLotDTO.setId(UUID.randomUUID());
        return ResponseEntity.status(HttpStatus.CREATED).body(parkingLotService.createParkingLot(parkingLotDTO));
    }

    /**
     * A missing lot used to come back as {@code 200 OK} with a {@code null} body;
     * the service now raises a not-found error and the advice turns it into a 404.
     */
    @GetMapping("/{id}")
    @Operation(summary = "Fetch one parking lot")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Found"),
            @ApiResponse(responseCode = "404", description = "No such parking lot")
    })
    public ResponseEntity<ParkingLotDTO> getParkingLotById(@PathVariable UUID id) {
        return ResponseEntity.ok(parkingLotService.getParkingLotById(id));
    }

    @GetMapping
    @Operation(summary = "List every parking lot")
    public ResponseEntity<List<ParkingLotDTO>> getAllParkingLots() {
        return ResponseEntity.ok(parkingLotService.getAllParkingLots());
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a parking lot")
    public ResponseEntity<ParkingLotDTO> updateParkingLot(@PathVariable UUID id,
                                                          @Valid @RequestBody ParkingLotDTO parkingLotDTO) {
        parkingLotDTO.setId(id);
        return ResponseEntity.ok(parkingLotService.updateParkingLot(parkingLotDTO));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a parking lot")
    @ApiResponse(responseCode = "204", description = "Deleted")
    public ResponseEntity<Void> deleteParkingLot(@PathVariable UUID id) {
        parkingLotService.deleteParkingLot(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Bulk-creates lots from an .xlsx workbook whose first row is a header and whose
     * columns are: number of blocks, address, longitude, latitude.
     *
     * <p>Three problems in the original version are fixed here. It caught
     * {@code org.jooq.exception.IOException} - an unrelated class that this code can
     * never throw - and rethrew the real {@code java.io.IOException} as a bare
     * {@code RuntimeException}, producing a 500 with a stack trace. It also read
     * cells without checking they were present or numeric, so one short or
     * mistyped row threw a {@code NullPointerException} halfway through the import.
     * Rows are now validated up front and reported by row number.
     */
    @PostMapping("/upload-parkingLots")
    @Operation(summary = "Bulk-create parking lots from an .xlsx workbook",
            description = "Columns: numberOfBlocks, address, longitude, latitude. The first row is skipped "
                    + "as a header. The whole file is rejected if any row is malformed.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "All rows imported"),
            @ApiResponse(responseCode = "400", description = "The workbook could not be read, or a row is malformed")
    })
    public ResponseEntity<List<ParkingLotDTO>> uploadParkingLots(@RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new InvalidRequestException("A non-empty .xlsx file is required");
        }

        List<ParkingLotDTO> parsed = new ArrayList<>();
        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            for (Row row : sheet) {
                if (row.getRowNum() == 0) {
                    continue;
                }
                if (isBlank(row)) {
                    continue;
                }
                parsed.add(new ParkingLotDTO(
                        UUID.randomUUID(),
                        (int) requireNumeric(row, COL_NUMBER_OF_BLOCKS, "numberOfBlocks"),
                        requireText(row, COL_ADDRESS, "address"),
                        optionalNumeric(row, COL_LONGITUDE),
                        optionalNumeric(row, COL_LATITUDE)));
            }
        } catch (IOException e) {
            throw new InvalidRequestException("The uploaded file is not a readable .xlsx workbook");
        }

        // Parse everything before writing anything, so a bad row on line 40 does not
        // leave 39 lots half-imported.
        List<ParkingLotDTO> created = new ArrayList<>(parsed.size());
        for (ParkingLotDTO lot : parsed) {
            created.add(parkingLotService.createParkingLot(lot));
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    private static boolean isBlank(Row row) {
        for (int column = COL_NUMBER_OF_BLOCKS; column <= COL_LATITUDE; column++) {
            Cell cell = row.getCell(column);
            if (cell != null && cell.getCellType() != org.apache.poi.ss.usermodel.CellType.BLANK) {
                return false;
            }
        }
        return true;
    }

    private static double requireNumeric(Row row, int column, String name) {
        Cell cell = row.getCell(column);
        if (cell == null || cell.getCellType() != org.apache.poi.ss.usermodel.CellType.NUMERIC) {
            throw new InvalidRequestException(
                    "Row " + (row.getRowNum() + 1) + ": '" + name + "' must be a number");
        }
        return cell.getNumericCellValue();
    }

    private static String requireText(Row row, int column, String name) {
        Cell cell = row.getCell(column);
        if (cell == null || cell.getCellType() != org.apache.poi.ss.usermodel.CellType.STRING
                || cell.getStringCellValue().trim().isEmpty()) {
            throw new InvalidRequestException(
                    "Row " + (row.getRowNum() + 1) + ": '" + name + "' must be non-empty text");
        }
        return cell.getStringCellValue().trim();
    }

    private static Double optionalNumeric(Row row, int column) {
        Cell cell = row.getCell(column);
        if (cell == null || cell.getCellType() != org.apache.poi.ss.usermodel.CellType.NUMERIC) {
            return null;
        }
        return cell.getNumericCellValue();
    }
}
