package com.soloforge.board.task.controller;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.soloforge.board.common.R;
import com.soloforge.board.task.dto.TaskSaveRequest;
import com.soloforge.board.task.dto.TaskStatusRequest;
import com.soloforge.board.task.service.TaskService;
import com.soloforge.board.task.vo.TaskVO;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @PostMapping
    public R<TaskVO> create(@Valid @RequestBody TaskSaveRequest request) {
        return R.ok(taskService.create(request));
    }

    @PutMapping("/{id}")
    public R<TaskVO> update(@PathVariable Long id, @Valid @RequestBody TaskSaveRequest request) {
        return R.ok(taskService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        taskService.delete(id);
        return R.ok();
    }

    @GetMapping("/{id}")
    public R<TaskVO> detail(@PathVariable Long id) {
        return R.ok(taskService.detail(id));
    }

    @PatchMapping("/{id}/status")
    public R<TaskVO> updateStatus(@PathVariable Long id, @Valid @RequestBody TaskStatusRequest request) {
        return R.ok(taskService.updateStatus(id, request));
    }
}