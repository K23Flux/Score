package com.score.entity.vo;

import lombok.Data;

/**
 * 导入提交结果。
 */
@Data
public class ImportResultVo {
    private int inserted;
    private int updated;
    private int skipped;
    private String message;
}
