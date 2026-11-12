package com.score.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;

/**
 * 学年学期。
 */
@Data
@TableName("academic_term")
public class AcademicTerm {
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 学年，例如 2026-2027 */
    private String academicYear;

    /** 学期：1=第一学期，2=第二学期 */
    private String semester;

    /** 是否当前学期（字段名避开保留字 current） */
    @TableField("is_current")
    private Boolean isCurrent;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;

    private String delFlag;
}
