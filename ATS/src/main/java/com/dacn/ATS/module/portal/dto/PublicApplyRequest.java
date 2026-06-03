package com.dacn.ATS.module.portal.dto;

import org.springframework.web.multipart.MultipartFile;

public class PublicApplyRequest {

    private String name;
    private String email;
    private String phone;
    private String skills;
    private Integer experienceYears;
    private MultipartFile file;

    // Optional Skill Evidence 1
    private String evidenceSkill1;
    private String evidenceType1;
    private String evidenceProvider1;
    private String evidenceUrl1;

    // Optional Skill Evidence 2
    private String evidenceSkill2;
    private String evidenceType2;
    private String evidenceProvider2;
    private String evidenceUrl2;

    // Optional Skill Evidence 3
    private String evidenceSkill3;
    private String evidenceType3;
    private String evidenceProvider3;
    private String evidenceUrl3;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getSkills() {
        return skills;
    }

    public void setSkills(String skills) {
        this.skills = skills;
    }

    public Integer getExperienceYears() {
        return experienceYears;
    }

    public void setExperienceYears(Integer experienceYears) {
        this.experienceYears = experienceYears;
    }

    public MultipartFile getFile() {
        return file;
    }

    public void setFile(MultipartFile file) {
        this.file = file;
    }

    public String getEvidenceSkill1() {
        return evidenceSkill1;
    }

    public void setEvidenceSkill1(String evidenceSkill1) {
        this.evidenceSkill1 = evidenceSkill1;
    }

    public String getEvidenceType1() {
        return evidenceType1;
    }

    public void setEvidenceType1(String evidenceType1) {
        this.evidenceType1 = evidenceType1;
    }

    public String getEvidenceProvider1() {
        return evidenceProvider1;
    }

    public void setEvidenceProvider1(String evidenceProvider1) {
        this.evidenceProvider1 = evidenceProvider1;
    }

    public String getEvidenceUrl1() {
        return evidenceUrl1;
    }

    public void setEvidenceUrl1(String evidenceUrl1) {
        this.evidenceUrl1 = evidenceUrl1;
    }

    public String getEvidenceSkill2() {
        return evidenceSkill2;
    }

    public void setEvidenceSkill2(String evidenceSkill2) {
        this.evidenceSkill2 = evidenceSkill2;
    }

    public String getEvidenceType2() {
        return evidenceType2;
    }

    public void setEvidenceType2(String evidenceType2) {
        this.evidenceType2 = evidenceType2;
    }

    public String getEvidenceProvider2() {
        return evidenceProvider2;
    }

    public void setEvidenceProvider2(String evidenceProvider2) {
        this.evidenceProvider2 = evidenceProvider2;
    }

    public String getEvidenceUrl2() {
        return evidenceUrl2;
    }

    public void setEvidenceUrl2(String evidenceUrl2) {
        this.evidenceUrl2 = evidenceUrl2;
    }

    public String getEvidenceSkill3() {
        return evidenceSkill3;
    }

    public void setEvidenceSkill3(String evidenceSkill3) {
        this.evidenceSkill3 = evidenceSkill3;
    }

    public String getEvidenceType3() {
        return evidenceType3;
    }

    public void setEvidenceType3(String evidenceType3) {
        this.evidenceType3 = evidenceType3;
    }

    public String getEvidenceProvider3() {
        return evidenceProvider3;
    }

    public void setEvidenceProvider3(String evidenceProvider3) {
        this.evidenceProvider3 = evidenceProvider3;
    }

    public String getEvidenceUrl3() {
        return evidenceUrl3;
    }

    public void setEvidenceUrl3(String evidenceUrl3) {
        this.evidenceUrl3 = evidenceUrl3;
    }
}