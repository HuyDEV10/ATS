package com.dacn.ATS.module.verification.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class VerifiedSkillProfile {
    private Long candidateId;
    private int totalEvidence;
    private int verifiedEvidence;
    private int averageConfidence;
    private List<VerifiedSkillItem> skills = new ArrayList<>();
}