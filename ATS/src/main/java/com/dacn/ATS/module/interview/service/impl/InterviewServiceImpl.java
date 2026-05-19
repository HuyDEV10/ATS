package com.dacn.ATS.module.interview.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dacn.ATS.common.enums.ResultCodeEnum;
import com.dacn.ATS.exception.BusinessException;
import com.dacn.ATS.module.application.entity.JobApplication;
import com.dacn.ATS.module.application.enums.ApplicationStatus;
import com.dacn.ATS.module.application.mapper.JobApplicationMapper;
import com.dacn.ATS.module.auth.entity.User;
import com.dacn.ATS.module.auth.mapper.UserMapper;
import com.dacn.ATS.module.interview.entity.InterviewRecord;
import com.dacn.ATS.module.interview.enums.InterviewStatus;
import com.dacn.ATS.module.interview.mapper.InterviewRecordMapper;
import com.dacn.ATS.module.interview.service.InterviewService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class InterviewServiceImpl implements InterviewService {

    private static final int MIN_SCORE = 0;
    private static final int MAX_SCORE = 100;

    @Autowired
    private InterviewRecordMapper interviewMapper;

    @Autowired
    private JobApplicationMapper applicationMapper;

    @Autowired
    private UserMapper userMapper;

    @Override
    public Page<InterviewRecord> pageMyInterviews(int page, int size, Long interviewerId) {
        return pageInterviews(page, size, null, interviewerId);
    }

    @Override
    public InterviewRecord scheduleInterview(InterviewRecord interview) {
        validateScheduleInput(interview);
        validateApplicationCanSchedule(interview.getApplicationId());
        validateInterviewer(interview.getInterviewerId());
        ensureNoDuplicateRound(interview.getApplicationId(), interview.getRound(), null);
        ensureInterviewerIsAvailable(interview.getInterviewerId(), interview.getInterviewDate(), null);

        interview.setId(null);
        interview.setStatus(InterviewStatus.SCHEDULED.name());
        interview.setCreateTime(LocalDateTime.now());
        interview.setUpdateTime(LocalDateTime.now());
        interview.setDeleted(0);

        interviewMapper.insert(interview);

        JobApplication application = applicationMapper.selectById(interview.getApplicationId());
        application.setStatus(ApplicationStatus.INTERVIEW_SCHEDULED.name());
        application.setUpdateTime(LocalDateTime.now());
        applicationMapper.updateById(application);

        return interview;
    }

    @Override
    public InterviewRecord updateInterviewResult(InterviewRecord interview) {
        InterviewRecord existing = getInterviewById(interview.getId());
        if (existing == null) {
            throw new BusinessException(ResultCodeEnum.NOT_FOUND, "Interview not found");
        }

        if (!InterviewStatus.SCHEDULED.name().equals(existing.getStatus())) {
            throw new BusinessException(ResultCodeEnum.BAD_REQUEST, "Only scheduled interviews can be updated");
        }

        validateScheduleInput(interview);
        validateInterviewer(interview.getInterviewerId());
        ensureNoDuplicateRound(interview.getApplicationId(), interview.getRound(), interview.getId());
        ensureInterviewerIsAvailable(interview.getInterviewerId(), interview.getInterviewDate(), interview.getId());

        interview.setStatus(existing.getStatus());
        interview.setCreateTime(existing.getCreateTime());
        interview.setUpdateTime(LocalDateTime.now());

        interviewMapper.updateById(interview);
        return interviewMapper.selectById(interview.getId());
    }

    @Override
    public void cancelInterview(Long id) {
        InterviewRecord interview = getInterviewById(id);
        if (interview == null) {
            throw new BusinessException(ResultCodeEnum.NOT_FOUND, "Interview not found");
        }

        if (!InterviewStatus.SCHEDULED.name().equals(interview.getStatus())) {
            throw new BusinessException(ResultCodeEnum.BAD_REQUEST, "Only scheduled interviews can be cancelled");
        }

        interview.setStatus(InterviewStatus.CANCELLED.name());
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
        if (interview == null) {
            return false;
        }

        if (!InterviewStatus.SCHEDULED.name().equals(interview.getStatus())) {
            throw new BusinessException(ResultCodeEnum.BAD_REQUEST, "Only scheduled interviews can be completed");
        }

        if (!StringUtils.hasText(feedback)) {
            throw new BusinessException(ResultCodeEnum.BAD_REQUEST, "Feedback is required");
        }

        validateScore(score);

        interview.setStatus(InterviewStatus.COMPLETED.name());
        interview.setFeedback(feedback);
        interview.setScore(score);

        if (StringUtils.hasText(recommendation)) {
            interview.setRecommendation(recommendation);
        }

        interview.setUpdateTime(LocalDateTime.now());
        interviewMapper.updateById(interview);

        JobApplication application = applicationMapper.selectById(interview.getApplicationId());
        if (application != null && ApplicationStatus.INTERVIEW_SCHEDULED.name().equals(application.getStatus())) {
            application.setStatus(ApplicationStatus.INTERVIEWED.name());
            application.setUpdateTime(LocalDateTime.now());
            applicationMapper.updateById(application);
        }

        return true;
    }

    private void validateScheduleInput(InterviewRecord interview) {
        if (interview.getApplicationId() == null) {
            throw new BusinessException(ResultCodeEnum.BAD_REQUEST, "Application is required");
        }

        if (interview.getInterviewerId() == null) {
            throw new BusinessException(ResultCodeEnum.BAD_REQUEST, "Interviewer is required");
        }

        if (interview.getInterviewDate() == null) {
            throw new BusinessException(ResultCodeEnum.BAD_REQUEST, "Interview date is required");
        }

        if (interview.getInterviewDate().isBefore(LocalDateTime.now())) {
            throw new BusinessException(ResultCodeEnum.BAD_REQUEST, "Interview date must be in the future");
        }

        if (interview.getRound() == null || interview.getRound() <= 0) {
            throw new BusinessException(ResultCodeEnum.BAD_REQUEST, "Interview round must be greater than 0");
        }

        if (!StringUtils.hasText(interview.getMeetingLink()) && !StringUtils.hasText(interview.getLocation())) {
            throw new BusinessException(ResultCodeEnum.BAD_REQUEST, "Meeting link or location is required");
        }
    }

    private void validateApplicationCanSchedule(Long applicationId) {
        JobApplication application = applicationMapper.selectById(applicationId);
        if (application == null) {
            throw new BusinessException(ResultCodeEnum.NOT_FOUND, "Application not found");
        }

        String status = application.getStatus();
        boolean canSchedule = ApplicationStatus.SHORTLISTED.name().equals(status)
                || ApplicationStatus.AI_SCREENED.name().equals(status)
                || ApplicationStatus.INTERVIEW_SCHEDULED.name().equals(status);

        if (!canSchedule) {
            throw new BusinessException(
                    ResultCodeEnum.BAD_REQUEST,
                    "Application status does not allow interview scheduling: " + status);
        }
    }

    private void validateInterviewer(Long interviewerId) {
        User interviewer = userMapper.selectById(interviewerId);
        if (interviewer == null) {
            throw new BusinessException(ResultCodeEnum.NOT_FOUND, "Interviewer not found");
        }

        if (!"INTERVIEWER".equalsIgnoreCase(interviewer.getRole())) {
            throw new BusinessException(ResultCodeEnum.BAD_REQUEST, "Selected user is not an interviewer");
        }
    }

    private void ensureNoDuplicateRound(Long applicationId, Integer round, Long excludeInterviewId) {
        LambdaQueryWrapper<InterviewRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(InterviewRecord::getApplicationId, applicationId)
                .eq(InterviewRecord::getRound, round)
                .ne(InterviewRecord::getStatus, InterviewStatus.CANCELLED.name());

        if (excludeInterviewId != null) {
            wrapper.ne(InterviewRecord::getId, excludeInterviewId);
        }

        Long count = interviewMapper.selectCount(wrapper);
        if (count != null && count > 0) {
            throw new BusinessException(ResultCodeEnum.BAD_REQUEST, "This interview round already exists");
        }
    }

    private void ensureInterviewerIsAvailable(Long interviewerId, LocalDateTime interviewDate,
            Long excludeInterviewId) {
        LocalDateTime from = interviewDate.minusMinutes(59);
        LocalDateTime to = interviewDate.plusMinutes(59);

        LambdaQueryWrapper<InterviewRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(InterviewRecord::getInterviewerId, interviewerId)
                .eq(InterviewRecord::getStatus, InterviewStatus.SCHEDULED.name())
                .between(InterviewRecord::getInterviewDate, from, to);

        if (excludeInterviewId != null) {
            wrapper.ne(InterviewRecord::getId, excludeInterviewId);
        }

        Long count = interviewMapper.selectCount(wrapper);
        if (count != null && count > 0) {
            throw new BusinessException(ResultCodeEnum.BAD_REQUEST,
                    "Interviewer already has another interview around this time");
        }
    }

    private void validateScore(Integer score) {
        if (score == null) {
            throw new BusinessException(ResultCodeEnum.BAD_REQUEST, "Score is required");
        }

        if (score < MIN_SCORE || score > MAX_SCORE) {
            throw new BusinessException(ResultCodeEnum.BAD_REQUEST, "Score must be between 0 and 100");
        }
    }
}