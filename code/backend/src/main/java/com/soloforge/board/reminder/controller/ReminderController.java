package com.soloforge.board.reminder.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.soloforge.board.common.R;
import com.soloforge.board.common.SessionUtil;
import com.soloforge.board.reminder.service.ReminderService;
import com.soloforge.board.reminder.vo.ReminderVO;

import jakarta.servlet.http.HttpSession;

@RestController
@RequestMapping("/api/reminders")
public class ReminderController {

    private final ReminderService reminderService;

    public ReminderController(ReminderService reminderService) {
        this.reminderService = reminderService;
    }

    @GetMapping("/mine")
    public R<ReminderVO> mine(HttpSession session) {
        return R.ok(reminderService.myReminders(SessionUtil.get(session).getUserId()));
    }
}