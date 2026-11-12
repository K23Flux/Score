package com.score.service;

import com.score.entity.AppSetting;
import com.score.entity.bo.AiSettingsBo;
import com.score.mapper.AppSettingMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Date;

/**
 * 运行期设置：数据库 `app_setting` 优先，未写入时回退到环境变量。
 */
@Service
public class SettingsService {

    public static final String AI_ENABLED = "ai.enabled";
    public static final String AI_BASE_URL = "ai.baseUrl";
    public static final String AI_MODEL = "ai.model";
    public static final String AI_API_KEY = "ai.apiKey";

    @Value("${score.ai.enabled:false}")
    private boolean envEnabled;
    @Value("${score.ai.base-url:}")
    private String envBaseUrl;
    @Value("${score.ai.model:}")
    private String envModel;
    @Value("${score.ai.api-key:}")
    private String envApiKey;

    @Autowired
    private AppSettingMapper settingMapper;

    public AiSettings ai() {
        return new AiSettings(
                parseBoolean(get(AI_ENABLED, Boolean.toString(envEnabled))),
                trim(get(AI_BASE_URL, envBaseUrl)),
                trim(get(AI_MODEL, envModel)),
                trim(get(AI_API_KEY, envApiKey)));
    }

    public boolean isAiReady() {
        AiSettings settings = ai();
        return settings.enabled()
                && StringUtils.hasText(settings.baseUrl())
                && StringUtils.hasText(settings.model());
    }

    public void saveAi(AiSettingsBo bo) {
        if (bo == null) {
            throw new IllegalArgumentException("设置内容不能为空");
        }
        if (bo.getEnabled() != null) {
            put(AI_ENABLED, Boolean.toString(bo.getEnabled()));
        }
        if (bo.getBaseUrl() != null) {
            String baseUrl = bo.getBaseUrl().trim();
            if (StringUtils.hasText(baseUrl)
                    && !baseUrl.startsWith("http://")
                    && !baseUrl.startsWith("https://")) {
                throw new IllegalArgumentException("接口地址需以 http:// 或 https:// 开头");
            }
            put(AI_BASE_URL, baseUrl);
        }
        if (bo.getModel() != null) {
            put(AI_MODEL, bo.getModel().trim());
        }
        if (Boolean.TRUE.equals(bo.getClearApiKey())) {
            put(AI_API_KEY, "");
        } else if (StringUtils.hasText(bo.getApiKey())) {
            put(AI_API_KEY, bo.getApiKey().trim());
        }
    }

    private String get(String key, String fallback) {
        AppSetting setting = settingMapper.selectById(key);
        if (setting == null) {
            return fallback == null ? "" : fallback;
        }
        return setting.getSettingValue() == null ? "" : setting.getSettingValue();
    }

    private void put(String key, String value) {
        AppSetting existing = settingMapper.selectById(key);
        AppSetting setting = new AppSetting();
        setting.setSettingKey(key);
        setting.setSettingValue(value == null ? "" : value);
        setting.setUpdatedAt(new Date());
        if (existing == null) {
            settingMapper.insert(setting);
        } else {
            settingMapper.updateById(setting);
        }
    }

    private boolean parseBoolean(String value) {
        return "true".equalsIgnoreCase(value == null ? "" : value.trim());
    }

    private String trim(String value) {
        return value == null ? "" : value.trim();
    }
}
