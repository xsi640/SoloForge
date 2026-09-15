package com.soloforge.board.task.service.impl;

import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.soloforge.board.auth.service.AccountLookupService;
import com.soloforge.board.board.service.BoardService;
import com.soloforge.board.column.service.ColumnService;
import com.soloforge.board.common.BizException;
import com.soloforge.board.common.ErrorCode;
import com.soloforge.board.common.FieldErrorItem;
import com.soloforge.board.common.enums.Priority;
import com.soloforge.board.task.converter.TaskVOConverter;
import com.soloforge.board.task.dto.TaskSaveRequest;
import com.soloforge.board.task.dto.TaskStatusRequest;
import com.soloforge.board.task.entity.Task;
import com.soloforge.board.task.entity.TaskDetail;
import com.soloforge.board.task.mapper.TaskMapper;
import com.soloforge.board.task.service.TaskService;
import com.soloforge.board.task.vo.TaskVO;

@Service
public class TaskServiceImpl implements TaskService {

    private static final int TITLE_MAX_LENGTH = 100;
    private static final int DESCRIPTION_MAX_LENGTH = 1000;

    private final TaskMapper taskMapper;
    private final BoardService boardService;
    private final ColumnService columnService;
    private final AccountLookupService accountLookupService;

    public TaskServiceImpl(TaskMapper taskMapper, BoardService boardService, ColumnService columnService,
            AccountLookupService accountLookupService) {
        this.taskMapper = taskMapper;
        this.boardService = boardService;
        this.columnService = columnService;
        this.accountLookupService = accountLookupService;
    }

    @Override
    @Transactional
    public TaskVO create(TaskSaveRequest request) {
        Task task = new Task();
        task.setBoardId(boardService.requireBoardId());
        applyEditableFields(task, request);
        taskMapper.insert(task);
        return requireDetail(task.getId());
    }

    @Override
    @Transactional
    public TaskVO update(Long taskId, TaskSaveRequest request) {
        Task existing = requireTask(taskId);
        Task task = new Task();
        task.setId(taskId);
        task.setBoardId(existing.getBoardId());
        applyEditableFields(task, request);
        taskMapper.update(task);
        return requireDetail(taskId);
    }

    @Override
    @Transactional
    public void delete(Long taskId) {
        requireTask(taskId);
        taskMapper.deleteById(taskId);
    }

    @Override
    public TaskVO detail(Long taskId) {
        return requireDetail(taskId);
    }

    @Override
    @Transactional
    public TaskVO updateStatus(Long taskId, TaskStatusRequest request) {
        TaskDetail existing = requireTask(taskId);
        if (request.getColumnId() == null) {
            throw fieldError("columnId", "请选择所属列");
        }
        columnService.requireColumn(request.getColumnId());
        if (Objects.equals(existing.getColumnId(), request.getColumnId())) {
            return TaskVOConverter.toVO(existing);
        }
        taskMapper.updateColumnId(taskId, request.getColumnId());
        return requireDetail(taskId);
    }

    private void applyEditableFields(Task task, TaskSaveRequest request) {
        task.setTitle(resolveTitle(request.getTitle()));
        task.setAssigneeId(resolveAssigneeId(request.getAssigneeId()));
        task.setColumnId(resolveColumnId(request.getColumnId()));
        task.setPriority(resolvePriority(request.getPriority()));
        task.setDueDate(request.getDueDate());
        task.setDescription(resolveDescription(request.getDescription()));
    }

    private String resolveTitle(String title) {
        String trimmed = title == null ? "" : title.trim();
        if (trimmed.isEmpty()) {
            throw fieldError("title", "请输入任务标题");
        }
        if (trimmed.length() > TITLE_MAX_LENGTH) {
            throw fieldError("title", "标题最多 100 个字符");
        }
        return trimmed;
    }

    private Long resolveAssigneeId(Long assigneeId) {
        if (assigneeId == null || !accountLookupService.existsById(assigneeId)) {
            throw fieldError("assigneeId", "负责人不存在");
        }
        return assigneeId;
    }

    private Long resolveColumnId(Long columnId) {
        if (columnId == null) {
            throw fieldError("columnId", "请选择所属列");
        }
        columnService.requireColumn(columnId);
        return columnId;
    }

    private String resolvePriority(String priority) {
        if (priority == null || priority.trim().isEmpty()) {
            return Priority.MEDIUM.name();
        }
        String candidate = priority.trim();
        for (Priority value : Priority.values()) {
            if (value.name().equals(candidate)) {
                return value.name();
            }
        }
        throw fieldError("priority", "优先级取值不正确");
    }

    private String resolveDescription(String description) {
        if (description == null) {
            return null;
        }
        if (description.length() > DESCRIPTION_MAX_LENGTH) {
            throw fieldError("description", "描述最多 1000 个字符");
        }
        return description.trim();
    }

    private TaskDetail requireTask(Long taskId) {
        TaskDetail detail = taskId == null ? null : taskMapper.findDetailById(taskId);
        if (detail == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "任务不存在");
        }
        return detail;
    }

    private TaskVO requireDetail(Long taskId) {
        return TaskVOConverter.toVO(requireTask(taskId));
    }

    private BizException fieldError(String field, String reason) {
        return new BizException(ErrorCode.VALIDATION_FAILED, reason, new FieldErrorItem(field, reason));
    }
}