package com.score.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * GitHub OAuth 的 state，一次性、带过期时间。
 */
@Data
@TableName("oauth_state")
public class OAuthState {
    @TableId(type = IdType.INPUT)
    private String state;

    /** login=登录，bind=绑定当前管理员 */
    private String mode;

    /** bind 模式下要绑定的管理员用户名 */
    private String adminUsername;

    private Date expiresAt;
}
