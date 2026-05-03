package com.dacn.ATS.module.job.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.dacn.ATS.module.job.entity.Job;

import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface JobMapper extends BaseMapper<Job> {
    // Có thể thêm query đặc biệt nếu cần
}