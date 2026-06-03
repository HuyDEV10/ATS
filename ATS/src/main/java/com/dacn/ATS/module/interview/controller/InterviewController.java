package com.dacn.ATS.module.interview.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dacn.ATS.common.util.CurrentUserUtil;
import com.dacn.ATS.module.application.entity.JobApplication;
import com.dacn.ATS.module.application.service.JobApplicationService;
import com.dacn.ATS.module.auth.service.UserService;
import com.dacn.ATS.module.candidate.entity.Candidate;
import com.dacn.ATS.module.candidate.service.CandidateService;
import com.dacn.ATS.module.interview.entity.InterviewRecord;
import com.dacn.ATS.module.interview.service.InterviewService;
import com.dacn.ATS.module.job.entity.Job;
import com.dacn.ATS.module.job.service.JobService;
import com.dacn.ATS.module.notification.service.CandidateEmailService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/interviews")
public class InterviewController {

    @Autowired
    private UserService userService;

    @Autowired
    private InterviewService interviewService;

    @Autowired
    private JobApplicationService applicationService;

    @Autowired
    private JobService jobService;

    @Autowired
    private CandidateService candidateService;

    @Autowired
    private CandidateEmailService candidateEmailService;

    private Long getCurrentUserId() {
        return CurrentUserUtil.getCurrentUserId();
    }

    @GetMapping
    public String listInterviews(@RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Long applicationId,
            Model model) {

        Page<InterviewRecord> interviewPage = interviewService.pageInterviews(page, size, applicationId, null);

        model.addAttribute("interviewPage", interviewPage);
        model.addAttribute("applicationId", applicationId);

        return "interview/list";
    }

    @GetMapping("/create")
    public String showCreateForm(@RequestParam Long applicationId, Model model) {
        InterviewRecord interview = new InterviewRecord();
        interview.setApplicationId(applicationId);

        model.addAttribute("interview", interview);

        return "interview/form";
    }

    @GetMapping("/my")
    public String myInterviews(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            Model model) {

        Long interviewerId = getCurrentUserId();

        Page<InterviewRecord> interviewPage = interviewService.pageMyInterviews(page, size, interviewerId);

        model.addAttribute("interviewPage", interviewPage);

        return "interview/my";
    }

    @PostMapping("/create")
    public String scheduleInterview(
            @ModelAttribute InterviewRecord interview,
            RedirectAttributes redirectAttributes) {

        if (interview.getInterviewerId() == null) {
            interview.setInterviewerId(getCurrentUserId());
        }

        InterviewRecord saved = interviewService.scheduleInterview(interview);

        sendInterviewInvitationIfPossible(saved, redirectAttributes);

        redirectAttributes.addFlashAttribute("success", "Interview scheduled");

        return "redirect:/interviews?applicationId=" + saved.getApplicationId();
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Long id, Model model) {
        InterviewRecord interview = interviewService.getInterviewById(id);
        model.addAttribute("interview", interview);
        return "interview/form";
    }

    @PostMapping("/edit/{id}")
    public String updateInterview(
            @PathVariable Long id,
            @ModelAttribute InterviewRecord interview,
            @RequestParam(required = false) Integer technicalScore,
            @RequestParam(required = false) Integer communicationScore,
            @RequestParam(required = false) Integer problemSolvingScore,
            @RequestParam(required = false) Integer experienceFitScore,
            @RequestParam(required = false) Integer attitudeScore,
            @RequestParam(required = false) String finalRecommendation,
            @RequestParam(required = false) String scorecardNote,
            RedirectAttributes redirectAttributes) {

        interview.setId(id);

        String scorecardFeedback = buildScorecardFeedback(
                technicalScore,
                communicationScore,
                problemSolvingScore,
                experienceFitScore,
                attitudeScore,
                scorecardNote,
                interview.getFeedback());

        Integer averageScore = calculateAverageScore(
                technicalScore,
                communicationScore,
                problemSolvingScore,
                experienceFitScore,
                attitudeScore);

        if (StringUtils.hasText(scorecardFeedback)) {
            interview.setFeedback(scorecardFeedback);
        }

        if (averageScore != null) {
            interview.setScore(averageScore);
        }

        if (StringUtils.hasText(finalRecommendation)) {
            interview.setRecommendation(finalRecommendation);
        }

        interviewService.updateInterviewResult(interview);

        redirectAttributes.addFlashAttribute("success", "Interview scorecard updated");

        return "redirect:/interviews?applicationId=" + interview.getApplicationId();
    }

