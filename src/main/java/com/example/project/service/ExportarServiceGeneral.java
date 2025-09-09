package com.example.project.service;

import com.example.project.entity.*;
import com.example.project.dto.ExportDataDTO;
import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfWriter;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.function.Function;
import java.awt.Color;

@Service
public class ExportarServiceGeneral {

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd/MM/yyyy");
    private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("#0.00");

    /**
     * Método genérico para exportar cualquier tipo de datos a PDF
     */
    public <T> void exportarAPdf(List<T> datos, ExportDataDTO<T> exportConfig,
                                 HttpServletResponse response) throws IOException {
        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition",
                "attachment; filename=" + exportConfig.getFileName() + ".pdf");

        Document document = new Document(exportConfig.isLandscape() ?
                PageSize.A4.rotate() : PageSize.A4);
        PdfWriter.getInstance(document, response.getOutputStream());
        document.open();

        // Título
        Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, Color.BLACK);
        Paragraph title = new Paragraph(exportConfig.getTitle(), titleFont);
        title.setAlignment(Element.ALIGN_CENTER);
        document.add(title);
        document.add(new Paragraph(" "));

        // Tabla
        PdfPTable table = new PdfPTable(exportConfig.getHeaders().length);
        table.setWidthPercentage(100);

        if (exportConfig.getColumnWidths() != null) {
            table.setWidths(exportConfig.getColumnWidths());
        }

        // Headers
        for (String header : exportConfig.getHeaders()) {
            agregarCeldaEncabezado(table, header);
        }

        // Datos
        for (T item : datos) {
            for (Function<T, String> extractor : exportConfig.getDataExtractors()) {
                table.addCell(extractor.apply(item));
            }
        }

        document.add(table);
        document.close();
    }

    /**
     * Método genérico para exportar cualquier tipo de datos a Excel
     */
    public <T> void exportarAXlsx(List<T> datos, ExportDataDTO<T> exportConfig,
                                  HttpServletResponse response) throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition",
                "attachment; filename=" + exportConfig.getFileName() + ".xlsx");

        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet(exportConfig.getSheetName());

        // Headers
        Row header = sheet.createRow(0);
        String[] headers = exportConfig.getHeaders();
        for (int i = 0; i < headers.length; i++) {
            Cell cell = header.createCell(i);
            cell.setCellValue(headers[i]);
        }

        // Datos
        int rowIdx = 1;
        for (T item : datos) {
            Row row = sheet.createRow(rowIdx++);
            int cellIdx = 0;
            for (Function<T, String> extractor : exportConfig.getDataExtractors()) {
                row.createCell(cellIdx++).setCellValue(extractor.apply(item));
            }
        }

        // Auto-ajustar columnas
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }

        workbook.write(response.getOutputStream());
        workbook.close();
    }

    private void agregarCeldaEncabezado(PdfPTable table, String texto) {
        PdfPCell cell = new PdfPCell();
        cell.setBackgroundColor(new Color(192, 192, 192));
        cell.setPadding(5);
        Font font = FontFactory.getFont(FontFactory.HELVETICA_BOLD);
        cell.setPhrase(new Phrase(texto, font));
        table.addCell(cell);
    }

    // Métodos específicos para mantener compatibilidad (si los necesitas)
    public void exportarEspaciosPdf(List<Espacio> espacios, HttpServletResponse response) throws IOException {
        ExportDataDTO<Espacio> config = ExportDataDTO.<Espacio>builder()
                .title("Lista de Espacios Deportivos")
                .fileName("espacios_deportivos")
                .sheetName("Espacios")
                .headers(new String[]{"Nombre", "Tipo", "Lugar", "Costo", "Estado"})
                .columnWidths(new float[]{3, 3, 3, 2, 2})
                .landscape(true)
                .dataExtractors(List.of(
                        esp -> esp.getNombre(),
                        esp -> esp.getTipoEspacio().getNombre(),
                        esp -> esp.getIdLugar().getLugar(),
                        esp -> "S/ " + String.format("%.2f", esp.getCosto()),
                        esp -> esp.getIdEstadoEspacio().getEstado()
                ))
                .build();

        exportarAPdf(espacios, config, response);
    }


    public void exportarEspaciosXlsx(List<Espacio> espacios, HttpServletResponse response) throws IOException {
        ExportDataDTO<Espacio> config = ExportDataDTO.<Espacio>builder()
                .title("Lista de Espacios Deportivos")
                .fileName("espacios_deportivos")
                .sheetName("Espacios")
                .headers(new String[]{"Nombre", "Tipo", "Lugar", "Costo", "Estado"})
                .dataExtractors(List.of(
                        esp -> esp.getNombre(),
                        esp -> esp.getTipoEspacio().getNombre(),
                        esp -> esp.getIdLugar().getLugar(),
                        esp -> String.valueOf(esp.getCosto()),
                        esp -> esp.getIdEstadoEspacio().getEstado()
                ))
                .build();

        exportarAXlsx(espacios, config, response);
    }
}