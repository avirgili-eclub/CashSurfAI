package com.py.cashsurfai.finanzas.services;
import com.py.cashsurfai.finanzas.domain.models.entity.Expense;
import com.py.cashsurfai.finanzas.domain.repository.ExpenseRepository;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class ExcelExportService {

    private final ExpenseRepository expenseRepository;

    @Autowired
    public ExcelExportService(ExpenseRepository expenseRepository) {
        this.expenseRepository = expenseRepository;
    }

    public ByteArrayInputStream generateExpenseExcel(Long userId, LocalDate startDate, LocalDate endDate) throws IOException {
        List<Expense> expenses = expenseRepository.findByUserIdAndDateBetween(userId, startDate, endDate);

        try (SXSSFWorkbook workbook = new SXSSFWorkbook(100)) {
            Sheet sheet = workbook.createSheet("Expenses");

            // Encabezados
            Row headerRow = sheet.createRow(0);
            headerRow.createCell(0).setCellValue("Amount");
            headerRow.createCell(1).setCellValue("Date");
            headerRow.createCell(2).setCellValue("Description");
            headerRow.createCell(3).setCellValue("Category");

            // Datos
            int rowNum = 1;
            for (Expense expense : expenses) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(expense.getAmount());
                row.createCell(1).setCellValue(expense.getDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
                row.createCell(2).setCellValue(expense.getDescription());
//                row.createCell(3).setCellValue(expense.getCategory());
            }

            // Establecer anchos fijos para columnas (en unidades de 1/256 de un carácter)
            sheet.setColumnWidth(0, 15 * 256); // Amount: 15 caracteres
            sheet.setColumnWidth(1, 12 * 256); // Date: 12 caracteres
            sheet.setColumnWidth(2, 30 * 256); // Description: 30 caracteres
            sheet.setColumnWidth(3, 15 * 256); // Category: 15 caracteres

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            workbook.dispose();
            return new ByteArrayInputStream(out.toByteArray());
        }
    }

    public ByteArrayInputStream generateExpenseExcelAdvance(Long userId, LocalDate startDate, LocalDate endDate) throws IOException {
        List<Expense> expenses = expenseRepository.findByUserIdAndDateBetween(userId, startDate, endDate);

        // Usar try-with-resources para cerrar el workbook automáticamente
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Expenses");

            // Encabezados
            Row headerRow = sheet.createRow(0);
            headerRow.createCell(0).setCellValue("Amount");
            headerRow.createCell(1).setCellValue("Date");
            headerRow.createCell(2).setCellValue("Description");
            headerRow.createCell(3).setCellValue("Category");

            // Datos
            int rowNum = 1;
            for (Expense expense : expenses) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(expense.getAmount());
                row.createCell(1).setCellValue(expense.getDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
                row.createCell(2).setCellValue(expense.getDescription());
//                row.createCell(3).setCellValue(expense.getCategory());
            }

            // Ajustar tamaño de columnas
            for (int i = 0; i < 4; i++) {
                sheet.autoSizeColumn(i);
            }

            // Escribir a un stream
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);

            // No necesitamos referencias al workbook después de escribir
            return new ByteArrayInputStream(out.toByteArray());
        } // El workbook se cierra automáticamente aquí gracias a try-with-resources
    }
}