    @GetMapping("/assign")
    public String assignForm(@RequestParam Long applicationId, Model model) {
        model.addAttribute("applicationId", applicationId);

        var interviewers = userService.findAllUsers()
                .stream()
                .filter(u -> "INTERVIEWER".equals(u.getRole()))
                .toList();

        model.addAttribute("interviewers", interviewers);

        return "interview/assign";
    }

    @PostMapping("/complete/{id}")
    public String completeInterview(@PathVariable Long id,
            @RequestParam String feedback,
            @RequestParam(required = false) Integer score,
            @RequestParam(required = false) String recommendation,
            RedirectAttributes redirectAttributes) {

        boolean ok = interviewService.completeInterview(id, feedback, score, recommendation);

        if (ok) {
            redirectAttributes.addFlashAttribute("success", "Interview completed");
        } else {
            redirectAttributes.addFlashAttribute("error", "Cannot complete interview");
        }

        InterviewRecord interview = interviewService.getInterviewById(id);

        return "redirect:/interviews?applicationId=" + (interview != null ? interview.getApplicationId() : "");
    }

    @GetMapping("/cancel/{id}")
    public String cancelInterview(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        InterviewRecord interview = interviewService.getInterviewById(id);

        if (interview != null) {
            interviewService.cancelInterview(id);
            redirectAttributes.addFlashAttribute("success", "Interview cancelled");
            return "redirect:/interviews?applicationId=" + interview.getApplicationId();
        }

        return "redirect:/interviews";
    }

    @GetMapping("/view/{id}")
    public String viewInterview(@PathVariable Long id, Model model) {
        InterviewRecord interview = interviewService.getInterviewById(id);
        model.addAttribute("interview", interview);
        return "interview/view";
    }

    private void sendInterviewInvitationIfPossible(
            InterviewRecord interview,
            RedirectAttributes redirectAttributes) {

        try {
            JobApplication application = applicationService.getApplicationById(interview.getApplicationId());
            Job job = jobService.getJobById(application.getJobId());
            Candidate candidate = candidateService.getCandidateById(application.getCandidateId());

            candidateEmailService.sendInterviewInvitationEmail(candidate, job, interview);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute(
                    "warning",
                    "Interview scheduled, but invitation email could not be sent: " + e.getMessage());
        }
    }

    private String buildScorecardFeedback(
            Integer technicalScore,
            Integer communicationScore,
            Integer problemSolvingScore,
            Integer experienceFitScore,
            Integer attitudeScore,
            String scorecardNote,
            String oldFeedback) {

        StringBuilder builder = new StringBuilder();

        builder.append("[Interview Scorecard]\n");
        builder.append("Technical Skill: ").append(displayScore(technicalScore)).append("/5\n");
        builder.append("Communication: ").append(displayScore(communicationScore)).append("/5\n");
        builder.append("Problem Solving: ").append(displayScore(problemSolvingScore)).append("/5\n");
        builder.append("Experience Fit: ").append(displayScore(experienceFitScore)).append("/5\n");
        builder.append("Attitude: ").append(displayScore(attitudeScore)).append("/5\n");

        if (StringUtils.hasText(scorecardNote)) {
            builder.append("\nScorecard note:\n").append(scorecardNote.trim()).append("\n");
        }

        if (StringUtils.hasText(oldFeedback)) {
            builder.append("\nGeneral feedback:\n").append(oldFeedback.trim()).append("\n");
        }

        return builder.toString();
    }

    private Integer calculateAverageScore(Integer... scores) {
        int total = 0;
        int count = 0;

        for (Integer score : scores) {
            if (score != null) {
                total += score;
                count++;
            }
        }

        if (count == 0) {
            return null;
        }

        return Math.round((total * 10.0f) / (count * 5.0f));
    }

    private String displayScore(Integer score) {
        return score == null ? "-" : score.toString();
    }
}