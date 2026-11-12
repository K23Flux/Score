package com.score.service;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.score.common.NotAdminException;
import com.score.entity.Admin;
import com.score.mapper.AdminMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 登录身份与管理员权限校验。角色以数据库为准，不依赖会话里的旧值。
 */
@Service
public class AdminGuardService {

    public static final String ROLE_ADMIN = "admin";
    public static final String ROLE_TEACHER = "teacher";

    @Autowired
    private AdminMapper adminMapper;

    /** 已登录用户；账号被删除或停用时视为无效。 */
    public Admin currentUser() {
        StpUtil.checkLogin();
        String userName = StpUtil.getLoginIdAsString();
        Admin admin = adminMapper.selectOne(new LambdaQueryWrapper<Admin>()
                .eq(Admin::getUserName, userName)
                .eq(Admin::getDelFlag, "0")
                .last("limit 1"));
        if (admin == null) {
            throw new NotAdminException("账号不存在或已停用，请重新登录");
        }
        return admin;
    }

    public Admin requireAdmin() {
        Admin admin = currentUser();
        if (!ROLE_ADMIN.equalsIgnoreCase(admin.getRole())) {
            throw new NotAdminException();
        }
        return admin;
    }

    public static String roleOf(Admin admin) {
        return admin == null || admin.getRole() == null ? ROLE_TEACHER : admin.getRole();
    }
}
