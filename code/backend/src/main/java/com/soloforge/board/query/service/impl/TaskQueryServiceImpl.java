package com.soloforge.board.query.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;

import com.soloforge.board.common.BizException;
import com.soloforge.board.common.DueDateCalculator;
import com.soloforge.board.common.ErrorCode;
import com.soloforge.board.common.FieldErrorItem;
import com.soloforge.board.common.enums.Priority;
import com.soloforge.board.query.dto.TaskQuery;
import com.soloforge.board.query.service.TaskQueryService;
import com.soloforge.board.task.converter.TaskVOConverter;
import com.soloforge.board.task.mapper.TaskMapper;
import com.soloforge.board.task.vo.TaskVO;

@Service
public class TaskQueryServiceImpl implements TaskQueryService {

    private static final String DUE_FILTER_ALL = "ALL";
    private static final String DUE_FILTER_OVERDUE = "OVERDUE";
    private static final String DUE_FILTER_DUE_SOON = "DUE_SOON";

    private final TaskMapper taskMapper;

    public TaskQueryServiceImpl(TaskMapper taskMapper) {
        this.taskMapper = taskMapper;
    }

    @Override
    public List<TaskVO> query(TaskQuery query) {
        return TaskVOConverter.toVOList(taskMapper.findByCondition(normalize(query)));
    }

    private TaskQuery normalize(TaskQuery query) {
        TaskQuery normalized = new TaskQuery();
        normalized.setKeyword(resolveKeyword(query == null ? null : query.getKeyword()));
        normalized.setAssigneeId(resolveAssigneeId(query == null ? null : query.getAssigneeId()));
        normalized.setPriority(resolvePriority(query == null ? null : query.getPriority()));
        normalized.setDueFilter(resolveDueFilter(query == null ? null : query.getDueFilter()));
        normalized.setReferenceDate(DueDateCalculator.today());
        return normalized;
    }

    private String resolveKeyword(String keyword) {
        if (keyword == null) {
            return null;
        }
        String trimmed = keyword.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private Long resolveAssigneeId(Long assigneeId) {
        if (assigneeId == null || assigneeId <= 0L) {
            return null;
        }
        return assigneeId;
    }

    private String resolvePriority(String priority) {
        if (priority == null || priority.trim().isEmpty()) {
            return null;
        }
        String candidate = priority.trim();
        for (Priority value : Priority.values()) {
            if (value.name().equals(candidate)) {
                return value.name();
            }
        }
        throw fieldError("priority", "优先级取值不正确");
    }

    private String resolveDueFilter(String dueFilter) {
        if (dueFilter == null || dueFilter.trim().isEmpty()) {
            return DUE_FILTER_ALL;
        }
        String candidate = dueFilter.trim();
        if (DUE_FILTER_ALL.equals(candidate) || DUE_FILTER_OVERDUE.equals(candidate)
                || DUE_FILTER_DUE_SOON.equals(candidate)) {
            return candidate;
        }
        throw fieldError("dueFilter", "截止日期筛选取值不正确");
    }

    private BizException fieldError(String field, String reason) {
        return new BizException(ErrorCode.VALIDATION_FAILED, reason, new FieldErrorItem(field, reason));
    }
}