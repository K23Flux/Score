package com.score.entity.vo;

import lombok.Data;

/**
 * 导入文件中的一列。
 */
@Data
public class ImportColumnVo {
    private Integer index;
    private String header;
    /** 脱敏后的样例值，仅用于确认列含义 */
    private String sample;
}
