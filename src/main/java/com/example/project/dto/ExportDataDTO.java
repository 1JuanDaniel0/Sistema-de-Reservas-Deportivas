package com.example.project.dto;

import lombok.Builder;
import lombok.Data;
import java.util.List;
import java.util.function.Function;

@Data
@Builder
public class ExportDataDTO<T> {
    private String title;
    private String fileName;
    private String sheetName;
    private String[] headers;
    private float[] columnWidths;
    private boolean landscape;
    private List<Function<T, String>> dataExtractors;
}