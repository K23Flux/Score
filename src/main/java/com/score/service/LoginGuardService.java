package com.score.service;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 登录失败追踪：连续失败后才要求滑块验证，并按 IP 限制尝试频率。
 *
 * 状态保存在应用内存，重启后清零；当前为单实例部署，不依赖 Redis。
 */
@Service
public class LoginGuardService {

    private static final int CAPTCHA_THRESHOLD = 3;
    private static final long FAILURE_WINDOW_MS = 15 * 60 * 1000L;
    private static final int IP_ATTEMPT_LIMIT = 60;
    private static final long IP_WINDOW_MS = 5 * 60 * 1000L;

    private final Map<String, Deque<Long>> failuresByUser = new ConcurrentHashMap<>();
    private final Map<String, Deque<Long>> attemptsByIp = new ConcurrentHashMap<>();

    public boolean isCaptchaRequired(String userName, String ip) {
        Deque<Long> failures = failuresByUser.get(userKey(userName, ip));
        if (failures == null) {
            return false;
        }
        synchronized (failures) {
            prune(failures, FAILURE_WINDOW_MS);
            return failures.size() >= CAPTCHA_THRESHOLD;
        }
    }

    public boolean isRateLimited(String ip) {
        Deque<Long> attempts = attemptsByIp.get(ip);
        if (attempts == null) {
            return false;
        }
        synchronized (attempts) {
            prune(attempts, IP_WINDOW_MS);
            return attempts.size() >= IP_ATTEMPT_LIMIT;
        }
    }

    public void recordAttempt(String ip) {
        if (!StringUtils.hasText(ip)) {
            return;
        }
        Deque<Long> attempts = attemptsByIp.computeIfAbsent(ip, key -> new ArrayDeque<>());
        synchronized (attempts) {
            prune(attempts, IP_WINDOW_MS);
            attempts.addLast(System.currentTimeMillis());
        }
    }

    public void recordFailure(String userName, String ip) {
        String key = userKey(userName, ip);
        Deque<Long> failures = failuresByUser.computeIfAbsent(key, k -> new ArrayDeque<>());
        synchronized (failures) {
            prune(failures, FAILURE_WINDOW_MS);
            failures.addLast(System.currentTimeMillis());
        }
    }

    public void recordSuccess(String userName, String ip) {
        failuresByUser.remove(userKey(userName, ip));
    }

    private String userKey(String userName, String ip) {
        String name = userName == null ? "" : userName.trim().toLowerCase(Locale.ROOT);
        return name + "|" + (ip == null ? "" : ip);
    }

    private void prune(Deque<Long> timestamps, long windowMs) {
        long threshold = System.currentTimeMillis() - windowMs;
        while (!timestamps.isEmpty() && timestamps.peekFirst() < threshold) {
            timestamps.pollFirst();
        }
    }
}
