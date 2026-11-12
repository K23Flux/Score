package com.score.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.score.entity.vo.ImportMappingVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 可选的 AI 表头识别。
 *
 * 只发送表头和脱敏样例，不发送姓名、身份证等原始数据。
 * 未配置或调用失败时由调用方回退到规则识别。
 */
@Slf4j
@Service
public class AiColumnMapper {

    @Value("${score.ai.timeout-seconds:8}")
    private int timeoutSeconds;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SettingsService settingsService;

    /** 每次按数据库设置判断，管理页修改后立即生效。 */
    public boolean isEnabled() {
        return settingsService.isAiReady();
    }

    /**
     * 让模型根据表头和脱敏样例建议列映射。
     * 返回的映射仍需用户在页面上确认。
     */
    public Optional<ImportMappingVo> suggest(ImportType type, List<String> headers, List<List<String>> samples) {
        if (!isEnabled() || headers == null || headers.isEmpty()) {
            return Optional.empty();
        }
        try {
            String content = call(type, headers, samples);
            return parse(content, headers, type);
        } catch (Exception e) {
            log.warn("AI 列映射失败，回退到规则识别：{}", e.getMessage());
            return Optional.empty();
        }
    }

    private String call(ImportType type, List<String> headers, List<List<String>> samples) throws Exception {
        AiSettings ai = settingsService.ai();
        return request(ai.baseUrl(), ai.apiKey(), ai.model(),
                "你是成绩表结构识别助手。只返回一个 JSON 对象，不要 Markdown，不要解释。",
                buildPrompt(type, headers, samples), null);
    }

    /**
     * 设置页的“测试”按钮：用当前表单值或已保存配置发一次最小请求。
     */
    public AiTestResult test(String baseUrlOverride, String modelOverride, String apiKeyOverride) {
        AiSettings saved = settingsService.ai();
        String baseUrl = StringUtils.hasText(baseUrlOverride) ? baseUrlOverride.trim() : saved.baseUrl();
        String model = StringUtils.hasText(modelOverride) ? modelOverride.trim() : saved.model();
        String apiKey = StringUtils.hasText(apiKeyOverride) ? apiKeyOverride.trim() : saved.apiKey();
        if (!StringUtils.hasText(baseUrl) || !StringUtils.hasText(model)) {
            return new AiTestResult(false, 0, "", "请先填写接口地址和模型名称");
        }
        long start = System.currentTimeMillis();
        try {
            String reply = request(baseUrl, apiKey, model,
                    "你是连接测试助手。", "请只回复 OK", 16);
            return new AiTestResult(true, System.currentTimeMillis() - start, reply, null);
        } catch (Exception e) {
            String message = StringUtils.hasText(e.getMessage())
                    ? e.getMessage()
                    : e.getClass().getSimpleName();
            return new AiTestResult(false, System.currentTimeMillis() - start, "", message);
        }
    }

    private String request(String baseUrl, String apiKey, String model,
                           String systemPrompt, String userPrompt, Integer maxTokens) throws Exception {
        ObjectNode body = objectMapper.createObjectNode();
        body.put("model", model);
        body.put("temperature", 0);
        if (maxTokens != null) {
            body.put("max_tokens", maxTokens);
        }

        ArrayNode messages = body.putArray("messages");
        ObjectNode system = messages.addObject();
        system.put("role", "system");
        system.put("content", systemPrompt);

        ObjectNode user = messages.addObject();
        user.put("role", "user");
        user.put("content", userPrompt);

        String url = baseUrl.replaceAll("/+$", "") + "/chat/completions";
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(Math.max(2, timeoutSeconds)))
                .header("Content-Type", "application/json");
        if (StringUtils.hasText(apiKey)) {
            builder.header("Authorization", "Bearer " + apiKey);
        }
        HttpRequest request = builder
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body), StandardCharsets.UTF_8))
                .build();

        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(3))
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() / 100 != 2) {
            throw new IllegalStateException("HTTP " + response.statusCode());
        }
        JsonNode root = objectMapper.readTree(response.body());
        return root.path("choices").path(0).path("message").path("content").asText("");
    }

    private String buildPrompt(ImportType type, List<String> headers, List<List<String>> samples) throws Exception {
        String headersJson = objectMapper.writeValueAsString(headers);
        String samplesJson = objectMapper.writeValueAsString(samples);
        String target = type == ImportType.STUDENT
                ? "这是学生信息表，只需要姓名列和身份证列。"
                : "这是成绩表，除姓名、身份证、班级、序号、学号、总分、总成绩、备注、排名、名次、年级、性别、联系方式外，其余数据列都视为科目。";
        return "根据表头和脱敏样例判断各列用途。\n"
                + target + "\n"
                + "只返回 JSON："
                + "{\"nameColumn\":\"列名\",\"idColumn\":\"列名或null\","
                + "\"subjectColumns\":[\"列名\"],\"ignoredColumns\":[\"列名\"]}\n"
                + "要求：列名必须与表头完全一致；姓名列必填；身份证列可选；学生表不需要科目列。\n"
                + "表头：" + headersJson + "\n"
                + "脱敏样例：" + samplesJson;
    }

    private Optional<ImportMappingVo> parse(String content, List<String> headers, ImportType type) throws Exception {
        if (!StringUtils.hasText(content)) {
            return Optional.empty();
        }
        String json = content.trim();
        int start = json.indexOf('{');
        int end = json.lastIndexOf('}');
        if (start < 0 || end <= start) {
            return Optional.empty();
        }
        JsonNode node = objectMapper.readTree(json.substring(start, end + 1));

        ImportMappingVo mapping = new ImportMappingVo();
        mapping.setSubjectColumns(new ArrayList<>());
        mapping.setIgnoredColumns(new ArrayList<>());
        mapping.setNameColumn(indexOf(headers, text(node, "nameColumn")));
        mapping.setIdColumn(indexOf(headers, text(node, "idColumn")));

        JsonNode subjects = node.get("subjectColumns");
        if (subjects != null && subjects.isArray()) {
            for (JsonNode subject : subjects) {
                Integer index = indexOf(headers, subject.asText(""));
                if (index != null
                        && !index.equals(mapping.getNameColumn())
                        && !index.equals(mapping.getIdColumn())
                        && !mapping.getSubjectColumns().contains(index)) {
                    mapping.getSubjectColumns().add(index);
                }
            }
        }

        if (mapping.getNameColumn() == null) {
            return Optional.empty();
        }
        if (type == ImportType.STUDENT) {
            mapping.setSubjectColumns(new ArrayList<>());
            if (mapping.getIdColumn() == null) {
                return Optional.empty();
            }
        } else if (mapping.getSubjectColumns().isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(mapping);
    }

    private String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || value.isNull()) {
            return null;
        }
        String text = value.asText("").trim();
        return "null".equalsIgnoreCase(text) ? null : text;
    }

    private Integer indexOf(List<String> headers, String header) {
        if (!StringUtils.hasText(header)) {
            return null;
        }
        for (int i = 0; i < headers.size(); i++) {
            if (header.equals(headers.get(i))) {
                return i;
            }
        }
        return null;
    }
}
