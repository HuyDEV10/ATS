package com.dacn.ATS.module.ai.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("application_score_details")
public class ApplicationScoreDetail {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long applicationScoreId;
    private String criterion;
    private Integer weight;
    private Integer score;
    private String evidence;
    private String explanation;
    private LocalDateTime createTime;
    @TableLogic
    private Integer deleted;
}