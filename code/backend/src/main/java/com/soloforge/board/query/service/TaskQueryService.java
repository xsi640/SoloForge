package com.soloforge.board.query.service;

import java.util.List;

import com.soloforge.board.query.dto.TaskQuery;
import com.soloforge.board.task.vo.TaskVO;

public interface TaskQueryService {

    List<TaskVO> query(TaskQuery query);
}