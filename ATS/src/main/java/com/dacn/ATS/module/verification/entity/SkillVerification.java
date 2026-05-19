package com.dacn.ATS.module.verification.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("skill_verifications")
public class SkillVerification {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long candidateId;
    private String skillName;
    private String provider;
    private String sourceType;
    private String sourceName;
    private String certificateUrl;
    private String sourceUrl;
    private String artifactPath;
    private String evidenceText;
    private String status;
    private Integer confidenceScore;
    private String extractedSkills;
    private String evidenceSummary;
    private String trustSignals;
    private String riskSignals;
    private LocalDateTime lastAnalyzedAt;
    private LocalDateTime verifiedAt;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    @TableLogic
    private Integer deleted;
}