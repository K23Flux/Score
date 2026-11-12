package com.score.entity.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 表头列到导入字段的映射，使用列索引。
 */
@Data
public class ImportMappingVo {
    private Integer nameColumn;
    private Integer idColumn;
    private List<Integer> subjectColumns = new ArrayList<>();
    private List<Integer> ignoredColumns = new ArrayList<>();
}
