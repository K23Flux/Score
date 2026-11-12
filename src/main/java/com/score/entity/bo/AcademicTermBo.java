package com.score.entity.bo;

import lombok.Data;

/**
 * 新建学期的请求体。
 */
@Data
public class AcademicTermBo {
    /** 例如 2026-2027 */
    private String academicYear;
    /** 1=第一学期，2=第二学期 */
    private String semester;
    /** 创建后设为当前学期 */
    private Boolean current;
}
