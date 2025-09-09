package com.example.project.service;

import com.example.project.repository.admin.ListaCoordinadoresDto;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;

@Service
public class CoordinadorExportService {
    public void exportarCoordinadoresAExcel(List<ListaCoordinadoresDto> coordinadores, HttpServletResponse response) throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=coordinadores.xlsx");

        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Coordinadores");

        String[] columns = {"ID", "Nombre", "Apellido", "DNI", "Email", "Contraseña", "Estado"};

        CellStyle headerStyle = workbook.createCellStyle();
        headerStyle.setFillForegroundColor(IndexedColors.LIGHT_CORNFLOWER_BLUE.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerStyle.setFont(headerFont);

        Row headerRow = sheet.createRow(0);
        for (int i = 0; i < columns.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(columns[i]);
            cell.setCellStyle(headerStyle);
        }

        int rowIdx = 1;
        for (ListaCoordinadoresDto c : coordinadores) {
            Row row = sheet.createRow(rowIdx++);
            row.createCell(0).setCellValue(c.getIdUsuarios());
            row.createCell(1).setCellValue(c.getNombres());
            row.createCell(2).setCellValue(c.getApellidos());
            row.createCell(3).setCellValue(c.getDni());
            row.createCell(4).setCellValue(c.getCorreo());
            row.createCell(5).setCellValue(c.getContrasena());
            row.createCell(6).setCellValue(c.getEstado());
        }

        for (int i = 0; i < columns.length; i++) {
            sheet.autoSizeColumn(i);
        }

        workbook.write(response.getOutputStream());
        workbook.close();
    }
}
