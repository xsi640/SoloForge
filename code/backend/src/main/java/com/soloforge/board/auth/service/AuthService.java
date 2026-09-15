package com.soloforge.board.auth.service;

import com.soloforge.board.auth.dto.LoginRequest;
import com.soloforge.board.auth.vo.UserVO;

import jakarta.servlet.http.HttpSession;

public interface AuthService {

    UserVO login(LoginRequest request, HttpSession session);

    void logout(HttpSession session);

    UserVO currentUser(Long userId);

    void ensureInitialAdmin();
}