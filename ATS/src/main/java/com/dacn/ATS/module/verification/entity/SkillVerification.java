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
    private String certificateUrl;
    private String evidenceText;
    private String status;
    private LocalDateTime verifiedAt;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    @TableLogic
    private Integer deleted;
}