package com.dacn.ATS.module.dashboard.dto;

public class DashboardStatsDTO {

    private long totalJobs;
    private long totalCandidates;
    private long totalApplications;
    private long totalInterviews;
    private long pendingApplications;
    private long scheduledInterviews;
    private Long totalCompanies;

    public long getTotalJobs() {
        return totalJobs;
    }

    public void setTotalJobs(long totalJobs) {
        this.totalJobs = totalJobs;
    }

    public long getTotalCandidates() {
        return totalCandidates;
    }

    public void setTotalCandidates(long totalCandidates) {
        this.totalCandidates = totalCandidates;
    }

    public long getTotalApplications() {
        return totalApplications;
    }

    public void setTotalApplications(long totalApplications) {
        this.totalApplications = totalApplications;
    }

    public long getTotalInterviews() {
        return totalInterviews;
    }

    public void setTotalInterviews(long totalInterviews) {
        this.totalInterviews = totalInterviews;
    }

    public long getPendingApplications() {
        return pendingApplications;
    }

    public void setPendingApplications(long pendingApplications) {
        this.pendingApplications = pendingApplications;
    }

    public long getScheduledInterviews() {
        return scheduledInterviews;
    }

    public void setScheduledInterviews(long scheduledInterviews) {
        this.scheduledInterviews = scheduledInterviews;
    }

    public Long getTotalCompanies() {
        return totalCompanies;
    }

    public void setTotalCompanies(Long totalCompanies) {
        this.totalCompanies = totalCompanies;
    }
}