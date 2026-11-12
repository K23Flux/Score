package com.score.entity.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 导入预览结果。
 */
@Data
public class ImportPreviewVo {
    private List<ImportColumnVo> columns = new ArrayList<>();
    private ImportMappingVo mapping;
    /** rule / ai / manual */
    private String mappingSource;
    private boolean aiEnabled;
    private List<ImportPreviewRowVo> rows = new ArrayList<>();
    private List<ImportRowErrorVo> errors = new ArrayList<>();
    private List<String> headerErrors = new ArrayList<>();
    private int totalRows;
    private int validRows;
    private int invalidRows;
}
