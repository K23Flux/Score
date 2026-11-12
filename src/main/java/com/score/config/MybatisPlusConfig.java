package com.score.config;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.handler.TenantLineHandler;
import com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.StringValue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis-Plus 配置类
 * 启用多租户插件，实现按班级隔离数据
 */
import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;

@Configuration
public class MybatisPlusConfig {

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();

        // 1. 添加多租户插件
        interceptor.addInnerInterceptor(new TenantLineInnerInterceptor(new TenantLineHandler() {
            @Override
            public Expression getTenantId() {
                if(StpUtil.isLogin()){
                    String className =(String) StpUtil.getSession().get("className");
                    return new StringValue(className !=null?className:"DENY_ACCESS");
                }
                return new StringValue("DENY_ACCESS");
            }

            @Override
            public String getTenantIdColumn() {
                return "class_name";
            }

            @Override
            public boolean ignoreTable(String tableName) {
                // admin、系统设置和学期表不属于班级数据，不注入 class_name
                return "admin".equalsIgnoreCase(tableName)
                        || "app_setting".equalsIgnoreCase(tableName)
                        || "academic_term".equalsIgnoreCase(tableName)
                        || "oauth_state".equalsIgnoreCase(tableName)
                        || "login_ticket".equalsIgnoreCase(tableName);
            }
        }));

        // 添加分页插件
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));

        return interceptor;
    }
}
