package com.score.service;

/**
 * 学期上下文：学年 + 学期，用于学生、成绩的归属。
 */
public record TermInfo(String academicYear, String semester) {
}
