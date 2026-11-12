package com.score.entity.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 导入校验错误。
 */
@Data
@AllArgsConstructor
public class ImportRowErrorVo {
    private Integer rowNumber;
    private String message;
}
