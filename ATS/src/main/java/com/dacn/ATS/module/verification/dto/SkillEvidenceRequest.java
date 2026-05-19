package com.dacn.ATS.module.verification.dto;

import com.dacn.ATS.module.verification.enums.VerificationSourceType;
import lombok.Data;

@Data
public class SkillEvidenceRequest {
    private Long candidateId;
    private String declaredSkill;
    private VerificationSourceType sourceType;
    private String sourceName;
    private String sourceUrl;
    private String evidenceText;
}