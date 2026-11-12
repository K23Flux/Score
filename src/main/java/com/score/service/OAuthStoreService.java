package com.score.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.score.entity.LoginTicket;
import com.score.entity.OAuthState;
import com.score.mapper.LoginTicketMapper;
import com.score.mapper.OAuthStateMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.UUID;

/**
 * OAuth 的 state 与一次性登录票据，都存在数据库里并带过期时间。
 */
@Service
public class OAuthStoreService {

    private static final long STATE_TTL_MS = 10 * 60 * 1000L;
    private static final long TICKET_TTL_MS = 60 * 1000L;

    @Autowired
    private OAuthStateMapper stateMapper;
    @Autowired
    private LoginTicketMapper ticketMapper;

    public OAuthState createState(String mode, String adminUsername) {
        purge();
        OAuthState state = new OAuthState();
        state.setState(newId());
        state.setMode(mode);
        state.setAdminUsername(adminUsername);
        state.setExpiresAt(new Date(System.currentTimeMillis() + STATE_TTL_MS));
        stateMapper.insert(state);
        return state;
    }

    /** 取出并删除 state；不存在或过期时抛异常。 */
    public OAuthState consumeState(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("缺少 state");
        }
        OAuthState state = stateMapper.selectById(value);
        stateMapper.deleteById(value);
        if (state == null || state.getExpiresAt().before(new Date())) {
            throw new IllegalArgumentException("授权已失效，请重新发起");
        }
        return state;
    }

    public String issueTicket(String token) {
        purge();
        LoginTicket ticket = new LoginTicket();
        ticket.setTicket(newId());
        ticket.setToken(token);
        ticket.setExpiresAt(new Date(System.currentTimeMillis() + TICKET_TTL_MS));
        ticketMapper.insert(ticket);
        return ticket.getTicket();
    }

    /** 取出并删除票据；不存在或过期时抛异常。 */
    public LoginTicket consumeTicket(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("登录票据无效");
        }
        LoginTicket ticket = ticketMapper.selectById(value);
        ticketMapper.deleteById(value);
        if (ticket == null || ticket.getExpiresAt().before(new Date())) {
            throw new IllegalArgumentException("登录票据已过期，请重试");
        }
        return ticket;
    }

    private void purge() {
        Date now = new Date();
        stateMapper.delete(new LambdaQueryWrapper<OAuthState>().lt(OAuthState::getExpiresAt, now));
        ticketMapper.delete(new LambdaQueryWrapper<LoginTicket>().lt(LoginTicket::getExpiresAt, now));
    }

    private String newId() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
