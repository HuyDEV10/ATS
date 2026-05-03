package com.dacn.ATS.module.candidate.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.dacn.ATS.module.candidate.entity.Candidate;

import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CandidateMapper extends BaseMapper<Candidate> {
}