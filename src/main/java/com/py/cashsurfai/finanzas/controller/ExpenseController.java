package com.py.cashsurfai.finanzas.controller;

import com.py.cashsurfai.finanzas.services.ExcelExportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayInputStream;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.WeekFields;

@RestController
@RequestMapping("/api/expenses")
public class ExpenseController {

    private final ExcelExportService excelExportService;

    @Autowired
    public ExpenseController(ExcelExportService excelExportService) {
        this.excelExportService = excelExportService;
    }

    @GetMapping("/excel/{userId}")
    public ResponseEntity<Resource> downloadExpenseExcel(
            @PathVariable Long userId,
            @RequestParam("startDate") @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam("endDate") @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {
        try {
            ByteArrayInputStream in = excelExportService.generateExpenseExcel(userId, startDate, endDate);
            InputStreamResource resource = new InputStreamResource(in);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=expenses_" + userId + ".xlsx")
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(resource);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/excel/{userId}/monthly")
    public ResponseEntity<Resource> downloadMonthlyExpenseExcel(
            @PathVariable Long userId,
            @RequestParam("month") @DateTimeFormat(pattern = "yyyy-MM") YearMonth month) {
        LocalDate startDate = month.atDay(1);
        LocalDate endDate = month.atEndOfMonth();
        return downloadExpenseExcel(userId, startDate, endDate);
    }

    // Endpoint para descarga semanal
    @GetMapping("/excel/{userId}/weekly")
    public ResponseEntity<Resource> downloadWeeklyExpenseExcel(
            @PathVariable Long userId,
            @RequestParam("year") int year,
            @RequestParam("week") int week) {
        try {
            // Calcular el inicio y fin de la semana según el año y número de semana
            WeekFields weekFields = WeekFields.ISO; // Usar estándar ISO para semanas
            LocalDate startDate = LocalDate.ofYearDay(year, 1)
                    .with(weekFields.weekOfYear(), week)
                    .with(weekFields.dayOfWeek(), 1); // Lunes como inicio
            LocalDate endDate = startDate.plusDays(6); // Domingo como fin

            ByteArrayInputStream in = excelExportService.generateExpenseExcelAdvance(userId, startDate, endDate);
            InputStreamResource resource = new InputStreamResource(in);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=expenses_" + userId + "_week" + week + ".xlsx")
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(resource);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

}