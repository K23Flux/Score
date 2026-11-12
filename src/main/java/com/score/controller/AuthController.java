package com.score.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.score.common.ResultVo;
import com.score.entity.Admin;
import com.score.entity.LoginTicket;
import com.score.entity.OAuthState;
import com.score.mapper.AdminMapper;
import com.score.service.AdminGuardService;
import com.score.service.GithubOAuthService;
import com.score.service.OAuthStoreService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * GitHub OAuth 登录。管理员先在设置页绑定 GitHub，之后可在登录页用 GitHub 登录。
 */
@Slf4j
@RestController
@RequestMapping("/scoreCheck/auth")
public class AuthController {

    @Value("${score.public-base-url:https://score.ozoo.top}")
    private String publicBaseUrl;

    @Autowired
    private GithubOAuthService githubOAuthService;
    @Autowired
    private OAuthStoreService oauthStoreService;
    @Autowired
    private AdminMapper adminMapper;

    @GetMapping("/github/status")
    public ResultVo<?> githubStatus() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("enabled", githubOAuthService.isEnabled());
        return ResultVo.success(data);
    }

    /** 登录页发起 GitHub 授权，返回要跳转的地址。 */
    @GetMapping("/github/authorize")
    public ResultVo<?> githubAuthorize() {
        if (!githubOAuthService.isEnabled()) {
            return ResultVo.error("GitHub 登录尚未配置");
        }
        OAuthState state = oauthStoreService.createState("login", null);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("url", githubOAuthService.authorizeUrl(state.getState()));
        return ResultVo.success(data);
    }

    /** GitHub 回调：登录或绑定，处理完重定向前端。 */
    @GetMapping("/github/callback")
    public void githubCallback(@RequestParam(value = "code", required = false) String code,
                               @RequestParam(value = "state", required = false) String state,
                               HttpServletResponse response) throws IOException {
        String mode = "login";
        try {
            if (!StringUtils.hasText(code)) {
                throw new IllegalArgumentException("缺少 code");
            }
            OAuthState stored = oauthStoreService.consumeState(state);
            mode = stored.getMode();
            GithubOAuthService.GithubUser githubUser = githubOAuthService.exchange(code);

            if ("bind".equals(mode)) {
                bind(stored, githubUser);
                response.sendRedirect(publicBaseUrl + "/score/teacher/settings?github=bound");
                return;
            }

            Admin admin = adminMapper.selectOne(new LambdaQueryWrapper<Admin>()
                    .eq(Admin::getGithubId, githubUser.id())
                    .eq(Admin::getDelFlag, "0")
                    .last("limit 1"));
            if (admin == null) {
                response.sendRedirect(publicBaseUrl + "/score/login?githubError=not_bound");
                return;
            }
            StpUtil.login(admin.getUserName());
            String ticket = oauthStoreService.issueTicket(StpUtil.getTokenValue());
            response.sendRedirect(publicBaseUrl + "/score/login?ticket=" + ticket);
        } catch (Exception e) {
            log.warn("GitHub 授权失败：{}", e.getMessage());
            String target = "bind".equals(mode)
                    ? "/score/teacher/settings?github=error"
                    : "/score/login?githubError=failed";
            response.sendRedirect(publicBaseUrl + target);
        }
    }

    /** 前端用一次性票据换取登录态。 */
    @PostMapping("/ticket")
    public ResultVo<?> exchangeTicket(@RequestBody Map<String, String> body) {
        try {
            LoginTicket ticket = oauthStoreService.consumeTicket(body == null ? null : body.get("ticket"));
            Object loginId = StpUtil.getLoginIdByToken(ticket.getToken());
            if (loginId == null) {
                return ResultVo.error("登录已失效，请重试");
            }
            Admin admin = adminMapper.selectOne(new LambdaQueryWrapper<Admin>()
                    .eq(Admin::getUserName, loginId.toString())
                    .eq(Admin::getDelFlag, "0")
                    .last("limit 1"));
            if (admin == null) {
                return ResultVo.error("账号不存在或已停用");
            }
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("token", ticket.getToken());
            data.put("userName", admin.getUserName());
            data.put("className", admin.getClassName());
            data.put("role", AdminGuardService.roleOf(admin));
            return ResultVo.success(data);
        } catch (IllegalArgumentException e) {
            return ResultVo.error(e.getMessage());
        }
    }

    private void bind(OAuthState state, GithubOAuthService.GithubUser githubUser) {
        if (!StringUtils.hasText(state.getAdminUsername())) {
            throw new IllegalArgumentException("绑定请求缺少管理员信息");
        }
        Admin admin = adminMapper.selectOne(new LambdaQueryWrapper<Admin>()
                .eq(Admin::getUserName, state.getAdminUsername())
                .eq(Admin::getDelFlag, "0")
                .last("limit 1"));
        if (admin == null) {
            throw new IllegalArgumentException("管理员账号不存在");
        }
        Admin occupied = adminMapper.selectOne(new LambdaQueryWrapper<Admin>()
                .eq(Admin::getGithubId, githubUser.id())
                .last("limit 1"));
        if (occupied != null && !occupied.getId().equals(admin.getId())) {
            throw new IllegalArgumentException("该 GitHub 账号已绑定其他用户");
        }
        admin.setGithubId(githubUser.id());
        admin.setGithubLogin(githubUser.login());
        adminMapper.updateById(admin);
        log.info("管理员 {} 绑定 GitHub：{}", admin.getUserName(), githubUser.login());
    }
}
