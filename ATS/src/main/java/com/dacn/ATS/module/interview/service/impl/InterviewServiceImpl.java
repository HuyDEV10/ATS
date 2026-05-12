package com.dacn.ATS.module.interview.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dacn.ATS.common.enums.ResultCodeEnum;
import com.dacn.ATS.exception.BusinessException;
import com.dacn.ATS.module.interview.entity.InterviewRecord;
import com.dacn.ATS.module.interview.mapper.InterviewRecordMapper;
import com.dacn.ATS.module.interview.service.InterviewService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class InterviewServiceImpl implements InterviewService {
    @Override
    public Page<InterviewRecord> pageMyInterviews(int page, int size, Long interviewerId) {
        return pageInterviews(page, size, null, interviewerId);
    }

    @Autowired
    private InterviewRecordMapper interviewMapper;

    @Override
    public InterviewRecord scheduleInterview(InterviewRecord interview) {
        interview.setId(null);
        interview.setStatus("SCHEDULED");
        interview.setCreateTime(LocalDateTime.now());
        interview.setUpdateTime(LocalDateTime.now());
        interview.setDeleted(0);
        interviewMapper.insert(interview);
        return interview;
    }

    @Override
    public InterviewRecord updateInterviewResult(InterviewRecord interview) {
        InterviewRecord existing = getInterviewById(interview.getId());
        if (existing == null) {
            throw new BusinessException(ResultCodeEnum.NOT_FOUND, "Interview not found");
        }
        // Chỉ cho phép update nếu status là SCHEDULED
        if (!"SCHEDULED".equals(existing.getStatus())) {
            throw new BusinessException(ResultCodeEnum.BAD_REQUEST, "Only scheduled interviews can be updated");
        }
        interview.setUpdateTime(LocalDateTime.now());
        interviewMapper.updateById(interview);
        return interviewMapper.selectById(interview.getId());
    }

    @Override
    public void cancelInterview(Long id) {
        InterviewRecord interview = getInterviewById(id);
        if (interview == null)
            return;
        interview.setStatus("CANCELLED");
        interview.setUpdateTime(LocalDateTime.now());
        interviewMapper.updateById(interview);
    }

    @Override
    public InterviewRecord getInterviewById(Long id) {
        return interviewMapper.selectById(id);
    }

    @Override
    public Page<InterviewRecord> pageInterviews(int page, int size, Long applicationId, Long interviewerId) {
        Page<InterviewRecord> pageObj = new Page<>(page, size);
        LambdaQueryWrapper<InterviewRecord> wrapper = new LambdaQueryWrapper<>();
        if (applicationId != null) {
            wrapper.eq(InterviewRecord::getApplicationId, applicationId);
        }
        if (interviewerId != null) {
            wrapper.eq(InterviewRecord::getInterviewerId, interviewerId);
        }
        wrapper.orderByDesc(InterviewRecord::getInterviewDate);
        return interviewMapper.selectPage(pageObj, wrapper);
    }

    @Override
    public List<InterviewRecord> listByApplicationId(Long applicationId) {
        LambdaQueryWrapper<InterviewRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(InterviewRecord::getApplicationId, applicationId);
        wrapper.orderByAsc(InterviewRecord::getRound);
        return interviewMapper.selectList(wrapper);
    }

    @Override
    public boolean completeInterview(Long id, String feedback, Integer score, String recommendation) {
        InterviewRecord interview = getInterviewById(id);
        if (interview == null || !"SCHEDULED".equals(interview.getStatus())) {
            return false;
        }
        interview.setStatus("COMPLETED");
        if (StringUtils.hasText(feedback))
            interview.setFeedback(feedback);
        if (score != null)
            interview.setScore(score);
        if (StringUtils.hasText(recommendation))
            interview.setRecommendation(recommendation);
        interview.setUpdateTime(LocalDateTime.now());
        interviewMapper.updateById(interview);
        return true;
    }
}
