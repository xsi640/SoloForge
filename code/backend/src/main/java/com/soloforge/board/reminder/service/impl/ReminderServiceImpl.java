package com.soloforge.board.reminder.service.impl;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;

import com.soloforge.board.common.DueDateCalculator;
import com.soloforge.board.common.enums.DueState;
import com.soloforge.board.reminder.service.ReminderService;
import com.soloforge.board.reminder.vo.ReminderVO;
import com.soloforge.board.task.converter.TaskVOConverter;
import com.soloforge.board.task.entity.TaskDetail;
import com.soloforge.board.task.mapper.TaskMapper;
import com.soloforge.board.task.vo.TaskVO;

@Service
public class ReminderServiceImpl implements ReminderService {

    private static final Comparator<TaskVO> DUE_DATE_ORDER = Comparator.comparing(TaskVO::getDueDate)
            .thenComparing(TaskVO::getId);

    private final TaskMapper taskMapper;

    public ReminderServiceImpl(TaskMapper taskMapper) {
        this.taskMapper = taskMapper;
    }

    @Override
    public ReminderVO myReminders(Long userId) {
        List<TaskVO> overdue = new ArrayList<>();
        List<TaskVO> dueSoon = new ArrayList<>();
        if (userId != null) {
            for (TaskDetail detail : taskMapper.findReminderTasks(userId)) {
                DueState state = DueDateCalculator.resolve(detail.getDueDate());
                if (state == DueState.OVERDUE) {
                    overdue.add(TaskVOConverter.toVO(detail));
                } else if (state == DueState.DUE_SOON) {
                    dueSoon.add(TaskVOConverter.toVO(detail));
                }
            }
        }
        overdue.sort(DUE_DATE_ORDER);
        dueSoon.sort(DUE_DATE_ORDER);
        List<TaskVO> items = new ArrayList<>(overdue);
        items.addAll(dueSoon);
        return new ReminderVO(items, overdue.size(), dueSoon.size());
    }
}