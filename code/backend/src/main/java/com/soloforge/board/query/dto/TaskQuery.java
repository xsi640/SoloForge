package com.soloforge.board.query.dto;

import java.time.LocalDate;

public class TaskQuery {

    private String keyword;
    private Long assigneeId;
    private String priority;
    private String dueFilter;
    private LocalDate referenceDate;

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public Long getAssigneeId() {
        return assigneeId;
    }

    public void setAssigneeId(Long assigneeId) {
        this.assigneeId = assigneeId;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public String getDueFilter() {
        return dueFilter;
    }

    public void setDueFilter(String dueFilter) {
        this.dueFilter = dueFilter;
    }

    public LocalDate getReferenceDate() {
        return referenceDate;
    }

    public void setReferenceDate(LocalDate referenceDate) {
        this.referenceDate = referenceDate;
    }
}