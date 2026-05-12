package com.dacn.ATS.module.ai.service;

import com.dacn.ATS.module.ai.dto.CvScoreResult;

public interface AiScoringService {
    CvScoreResult scoreCv(Long applicationId);
}