package com.soloforge.board.reminder.vo;

import java.util.ArrayList;
import java.util.List;

import com.soloforge.board.task.vo.TaskVO;

public class ReminderVO {

    private List<TaskVO> items = new ArrayList<>();
    private int overdueCount;
    private int dueSoonCount;

    public ReminderVO() {
    }

    public ReminderVO(List<TaskVO> items, int overdueCount, int dueSoonCount) {
        this.items = items;
        this.overdueCount = overdueCount;
        this.dueSoonCount = dueSoonCount;
    }

    public List<TaskVO> getItems() {
        return items;
    }

    public void setItems(List<TaskVO> items) {
        this.items = items;
    }

    public int getOverdueCount() {
        return overdueCount;
    }

    public void setOverdueCount(int overdueCount) {
        this.overdueCount = overdueCount;
    }

    public int getDueSoonCount() {
        return dueSoonCount;
    }

    public void setDueSoonCount(int dueSoonCount) {
        this.dueSoonCount = dueSoonCount;
    }
}