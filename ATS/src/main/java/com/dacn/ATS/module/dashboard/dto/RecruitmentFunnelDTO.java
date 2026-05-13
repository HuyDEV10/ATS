package com.dacn.ATS.module.dashboard.dto;

public class RecruitmentFunnelDTO {
    private long pending;
    private long aiScreened;
    private long shortlisted;
    private long interviewScheduled;
    private long interviewed;
    private long offered;
    private long rejected;

    public long getPending() {
        return pending;
    }

    public void setPending(long pending) {
        this.pending = pending;
    }

    public long getAiScreened() {
        return aiScreened;
    }

    public void setAiScreened(long aiScreened) {
        this.aiScreened = aiScreened;
    }

    public long getShortlisted() {
        return shortlisted;
    }

    public void setShortlisted(long shortlisted) {
        this.shortlisted = shortlisted;
    }

    public long getInterviewScheduled() {
        return interviewScheduled;
    }

    public void setInterviewScheduled(long interviewScheduled) {
        this.interviewScheduled = interviewScheduled;
    }

    public long getInterviewed() {
        return interviewed;
    }

    public void setInterviewed(long interviewed) {
        this.interviewed = interviewed;
    }

    public long getOffered() {
        return offered;
    }

    public void setOffered(long offered) {
        this.offered = offered;
    }

    public long getRejected() {
        return rejected;
    }

    public void setRejected(long rejected) {
        this.rejected = rejected;
    }
}