package com.bob.api.conversation.dto;

public class BulkActionFields {

    private String status;
    private Integer assigneeId;
    private boolean assigneeIdPresent;
    private Long teamId;
    private boolean teamIdPresent;

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getAssigneeId() {
        return assigneeId;
    }

    public void setAssigneeId(Integer assigneeId) {
        this.assigneeIdPresent = true;
        this.assigneeId = assigneeId;
    }

    public boolean assigneeIdPresent() {
        return assigneeIdPresent;
    }

    public Long getTeamId() {
        return teamId;
    }

    public void setTeamId(Long teamId) {
        this.teamIdPresent = true;
        this.teamId = teamId;
    }

    public boolean teamIdPresent() {
        return teamIdPresent;
    }
}
