package com.dacn.ATS.module.resume.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("resumes")
public class Resume {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String fileName;
    private String filePath;
    private String fileHash;
    private Long uploadedBy;
    private LocalDateTime uploadTime;
    private String parseStatus; // PENDING, PARSED, FAILED
    @TableLogic
    private Integer deleted;
}