package com.score.service;

/**
 * AI 表头识别的有效配置（数据库优先，其次环境变量）。
 */
public record AiSettings(boolean enabled, String baseUrl, String model, String apiKey) {
}
