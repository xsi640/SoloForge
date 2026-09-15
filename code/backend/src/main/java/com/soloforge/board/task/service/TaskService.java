package com.soloforge.board.task.service;

import com.soloforge.board.task.dto.TaskSaveRequest;
import com.soloforge.board.task.dto.TaskStatusRequest;
import com.soloforge.board.task.vo.TaskVO;

public interface TaskService {

    TaskVO create(TaskSaveRequest request);

    TaskVO update(Long taskId, TaskSaveRequest request);

    void delete(Long taskId);

    TaskVO detail(Long taskId);

    TaskVO updateStatus(Long taskId, TaskStatusRequest request);
}