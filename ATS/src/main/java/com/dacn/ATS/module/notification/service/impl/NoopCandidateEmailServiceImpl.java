package com.dacn.ATS.module.notification.service.impl;

import com.dacn.ATS.module.application.entity.JobApplication;
import com.dacn.ATS.module.candidate.entity.Candidate;
import com.dacn.ATS.module.job.entity.Job;
import com.dacn.ATS.module.notification.service.CandidateEmailService;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "ats.mail.enabled", havingValue = "false", matchIfMissing = true)
public class NoopCandidateEmailServiceImpl implements CandidateEmailService {

    @Override
    public void sendApplicationCreatedEmail(Candidate candidate, Job job, JobApplication application) {
        // Mail disabled
    }

    @Override
    public void sendApplicationStatusChangedEmail(
            Candidate candidate,
            Job job,
            JobApplication application,
            String oldStatus,
            String newStatus,
            String hrNotes) {
        // Mail disabled
    }
}