package com.soloforge.board.common;

import java.io.Serializable;

public class CurrentUser implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long userId;
    private String username;
    private String displayName;
    private boolean admin;

    public CurrentUser() {
    }

    public CurrentUser(Long userId, String username, String displayName, boolean admin) {
        this.userId = userId;
        this.username = username;
        this.displayName = displayName;
        this.admin = admin;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public boolean isAdmin() {
        return admin;
    }

    public void setAdmin(boolean admin) {
        this.admin = admin;
    }
}