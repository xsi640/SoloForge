package com.soloforge.board.common;

import java.time.LocalDate;
import java.time.ZoneId;

import com.soloforge.board.common.enums.DueState;

public final class DueDateCalculator {

    public static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");

    private DueDateCalculator() {
    }

    public static LocalDate today() {
        return LocalDate.now(ZONE);
    }

    public static DueState resolve(LocalDate dueDate) {
        if (dueDate == null) {
            return DueState.NONE;
        }
        LocalDate today = today();
        if (dueDate.isBefore(today)) {
            return DueState.OVERDUE;
        }
        if (dueDate.isBefore(today.plusDays(2))) {
            return DueState.DUE_SOON;
        }
        return DueState.NORMAL;
    }

    public static boolean isOverdue(LocalDate dueDate) {
        return resolve(dueDate) == DueState.OVERDUE;
    }

    public static boolean isDueSoon(LocalDate dueDate) {
        return resolve(dueDate) == DueState.DUE_SOON;
    }
}