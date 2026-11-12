package com.score.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * GitHub OAuth：生成授权地址、用 code 换取用户身份。
 * 未配置 GITHUB_CLIENT_ID / GITHUB_CLIENT_SECRET 时视为未启用。
 */
@Service
public class GithubOAuthService {

    private static final String AUTHORIZE_URL = "https://github.com/login/oauth/authorize";
    private static final String TOKEN_URL = "https://github.com/login/oauth/access_token";
    private static final String USER_URL = "https://api.github.com/user";

    @Value("${score.github.redirect-uri:}")
    private String redirectUri;

    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private SettingsService settingsService;

    public String getRedirectUri() {
        return redirectUri;
    }

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    public boolean isEnabled() {
        return settingsService.github().enabled() && StringUtils.hasText(redirectUri);
    }

    public String authorizeUrl(String state) {
        return AUTHORIZE_URL
                + "?client_id=" + encode(settingsService.github().clientId())
                + "&redirect_uri=" + encode(redirectUri)
                + "&scope=" + encode("read:user")
                + "&state=" + encode(state);
    }

    public GithubUser exchange(String code) throws Exception {
        SettingsService.GithubConfig config = settingsService.github();
        ObjectNode body = objectMapper.createObjectNode();
        body.put("client_id", config.clientId());
        body.put("client_secret", config.clientSecret());
        body.put("code", code);
        body.put("redirect_uri", redirectUri);

        HttpRequest tokenRequest = HttpRequest.newBuilder(URI.create(TOKEN_URL))
                .timeout(Duration.ofSeconds(10))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .header("User-Agent", "ScoreCheck")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body), StandardCharsets.UTF_8))
                .build();
        JsonNode tokenJson = objectMapper.readTree(send(tokenRequest));
        String accessToken = tokenJson.path("access_token").asText("");
        if (!StringUtils.hasText(accessToken)) {
            throw new IllegalStateException("GitHub 授权失败：" + tokenJson.path("error_description").asText("无 access_token"));
        }

        HttpRequest userRequest = HttpRequest.newBuilder(URI.create(USER_URL))
                .timeout(Duration.ofSeconds(10))
                .header("Authorization", "Bearer " + accessToken)
                .header("Accept", "application/vnd.github+json")
                .header("User-Agent", "ScoreCheck")
                .GET()
                .build();
        JsonNode userJson = objectMapper.readTree(send(userRequest));
        long id = userJson.path("id").asLong(0);
        String login = userJson.path("login").asText("");
        if (id <= 0 || !StringUtils.hasText(login)) {
            throw new IllegalStateException("GitHub 用户信息不完整");
        }
        return new GithubUser(id, login);
    }

    private String send(HttpRequest request) throws Exception {
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() / 100 != 2) {
            throw new IllegalStateException("GitHub 接口返回 HTTP " + response.statusCode());
        }
        return response.body();
    }

    private String encode(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
    }

    public record GithubUser(long id, String login) {
    }
}
