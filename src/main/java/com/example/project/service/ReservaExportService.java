package com.example.project.service;

import com.example.project.entity.Reserva;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class ReservaExportService {

    public void exportarReservasAExcel(List<Reserva> reservas, HttpServletResponse response) throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=reservas.xlsx");

        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Reservas");

        String[] columns = {"#", "Usuario", "Fecha", "Hora Inicio", "Hora Fin", "Monto", "Estado", "Tipo de Pago"};

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

        CreationHelper createHelper = workbook.getCreationHelper();
        CellStyle dateStyle = workbook.createCellStyle();
        dateStyle.setDataFormat(createHelper.createDataFormat().getFormat("dd-MM-yyyy"));
        CellStyle timeStyle = workbook.createCellStyle();
        timeStyle.setDataFormat(createHelper.createDataFormat().getFormat("HH:mm"));

        int rowIdx = 1;
        for (Reserva reserva : reservas) {
            Row row = sheet.createRow(rowIdx++);
            row.createCell(0).setCellValue(reserva.getIdReserva());
            row.createCell(1).setCellValue(reserva.getVecino().getNombres() + " " + reserva.getVecino().getApellidos());
            Cell fechaCell = row.createCell(2);
            fechaCell.setCellValue(reserva.getFecha());
            fechaCell.setCellStyle(dateStyle);
            Cell inicioCell = row.createCell(3);
            inicioCell.setCellValue(LocalDateTime.from(reserva.getHoraInicio()));
            inicioCell.setCellStyle(timeStyle);
            Cell finCell = row.createCell(4);
            finCell.setCellValue(LocalDateTime.from(reserva.getHoraFin()));
            finCell.setCellStyle(timeStyle);
            row.createCell(5).setCellValue(reserva.getCosto());
            row.createCell(6).setCellValue(reserva.getEstado().getEstado());
            row.createCell(7).setCellValue(reserva.getTipoPago());
        }

        for (int i = 0; i < columns.length; i++) {
            sheet.autoSizeColumn(i);
        }

        workbook.write(response.getOutputStream());
        workbook.close();
    }
}
