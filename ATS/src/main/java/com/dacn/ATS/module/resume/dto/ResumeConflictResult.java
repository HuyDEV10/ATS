package com.dacn.ATS.module.resume.dto;

import java.util.ArrayList;
import java.util.List;

public class ResumeConflictResult {
    private Long candidateId;
    private Long resumeId;
    private String conflictLevel; // NONE, LOW, MEDIUM, HIGH
    private List<String> warnings = new ArrayList<>();

    private String formEmail;
    private String cvEmail;
    private String formPhone;
    private String cvPhone;
    private String formSkills;
    private String cvSkills;

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

    public String getConflictLevel() {
        return conflictLevel;
    }

    public void setConflictLevel(String conflictLevel) {
        this.conflictLevel = conflictLevel;
    }

    public List<String> getWarnings() {
        return warnings;
    }

    public void setWarnings(List<String> warnings) {
        this.warnings = warnings;
    }

    public String getFormEmail() {
        return formEmail;
    }

    public void setFormEmail(String formEmail) {
        this.formEmail = formEmail;
    }

    public String getCvEmail() {
        return cvEmail;
    }

    public void setCvEmail(String cvEmail) {
        this.cvEmail = cvEmail;
    }

    public String getFormPhone() {
        return formPhone;
    }

    public void setFormPhone(String formPhone) {
        this.formPhone = formPhone;
    }

    public String getCvPhone() {
        return cvPhone;
    }

    public void setCvPhone(String cvPhone) {
        this.cvPhone = cvPhone;
    }

    public String getFormSkills() {
        return formSkills;
    }

    public void setFormSkills(String formSkills) {
        this.formSkills = formSkills;
    }

    public String getCvSkills() {
        return cvSkills;
    }

    public void setCvSkills(String cvSkills) {
        this.cvSkills = cvSkills;
    }
}