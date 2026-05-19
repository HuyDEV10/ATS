package com.dacn.ATS.module.application.entity;

import com.baomidou.mybatisplus.annotation.*;
import java.time.LocalDateTime;

@TableName("job_applications")
public class JobApplication {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long jobId;
    private Long candidateId;

    // CV được dùng cho lần ứng tuyển này
    private Long resumeId;

    private String status;

    // NO_RESUME, CV_PARSE_FAILED, VERIFIED, NEEDS_REVIEW, IDENTITY_CONFLICT
    private String verificationStatus;

    // 0 - 100, càng cao càng lệch giữa form và CV
    private Integer mismatchScore;

    // Tóm tắt các điểm lệch giữa form và CV
    private String mismatchSummary;

    private String hrNotes;
    private LocalDateTime applicationDate;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getJobId() {
        return jobId;
    }

    public void setJobId(Long jobId) {
        this.jobId = jobId;
    }

    public Long getCandidateId() {
        return candidateId;
    }

    public void setCandidateId(Long candidateId) {
        this.candidateId = candidateId;
    }

    public Long getResumeId() {
        return resumeId;
    }

    public void setResumeId(Long resumeId) {
        this.resumeId = resumeId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getVerificationStatus() {
        return verificationStatus;
    }

    public void setVerificationStatus(String verificationStatus) {
        this.verificationStatus = verificationStatus;
    }

    public Integer getMismatchScore() {
        return mismatchScore;
    }

    public void setMismatchScore(Integer mismatchScore) {
        this.mismatchScore = mismatchScore;
    }

    public String getMismatchSummary() {
        return mismatchSummary;
    }

    public void setMismatchSummary(String mismatchSummary) {
        this.mismatchSummary = mismatchSummary;
    }

    public String getHrNotes() {
        return hrNotes;
    }

    public void setHrNotes(String hrNotes) {
        this.hrNotes = hrNotes;
    }

    public LocalDateTime getApplicationDate() {
        return applicationDate;
    }

    public void setApplicationDate(LocalDateTime applicationDate) {
        this.applicationDate = applicationDate;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }

    public Integer getDeleted() {
        return deleted;
    }

    public void setDeleted(Integer deleted) {
        this.deleted = deleted;
    }
}