package com.score.entity.vo;

import lombok.Data;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 导入预览中的一行。
 */
@Data
public class ImportPreviewRowVo {
    /** Excel 中的行号（从 1 开始） */
    private Integer rowNumber;
    private String studentName;
    /** 脱敏后的身份证号 */
    private String idCard;
    private String className;
    /** 科目 -> 原始分数文本 */
    private Map<String, String> subjects = new LinkedHashMap<>();
    private boolean valid;
    private String message;
}
