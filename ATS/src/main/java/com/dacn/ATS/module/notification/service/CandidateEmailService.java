package com.dacn.ATS.module.notification.service;

import com.dacn.ATS.module.application.entity.JobApplication;
import com.dacn.ATS.module.candidate.entity.Candidate;
import com.dacn.ATS.module.interview.entity.InterviewRecord;
import com.dacn.ATS.module.job.entity.Job;

public interface CandidateEmailService {

    void sendApplicationCreatedEmail(Candidate candidate, Job job, JobApplication application);

    void sendApplicationStatusChangedEmail(
            Candidate candidate,
            Job job,
            JobApplication application,
            String oldStatus,
            String newStatus,
            String hrNotes);

    void sendInterviewInvitationEmail(
            Candidate candidate,
            Job job,
            InterviewRecord interview);
}