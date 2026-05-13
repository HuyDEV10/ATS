package com.dacn.ATS.module.ai.service;

import com.dacn.ATS.module.ai.dto.AiScoringPrompt;
import com.dacn.ATS.module.candidate.entity.Candidate;
import com.dacn.ATS.module.job.entity.Job;

public interface AiScoringPromptBuilder {
    AiScoringPrompt build(Job job, Candidate candidate, String cvText);
}