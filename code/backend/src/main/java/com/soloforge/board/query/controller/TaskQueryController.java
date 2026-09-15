package com.soloforge.board.query.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.soloforge.board.common.R;
import com.soloforge.board.query.dto.TaskQuery;
import com.soloforge.board.query.service.TaskQueryService;
import com.soloforge.board.task.vo.TaskVO;

@RestController
@RequestMapping("/api/tasks")
public class TaskQueryController {

    private final TaskQueryService taskQueryService;

    public TaskQueryController(TaskQueryService taskQueryService) {
        this.taskQueryService = taskQueryService;
    }

    @GetMapping
    public R<List<TaskVO>> query(TaskQuery query) {
        return R.ok(taskQueryService.query(query));
    }
}