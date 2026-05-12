package com.dacn.ATS.module.ai.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("application_scores")
public class ApplicationScore {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long applicationId;
    private Long jobId;
    private Long candidateId;
    private Long resumeId;

    private Integer overallScore;
    private Integer skillScore;
    private Integer experienceScore;
    private Integer keywordScore;

    private String matchedSkills;
    private String missingSkills;
    private String strengths;
    private String weaknesses;
    private String recommendation;
    private String interviewQuestions;

    private LocalDateTime scoreTime;

    @TableLogic
    private Integer deleted;
}