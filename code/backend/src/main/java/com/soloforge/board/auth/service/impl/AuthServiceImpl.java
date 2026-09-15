package com.soloforge.board.auth.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.soloforge.board.auth.config.AdminProperties;
import com.soloforge.board.auth.dto.LoginRequest;
import com.soloforge.board.auth.entity.AppUser;
import com.soloforge.board.auth.mapper.AppUserMapper;
import com.soloforge.board.auth.service.AuthService;
import com.soloforge.board.auth.vo.UserVO;
import com.soloforge.board.common.BizException;
import com.soloforge.board.common.CurrentUser;
import com.soloforge.board.common.ErrorCode;
import com.soloforge.board.common.SessionUtil;
import com.soloforge.board.common.enums.UserStatus;

import jakarta.servlet.http.HttpSession;

@Service
public class AuthServiceImpl implements AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthServiceImpl.class);

    private final AppUserMapper appUserMapper;
    private final PasswordEncoder passwordEncoder;
    private final AdminProperties adminProperties;

    public AuthServiceImpl(AppUserMapper appUserMapper, PasswordEncoder passwordEncoder, AdminProperties adminProperties) {
        this.appUserMapper = appUserMapper;
        this.passwordEncoder = passwordEncoder;
        this.adminProperties = adminProperties;
    }

    @Override
    public UserVO login(LoginRequest request, HttpSession session) {
        AppUser user = appUserMapper.findByUsername(request.getUsername());
        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BizException(ErrorCode.BAD_CREDENTIALS);
        }
        if (UserStatus.DISABLED.name().equals(user.getStatus())) {
            throw new BizException(ErrorCode.ACCOUNT_DISABLED);
        }
        SessionUtil.set(session, new CurrentUser(user.getId(), user.getUsername(), user.getDisplayName(), Boolean.TRUE.equals(user.getAdmin())));
        return toUserVO(user);
    }

    @Override
    public void logout(HttpSession session) {
        if (session == null) {
            return;
        }
        SessionUtil.clear(session);
        session.invalidate();
    }

    @Override
    public UserVO currentUser(Long userId) {
        AppUser user = userId == null ? null : appUserMapper.findById(userId);
        if (user == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
        if (UserStatus.DISABLED.name().equals(user.getStatus())) {
            throw new BizException(ErrorCode.ACCOUNT_DISABLED);
        }
        return toUserVO(user);
    }

    @Override
    @Transactional
    public void ensureInitialAdmin() {
        if (appUserMapper.countAll() > 0) {
            return;
        }
        AppUser admin = new AppUser();
        admin.setUsername(adminProperties.getUsername());
        admin.setDisplayName(adminProperties.getDisplayName());
        admin.setPasswordHash(passwordEncoder.encode(adminProperties.getPassword()));
        admin.setAdmin(Boolean.TRUE);
        admin.setStatus(UserStatus.ENABLED.name());
        appUserMapper.insert(admin);
        log.warn("用户表为空，已创建初始管理员账号 [{}]，请首次登录后立即重置密码", adminProperties.getUsername());
    }

    private UserVO toUserVO(AppUser user) {
        return new UserVO(user.getId(), user.getUsername(), user.getDisplayName(), Boolean.TRUE.equals(user.getAdmin()));
    }
}
