package com.dacn.ATS.module.interview.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dacn.ATS.common.enums.ResultCodeEnum;
import com.dacn.ATS.exception.BusinessException;
import com.dacn.ATS.module.application.entity.JobApplication;
import com.dacn.ATS.module.application.enums.ApplicationStatus;
import com.dacn.ATS.module.application.mapper.JobApplicationMapper;
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

    @Autowired
    private InterviewRecordMapper interviewMapper;

    @Autowired
    private JobApplicationMapper applicationMapper;

    @Override
    public Page<InterviewRecord> pageMyInterviews(int page, int size, Long interviewerId) {
        return pageInterviews(page, size, null, interviewerId);
    }

    @Override
    public InterviewRecord scheduleInterview(InterviewRecord interview) {
        JobApplication application = applicationMapper.selectById(interview.getApplicationId());

        if (application == null) {
            throw new BusinessException(ResultCodeEnum.NOT_FOUND, "Application not found");
        }

        interview.setId(null);
        interview.setCompanyId(application.getCompanyId());
        interview.setStatus("SCHEDULED");
        interview.setCreateTime(LocalDateTime.now());
        interview.setUpdateTime(LocalDateTime.now());
        interview.setDeleted(0);

        interviewMapper.insert(interview);

        updateApplicationStatusIfAllowed(application, ApplicationStatus.INTERVIEW_SCHEDULED);

        return interview;
    }

    @Override
    public InterviewRecord updateInterviewResult(InterviewRecord interview) {
        InterviewRecord existing = getInterviewById(interview.getId());

        if (existing == null) {
            throw new BusinessException(ResultCodeEnum.NOT_FOUND, "Interview not found");
        }

        if (interview.getStatus() == null) {
            interview.setStatus(existing.getStatus());
        }

        interview.setApplicationId(existing.getApplicationId());
        interview.setCompanyId(existing.getCompanyId());
        interview.setInterviewerId(existing.getInterviewerId());
        interview.setInterviewDate(existing.getInterviewDate());
        interview.setRound(existing.getRound());
        interview.setMeetingLink(existing.getMeetingLink());
        interview.setLocation(existing.getLocation());
        interview.setNotes(existing.getNotes());
        interview.setUpdateTime(LocalDateTime.now());

        interviewMapper.updateById(interview);

        if ("COMPLETED".equals(interview.getStatus())) {
            JobApplication application = applicationMapper.selectById(existing.getApplicationId());
            updateApplicationStatusIfAllowed(application, ApplicationStatus.INTERVIEWED);
        }

        return interviewMapper.selectById(interview.getId());
    }

    @Override
    public void cancelInterview(Long id) {
        InterviewRecord interview = getInterviewById(id);

        if (interview == null) {
            return;
        }

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

        if (StringUtils.hasText(feedback)) {
            interview.setFeedback(feedback);
        }

        if (score != null) {
            interview.setScore(score);
        }

        if (StringUtils.hasText(recommendation)) {
            interview.setRecommendation(recommendation);
        }

        interview.setUpdateTime(LocalDateTime.now());
        interviewMapper.updateById(interview);

        JobApplication application = applicationMapper.selectById(interview.getApplicationId());
        updateApplicationStatusIfAllowed(application, ApplicationStatus.INTERVIEWED);

        return true;
    }

    private void updateApplicationStatusIfAllowed(JobApplication application, ApplicationStatus nextStatus) {
        if (application == null || application.getStatus() == null || nextStatus == null) {
            return;
        }

        try {
            ApplicationStatus currentStatus = ApplicationStatus.valueOf(application.getStatus());

            if (currentStatus.canTransitionTo(nextStatus)) {
                application.setStatus(nextStatus.name());
                application.setUpdateTime(LocalDateTime.now());
                applicationMapper.updateById(application);
            }
        } catch (Exception ignored) {
            // Keep interview flow safe even if application status is not valid.
        }
    }
}