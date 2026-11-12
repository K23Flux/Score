package com.score.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * OAuth 登录成功后交给前端的短期一次性票据。
 */
@Data
@TableName("login_ticket")
public class LoginTicket {
    @TableId(type = IdType.INPUT)
    private String ticket;

    private String token;

    private Date expiresAt;
}
