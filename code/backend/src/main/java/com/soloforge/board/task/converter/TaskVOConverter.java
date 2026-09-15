package com.soloforge.board.task.converter;

import java.util.ArrayList;
import java.util.List;

import com.soloforge.board.common.DueDateCalculator;
import com.soloforge.board.common.enums.DueState;
import com.soloforge.board.task.entity.TaskDetail;
import com.soloforge.board.task.vo.TaskVO;

public final class TaskVOConverter {

    private TaskVOConverter() {
    }

    public static TaskVO toVO(TaskDetail detail) {
        if (detail == null) {
            return null;
        }
        TaskVO vo = new TaskVO();
        vo.setId(detail.getId());
        vo.setTitle(detail.getTitle());
        vo.setAssigneeId(detail.getAssigneeId());
        vo.setAssigneeName(detail.getAssigneeName());
        vo.setColumnId(detail.getColumnId());
        vo.setColumnCode(detail.getColumnCode());
        vo.setPriority(detail.getPriority());
        vo.setDueDate(detail.getDueDate());
        DueState dueState = DueDateCalculator.resolve(detail.getDueDate());
        vo.setDueState(dueState.name());
        vo.setDescription(detail.getDescription());
        vo.setCreatedAt(detail.getCreatedAt());
        vo.setUpdatedAt(detail.getUpdatedAt());
        return vo;
    }

    public static List<TaskVO> toVOList(List<TaskDetail> details) {
        List<TaskVO> result = new ArrayList<>();
        if (details == null) {
            return result;
        }
        for (TaskDetail detail : details) {
            result.add(toVO(detail));
        }
        return result;
    }
}