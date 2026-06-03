package com.dacn.ATS.module.ai.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("ai_decision_reviews")
public class AiDecisionReview {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long companyId;
    private Long applicationId;
    private Long applicationScoreId;

    private String aiRecommendation;
    private String decision;
    private String reason;

    private Long reviewedBy;
    private LocalDateTime reviewedAt;

    private LocalDateTime createTime;

    @TableLogic
    private Integer deleted;
}