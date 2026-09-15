package com.soloforge.board.task.dto;

import jakarta.validation.constraints.NotNull;

public class TaskStatusRequest {

    @NotNull(message = "请选择目标列")
    private Long columnId;

    public Long getColumnId() {
        return columnId;
    }

    public void setColumnId(Long columnId) {
        this.columnId = columnId;
    }
}