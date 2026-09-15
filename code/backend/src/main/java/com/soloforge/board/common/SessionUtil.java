package com.soloforge.board.common;

import jakarta.servlet.http.HttpSession;

public final class SessionUtil {

    public static final String CURRENT_USER_KEY = "BOARD_CURRENT_USER";

    private SessionUtil() {
    }

    public static CurrentUser get(HttpSession session) {
        if (session == null) {
            return null;
        }
        return (CurrentUser) session.getAttribute(CURRENT_USER_KEY);
    }

    public static void set(HttpSession session, CurrentUser user) {
        session.setAttribute(CURRENT_USER_KEY, user);
    }

    public static void clear(HttpSession session) {
        if (session != null) {
            session.removeAttribute(CURRENT_USER_KEY);
        }
    }
}