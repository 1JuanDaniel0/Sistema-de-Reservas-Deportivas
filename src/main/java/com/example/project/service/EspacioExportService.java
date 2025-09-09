package com.example.project.service;

import com.example.project.repository.admin.EspacioDto;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;

@Service
public class EspacioExportService {
    public void exportarEspaciosAExcel(List<EspacioDto> espacios, HttpServletResponse response) throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=espacios.xlsx");

        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Espacios");

        String[] columns = {"ID", "Nombre", "Tipo", "Lugar", "Costo", "Estado"};

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
        for (EspacioDto espacio : espacios) {
            Row row = sheet.createRow(rowIdx++);
            row.createCell(0).setCellValue(espacio.getIdEspacio());
            row.createCell(1).setCellValue(espacio.getNombre());
            row.createCell(2).setCellValue(espacio.getNombreTipo() != null ? espacio.getNombreTipo() : espacio.getTipo());
            row.createCell(3).setCellValue(espacio.getNombreLugar());
            row.createCell(4).setCellValue(espacio.getCosto());
            row.createCell(5).setCellValue(espacio.getEstadoEspacio());
        }

        for (int i = 0; i < columns.length; i++) {
            sheet.autoSizeColumn(i);
        }

        workbook.write(response.getOutputStream());
        workbook.close();
    }
}
