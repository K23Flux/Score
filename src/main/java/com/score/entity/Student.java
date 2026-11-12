package com.score.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;

/**
 * @author imyiwen
 * @data 2026/4/21 17:03
 */
@Data
@TableName("student")
public class Student {
    @TableId(type= IdType.AUTO)
    private Long id;

    /**
     * 姓名
     */
    private String studentName;

    /**
     * 班级
     */
    private String className;

    /**
     * 身份证号
     */
    private String idCard;

    /** 学生手机号 */
    private String phone;

    /** 家长姓名 */
    private String parentName;

    /** 家长手机号 */
    private String parentPhone;

    /** 与学生的关系，如 父亲 / 母亲 / 其他 */
    private String parentRelation;

    /** 学年，例如 2026-2027 */
    private String academicYear;

    /** 学期：1=第一学期，2=第二学期 */
    private String semester;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;
    /**
     * 删除标识（0:正常、1:删除）
     */
    private String delFlag;
}
