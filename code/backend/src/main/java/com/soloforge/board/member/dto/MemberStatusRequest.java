package com.soloforge.board.member.dto;

import jakarta.validation.constraints.NotBlank;

public class MemberStatusRequest {

    @NotBlank(message = "请选择账号状态")
    private String status;

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}