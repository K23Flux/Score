package com.score.controller;

import com.score.common.ResultVo;
import com.score.entity.bo.AcademicTermBo;
import com.score.entity.bo.AiSettingsBo;
import com.score.service.AdminGuardService;
import com.score.service.AiSettings;
import com.score.service.SettingsService;
import com.score.service.TermService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 管理员设置：AI 表头识别、学年学期管理。仅 admin 角色可访问。
 */
@RestController
@RequestMapping("/scoreCheck/settings")
public class SettingsController {

    @Autowired
    private SettingsService settingsService;
    @Autowired
    private TermService termService;
    @Autowired
    private AdminGuardService adminGuardService;

    @GetMapping("/ai")
    public ResultVo<?> getAiSettings() {
        adminGuardService.requireAdmin();
        AiSettings settings = settingsService.ai();
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("enabled", settings.enabled());
        data.put("baseUrl", settings.baseUrl());
        data.put("model", settings.model());
        data.put("apiKeySet", StringUtils.hasText(settings.apiKey()));
        data.put("apiKeyMasked", mask(settings.apiKey()));
        data.put("ready", settingsService.isAiReady());
        return ResultVo.success(data);
    }

    @PutMapping("/ai")
    public ResultVo<?> updateAiSettings(@RequestBody AiSettingsBo bo) {
        adminGuardService.requireAdmin();
        settingsService.saveAi(bo);
        return ResultVo.success("保存成功");
    }

    @PostMapping("/terms")
    public ResultVo<?> createTerm(@RequestBody AcademicTermBo bo) {
        adminGuardService.requireAdmin();
        if (bo == null) {
            throw new IllegalArgumentException("学期信息不能为空");
        }
        return ResultVo.success(termService.create(
                bo.getAcademicYear(), bo.getSemester(), Boolean.TRUE.equals(bo.getCurrent())));
    }

    @PostMapping("/terms/{id}/current")
    public ResultVo<?> setCurrentTerm(@PathVariable Long id) {
        adminGuardService.requireAdmin();
        termService.setCurrent(id);
        return ResultVo.success("已设为当前学期");
    }

    private String mask(String apiKey) {
        if (!StringUtils.hasText(apiKey)) {
            return "";
        }
        String key = apiKey.trim();
        if (key.length() <= 4) {
            return "****";
        }
        return "****" + key.substring(key.length() - 4);
    }
}
