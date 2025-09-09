package com.example.project.service;

import com.example.project.entity.Pago;
import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.usermodel.Cell;
import com.lowagie.text.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.stream.Stream;

@Service
public class PagoExportService {

    // Exportar como PDF
    public void exportarPagosAPdf(List<Pago> pagos, HttpServletResponse response) throws IOException {
        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "attachment; filename=historial_pagos.pdf");

        Document document = new Document(PageSize.A4.rotate());
        PdfWriter.getInstance(document, response.getOutputStream());

        document.open();

        Font fontTitle = new Font(Font.HELVETICA, 20, Font.BOLD, Color.DARK_GRAY);
        Paragraph title = new Paragraph("Historial de Pagos", fontTitle);
        title.setAlignment(Paragraph.ALIGN_CENTER);


        document.add(title);
        document.add(Chunk.NEWLINE);

        PdfPTable table = new PdfPTable(7); // 7 columnas
        table.setWidthPercentage(100);
        table.setWidths(new int[]{1, 2, 3, 3, 2, 2, 3});

        addTableHeaderPdf(table);
        addTableDataPdf(table, pagos);

        document.add(table);
        document.close();
    }

    private void addTableHeaderPdf(PdfPTable table) {
        Stream.of("#", "Espacio", "Fecha de Reserva", "Horario", "Tipo de Pago", "Monto (S/)", "Fecha de Pago")
                .forEach(columnTitle -> {
                    PdfPCell header = new PdfPCell();
                    header.setBackgroundColor(new Color(79, 129, 189));
                    header.setBorderColor(Color.BLACK);
                    header.setPhrase(new Phrase(columnTitle, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, Color.BLACK)));
                    table.addCell(header);
                });
    }

    private void addTableDataPdf(PdfPTable table, List<Pago> pagos) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy HH:mm");
        for (Pago pago : pagos) {
            table.addCell(String.valueOf(pago.getIdPago()));
            table.addCell(pago.getReserva().getEspacio().getNombre());
            table.addCell(pago.getReserva().getFecha().toString());
            table.addCell(pago.getReserva().getHoraInicio() + " - " + pago.getReserva().getHoraFin());
            table.addCell(pago.getTipoPago());
            table.addCell(String.format("%.2f", pago.getMonto()));
            // Manejo mejorado de la fecha de pago
            String fechaPago = "No disponible";
            if (pago.getFechaPago() != null) {
                try {
                    // Asumiendo que getFechaPago() retorna un LocalDateTime
                    fechaPago = pago.getFechaPago().format(java.time.format.DateTimeFormatter
                            .ofPattern("dd-MM-yyyy HH:mm"));
                } catch (Exception e) {
                    try {
                        // Intenta parsear como String si el anterior falla
                        fechaPago = pago.getFechaPago().toString();
                    } catch (Exception ex) {
                        fechaPago = "Formato no válido";
                    }
                }
            }
            table.addCell(fechaPago);
            table.completeRow();
        }
    }

    // Exportar como Excel
    public void exportarPagosAExcel(List<Pago> pagos, HttpServletResponse response) throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=historial_pagos.xlsx");

        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Historial de Pagos");

        String[] columns = {"#", "Espacio", "Fecha de Reserva", "Horario", "Tipo de Pago", "Monto (S/)", "Estado", "Fecha de Pago"};

        // Estilo de encabezado
        CellStyle headerStyle = workbook.createCellStyle();
        headerStyle.setFillForegroundColor(IndexedColors.LIGHT_CORNFLOWER_BLUE.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        org.apache.poi.ss.usermodel.Font font = workbook.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.BLACK.getIndex());
        headerStyle.setFont(font);

        Row headerRow = sheet.createRow(0);
        for (int i = 0; i < columns.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(columns[i]);
            cell.setCellStyle(headerStyle);
        }

        // Estilo de fecha
        CreationHelper createHelper = workbook.getCreationHelper();
        CellStyle dateStyle = workbook.createCellStyle();
        dateStyle.setDataFormat(createHelper.createDataFormat().getFormat("dd-MM-yyyy"));
        CellStyle dateTimeStyle = workbook.createCellStyle();
        dateTimeStyle.setDataFormat(createHelper.createDataFormat().getFormat("dd-MM-yyyy HH:mm"));

        CellStyle timeStyle = workbook.createCellStyle();
        timeStyle.setDataFormat(createHelper.createDataFormat().getFormat("HH:mm"));

        // Datos
        int rowNum = 1;
        for (Pago pago : pagos) {
            Row row = sheet.createRow(rowNum++);

            row.createCell(0).setCellValue(pago.getIdPago());
            row.createCell(1).setCellValue(pago.getReserva().getEspacio().getNombre());

            // Fecha de reserva
            Cell fechaCell = row.createCell(2);
            fechaCell.setCellValue(pago.getReserva().getFecha());
            fechaCell.setCellStyle(dateStyle);

            // Horario
            row.createCell(3).setCellValue(pago.getReserva().getHoraInicio().toString().substring(0,5) + " - " +
                    pago.getReserva().getHoraFin().toString().substring(0,5));

            // Tipo de pago
            row.createCell(4).setCellValue(pago.getTipoPago());

            // Monto
            row.createCell(5).setCellValue(pago.getMonto().doubleValue());

            // Estado
            row.createCell(6).setCellValue(pago.getEstado());

            // Fecha de pago
            Cell fechaPagoCell = row.createCell(7);
            fechaPagoCell.setCellValue(pago.getFechaPago());
            fechaPagoCell.setCellStyle(dateTimeStyle);

        }

        // Ajustar columnas
        for (int i = 0; i < columns.length; i++) {
            sheet.autoSizeColumn(i);
        }

        // Filtros
        sheet.setAutoFilter(new org.apache.poi.ss.util.CellRangeAddress(0, 0, 0, columns.length - 1));

        workbook.write(response.getOutputStream());
        workbook.close();
    }
}
