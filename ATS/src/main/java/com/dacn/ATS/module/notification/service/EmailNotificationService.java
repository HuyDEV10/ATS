package com.dacn.ATS.module.notification.service;

import com.dacn.ATS.module.candidate.entity.Candidate;
import com.dacn.ATS.module.job.entity.Job;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class EmailNotificationService {

    private static final Logger log = LoggerFactory.getLogger(EmailNotificationService.class);

    private final ObjectProvider<JavaMailSender> mailSenderProvider;

    @Value("${spring.mail.username:}")
    private String fromEmail;

    public EmailNotificationService(ObjectProvider<JavaMailSender> mailSenderProvider) {
        this.mailSenderProvider = mailSenderProvider;
    }

    public void sendApplicationStatusEmail(Candidate candidate, Job job, String newStatus) {
        if (candidate == null || !StringUtils.hasText(candidate.getEmail())) {
            return;
        }

        String subject = buildSubject(newStatus, job);
        String content = buildContent(candidate, job, newStatus);

        send(candidate.getEmail(), subject, content);
    }

    public void send(String to, String subject, String content) {
        if (!StringUtils.hasText(to)) {
            return;
        }

        if (!StringUtils.hasText(fromEmail)) {
            log.warn("Skip email because GMAIL_USERNAME is not configured");
            return;
        }

        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();

        if (mailSender == null) {
            log.warn("Skip email because JavaMailSender is not available");
            return;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();

            message.setFrom(fromEmail);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(content);

            mailSender.send(message);
        } catch (Exception e) {
            log.error("Cannot send email to {}: {}", to, e.getMessage());
        }
    }

    private String buildSubject(String newStatus, Job job) {
        String jobTitle = job != null
                ? job.getTitle()
                : "your application";

        return switch (newStatus) {
            case "SHORTLISTED" -> "Your application has been shortlisted - " + jobTitle;
            case "INTERVIEW_SCHEDULED" -> "Interview invitation - " + jobTitle;
            case "OFFERED" -> "Offer update - " + jobTitle;
            case "REJECTED" -> "Application result - " + jobTitle;
            default -> "Application status updated - " + jobTitle;
        };
    }

    private String buildContent(Candidate candidate, Job job, String newStatus) {
        String name = StringUtils.hasText(candidate.getName())
                ? candidate.getName()
                : "Candidate";

        String jobTitle = job != null
                ? job.getTitle()
                : "the position you applied for";

        return "Dear " + name + ",\n\n"
                + "Your application for " + jobTitle
                + " has been updated to: " + newStatus + ".\n\n"
                + "Best regards,\n"
                + "SmartATS Recruitment Team";
    }
}