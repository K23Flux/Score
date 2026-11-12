package com.score.entity.bo;

import lombok.Data;

/**
 * 测试 AI 接口；字段为空时使用已保存的配置。
 */
@Data
public class AiTestBo {
    private String baseUrl;
    private String model;
    private String apiKey;
}
