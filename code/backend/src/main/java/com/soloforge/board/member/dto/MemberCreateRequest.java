package com.soloforge.board.member.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class MemberCreateRequest {

    @NotBlank(message = "请输入登录名")
    @Size(min = 3, max = 32, message = "登录名长度需为 3 至 32 个字符")
    @Pattern(regexp = "^[A-Za-z0-9_]+$", message = "登录名只能包含字母、数字与下划线")
    private String username;

    @NotBlank(message = "请输入显示名")
    @Size(min = 1, max = 32, message = "显示名长度需为 1 至 32 个字符")
    private String displayName;

    @NotBlank(message = "请输入密码")
    @Size(min = 6, max = 64, message = "密码长度需为 6 至 64 个字符")
    private String password;

    private String status;

    private Boolean isAdmin;

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

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Boolean getIsAdmin() {
        return isAdmin;
    }

    public void setIsAdmin(Boolean isAdmin) {
        this.isAdmin = isAdmin;
    }
}