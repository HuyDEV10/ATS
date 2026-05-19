package com.dacn.ATS.module.verification.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class SkillMatchResult {
    private Long candidateId;
    private Long jobId;
    private int matchScore;
    private int verifiedCoverageScore;
    private List<String> matchedVerifiedSkills = new ArrayList<>();
    private List<String> missingSkills = new ArrayList<>();
    private String explanation;
}