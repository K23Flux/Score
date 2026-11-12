package com.score.entity.bo;

import lombok.Data;

/**
 * 更新 AI 表头识别设置的请求体。字段为 null 表示不修改。
 */
@Data
public class AiSettingsBo {
    private Boolean enabled;
    private String baseUrl;
    private String model;
    /** 新密钥；为空且 clearApiKey=false 时保持原值 */
    private String apiKey;
    private Boolean clearApiKey;
}
