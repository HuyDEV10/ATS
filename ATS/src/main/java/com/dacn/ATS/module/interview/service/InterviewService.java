package com.dacn.ATS.module.interview.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dacn.ATS.module.interview.entity.InterviewRecord;

import java.util.List;

public interface InterviewService {
    InterviewRecord scheduleInterview(InterviewRecord interview);

    InterviewRecord updateInterviewResult(InterviewRecord interview);

    void cancelInterview(Long id);

    InterviewRecord getInterviewById(Long id);

    Page<InterviewRecord> pageInterviews(int page, int size, Long applicationId, Long interviewerId);

    Page<InterviewRecord> pageMyInterviews(int page, int size, Long interviewerId);

    List<InterviewRecord> listByApplicationId(Long applicationId);

    boolean completeInterview(Long id, String feedback, Integer score, String recommendation);
}
