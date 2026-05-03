package com.dacn.ATS.module.application.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.dacn.ATS.module.application.entity.JobApplication;

import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface JobApplicationMapper extends BaseMapper<JobApplication> {
}
