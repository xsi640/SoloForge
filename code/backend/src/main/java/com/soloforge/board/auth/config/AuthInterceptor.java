package com.soloforge.board.auth.config;

import java.io.IOException;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.soloforge.board.auth.entity.AppUser;
import com.soloforge.board.auth.mapper.AppUserMapper;
import com.soloforge.board.common.CurrentUser;
import com.soloforge.board.common.ErrorCode;
import com.soloforge.board.common.R;
import com.soloforge.board.common.SessionUtil;
import com.soloforge.board.common.enums.UserStatus;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@Component
public class AuthInterceptor implements HandlerInterceptor {

    private static final String MEMBER_API = "/api/members";
    private static final String JSON_CONTENT_TYPE = "application/json;charset=UTF-8";

    private final AppUserMapper appUserMapper;
    private final ObjectMapper objectMapper;

    public AuthInterceptor(AppUserMapper appUserMapper, ObjectMapper objectMapper) {
        this.appUserMapper = appUserMapper;
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws IOException {
        CurrentUser sessionUser = SessionUtil.get(request.getSession(false));
        if (sessionUser == null || sessionUser.getUserId() == null) {
            return writeError(response, ErrorCode.UNAUTHORIZED);
        }

        AppUser user = appUserMapper.findById(sessionUser.getUserId());
        if (user == null || UserStatus.DISABLED.name().equals(user.getStatus())) {
            invalidate(request);
            return writeError(response, ErrorCode.ACCOUNT_DISABLED);
        }

        SessionUtil.set(request.getSession(), new CurrentUser(user.getId(), user.getUsername(), user.getDisplayName(), Boolean.TRUE.equals(user.getAdmin())));

        if (isMemberApi(request) && !Boolean.TRUE.equals(user.getAdmin())) {
            return writeError(response, ErrorCode.FORBIDDEN);
        }
        return true;
    }

    private boolean isMemberApi(HttpServletRequest request) {
        String path = path(request);
        return MEMBER_API.equals(path) || path.startsWith(MEMBER_API + "/");
    }

    private String path(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String contextPath = request.getContextPath();
        if (contextPath == null || contextPath.isEmpty()) {
            return uri == null ? "" : uri;
        }
        return uri == null ? "" : uri.substring(contextPath.length());
    }

    private void invalidate(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            SessionUtil.clear(session);
            session.invalidate();
        }
    }

    private boolean writeError(HttpServletResponse response, ErrorCode errorCode) throws IOException {
        response.setStatus(errorCode.getHttpStatus());
        response.setContentType(JSON_CONTENT_TYPE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(R.fail(errorCode)));
        return false;
    }
}
