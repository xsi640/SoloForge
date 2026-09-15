package com.soloforge.board.auth.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.soloforge.board.auth.dto.LoginRequest;
import com.soloforge.board.auth.service.AuthService;
import com.soloforge.board.auth.vo.UserVO;
import com.soloforge.board.common.BizException;
import com.soloforge.board.common.CurrentUser;
import com.soloforge.board.common.ErrorCode;
import com.soloforge.board.common.R;
import com.soloforge.board.common.SessionUtil;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public R<UserVO> login(@Valid @RequestBody LoginRequest request, HttpSession session) {
        return R.ok(authService.login(request, session));
    }

    @PostMapping("/logout")
    public R<Void> logout(HttpSession session) {
        authService.logout(session);
        return R.ok();
    }

    @GetMapping("/me")
    public R<UserVO> me(HttpSession session) {
        CurrentUser currentUser = SessionUtil.get(session);
        if (currentUser == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
        return R.ok(authService.currentUser(currentUser.getUserId()));
    }
}
