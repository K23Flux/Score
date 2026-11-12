package com.score.entity.bo;

import lombok.Data;

/**
 * 编辑学生联系方式。字段为 null 表示不修改，空字符串表示清空。
 */
@Data
public class StudentContactBo {
    private Long id;
    private String phone;
    private String parentName;
    private String parentPhone;
    private String parentRelation;
}
