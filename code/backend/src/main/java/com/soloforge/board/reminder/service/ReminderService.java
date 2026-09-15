package com.soloforge.board.reminder.service;

import com.soloforge.board.reminder.vo.ReminderVO;

public interface ReminderService {

    ReminderVO myReminders(Long userId);
}