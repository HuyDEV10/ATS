package com.dacn.ATS.module.ai.service.impl;

import com.dacn.ATS.module.ai.dto.AiScoringPrompt;
import com.dacn.ATS.module.ai.service.AiScoringPromptBuilder;
import com.dacn.ATS.module.candidate.entity.Candidate;
import com.dacn.ATS.module.job.entity.Job;
import org.springframework.stereotype.Service;

@Service
public class DefaultAiScoringPromptBuilder implements AiScoringPromptBuilder {
    public static final String PROMPT_VERSION = "CV_SCORE_PROMPT_V1";

    @Override
    public AiScoringPrompt build(Job job, Candidate candidate, String cvText) {
        String systemPrompt = "Bạn là chuyên gia tuyển dụng kỹ thuật. "
                + "Chỉ đánh giá dựa trên bằng chứng trong JD và CV, không suy đoán tuổi/giới tính/địa chỉ.";
        String userPrompt = "JOB_TITLE:\n" + safe(job.getTitle())
                + "\n\nJOB_DESCRIPTION:\n" + safe(job.getDescription())
                + "\n\nJOB_DEPARTMENT:\n" + safe(job.getDepartment())
                + "\n\nCANDIDATE_DECLARED_SKILLS:\n" + safe(candidate.getSkills())
                + "\n\nCANDIDATE_EXPERIENCE_YEARS:\n" + safe(candidate.getExperienceYears())
                + "\n\nCV_TEXT:\n" + truncate(safe(cvText), 12_000);
        String responseSchema = """
                Return ONLY valid JSON, no markdown, no explanation outside JSON.
                JSON schema:
                {
                  "overallScore": 0,
                  "skillScore": 0,
                  "experienceScore": 0,
                  "keywordScore": 0,
                  "matchedSkills": ["skill"],
                  "missingSkills": ["skill"],
                  "strengths": ["short evidence-based strength"],
                  "weaknesses": ["short evidence-based weakness"],
                  "recommendation": "STRONG_MATCH | POTENTIAL_MATCH | WEAK_MATCH | NOT_MATCH - short reason",
                  "interviewQuestions": ["question"]
                }

                Scoring rules:
                - all scores must be integers from 0 to 100
                - skillScore measures required skill match
                - experienceScore measures relevant years and project evidence
                - keywordScore measures JD keyword coverage
                - overallScore must be weighted: skillScore 45%, keywordScore 35%, experienceScore 20%
                - matchedSkills and missingSkills must be based only on JD and CV evidence
                - do not infer age, gender, ethnicity, marital status, address, or other sensitive attributes
                """;
        return new AiScoringPrompt(PROMPT_VERSION, systemPrompt, userPrompt, responseSchema);
    }

    private String safe(Object value) {
        return value == null ? "" : value.toString();
    }

    private String truncate(String value, int maxLength) {
        if (value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }
}