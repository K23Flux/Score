package com.score.service;

/**
 * AI 接口连通性测试结果。
 */
public record AiTestResult(boolean ok, long latencyMs, String reply, String error) {
}
