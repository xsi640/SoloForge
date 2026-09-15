package com.soloforge.board.auth.vo;

import com.fasterxml.jackson.annotation.JsonProperty;

public class UserVO {

    private Long id;
    private String username;
    private String displayName;

    @JsonProperty("isAdmin")
    private boolean isAdmin;

    public UserVO() {
    }

    public UserVO(Long id, String username, String displayName, boolean isAdmin) {
        this.id = id;
        this.username = username;
        this.displayName = displayName;
        this.isAdmin = isAdmin;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public boolean getIsAdmin() {
        return isAdmin;
    }

    public void setIsAdmin(boolean isAdmin) {
        this.isAdmin = isAdmin;
    }
}