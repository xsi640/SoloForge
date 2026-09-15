package com.soloforge.board.task.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.soloforge.board.query.dto.TaskQuery;
import com.soloforge.board.task.entity.Task;
import com.soloforge.board.task.entity.TaskDetail;

@Mapper
public interface TaskMapper {

    TaskDetail findDetailById(@Param("id") Long id);

    List<TaskDetail> findByCondition(TaskQuery query);

    List<TaskDetail> findReminderTasks(@Param("assigneeId") Long assigneeId);

    int insert(Task task);

    int update(Task task);

    int updateColumnId(@Param("id") Long id, @Param("columnId") Long columnId);

    int deleteById(@Param("id") Long id);
}