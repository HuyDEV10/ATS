package com.dacn.ATS.module.notification.service.impl;

import com.dacn.ATS.module.application.entity.JobApplication;
import com.dacn.ATS.module.candidate.entity.Candidate;
import com.dacn.ATS.module.interview.entity.InterviewRecord;
import com.dacn.ATS.module.job.entity.Job;
import com.dacn.ATS.module.notification.service.CandidateEmailService;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.format.DateTimeFormatter;

@Service
@ConditionalOnProperty(name = "ats.mail.enabled", havingValue = "true")
public class CandidateEmailServiceImpl implements CandidateEmailService {

    private final JavaMailSender mailSender;

    @Value("${ats.mail.from:}")
    private String from;

    @Value("${ats.mail.company-name:SmartATS Recruitment}")
    private String companyName;

    public CandidateEmailServiceImpl(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Override
    public void sendApplicationCreatedEmail(Candidate candidate, Job job, JobApplication application) {
        if (!canSend(candidate)) {
            return;
        }

        String subject = "[SmartATS] Application received - " + safe(job.getTitle());

        String body = """
                Dear %s,

                Thank you for applying to the position: %s.

                We have received your application and our HR team will review it soon.

                Current application status: %s

                Best regards,
                %s
                """.formatted(
                safe(candidate.getName()),
                safe(job.getTitle()),
                safe(application.getStatus()),
                companyName);

        send(candidate.getEmail(), subject, body);
    }

    @Override
    public void sendApplicationStatusChangedEmail(
            Candidate candidate,
            Job job,
            JobApplication application,
            String oldStatus,
            String newStatus,
            String hrNotes) {

        if (!canSend(candidate)) {
            return;
        }

        String subject = "[SmartATS] Application status updated - " + safe(job.getTitle());

        StringBuilder body = new StringBuilder();

        body.append("Dear ").append(safe(candidate.getName())).append(",\n\n");
        body.append("Your application for the position \"")
                .append(safe(job.getTitle()))
                .append("\" has been updated.\n\n");

        body.append("Previous status: ").append(safe(oldStatus)).append("\n");
        body.append("Current status: ").append(safe(newStatus)).append("\n");

        if (StringUtils.hasText(hrNotes)) {
            body.append("\nHR note:\n").append(hrNotes).append("\n");
        }

        body.append("\nBest regards,\n").append(companyName);

        send(candidate.getEmail(), subject, body.toString());
    }

    @Override
    public void sendInterviewInvitationEmail(Candidate candidate, Job job, InterviewRecord interview) {
        if (!canSend(candidate)) {
            return;
        }

        String subject = "[SmartATS] Interview Invitation - " + safe(job.getTitle());

        String interviewTime = interview.getInterviewDate() == null
                ? "To be updated"
                : interview.getInterviewDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));

        StringBuilder body = new StringBuilder();

        body.append("Dear ").append(safe(candidate.getName())).append(",\n\n");
        body.append("You are invited to an interview for the position: ")
                .append(safe(job.getTitle()))
                .append(".\n\n");

        body.append("Interview time: ").append(interviewTime).append("\n");

        if (StringUtils.hasText(interview.getLocation())) {
            body.append("Location: ").append(interview.getLocation()).append("\n");
        }

        if (StringUtils.hasText(interview.getMeetingLink())) {
            body.append("Meeting link: ").append(interview.getMeetingLink()).append("\n");
        }

        if (StringUtils.hasText(interview.getNotes())) {
            body.append("\nNotes:\n").append(interview.getNotes()).append("\n");
        }

        body.append("\nPlease prepare before joining the interview.\n\n");
        body.append("Best regards,\n").append(companyName);

        send(candidate.getEmail(), subject, body.toString());
    }

    private void send(String to, String subject, String body) {
        SimpleMailMessage message = new SimpleMailMessage();

        if (StringUtils.hasText(from)) {
            message.setFrom(from);
        }

        message.setTo(to);
        message.setSubject(subject);
        message.setText(body);

        mailSender.send(message);
    }

    private boolean canSend(Candidate candidate) {
        return candidate != null && StringUtils.hasText(candidate.getEmail());
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}