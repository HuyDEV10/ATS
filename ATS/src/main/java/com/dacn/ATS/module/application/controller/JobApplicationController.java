package com.dacn.ATS.module.application.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dacn.ATS.common.util.CurrentUserUtil;
import com.dacn.ATS.module.ai.entity.AiDecisionReview;
import com.dacn.ATS.module.ai.entity.ApplicationScore;
import com.dacn.ATS.module.ai.entity.ApplicationScoreDetail;
import com.dacn.ATS.module.ai.mapper.AiDecisionReviewMapper;
import com.dacn.ATS.module.ai.mapper.ApplicationScoreDetailMapper;
import com.dacn.ATS.module.ai.mapper.ApplicationScoreMapper;
import com.dacn.ATS.module.application.entity.JobApplication;
import com.dacn.ATS.module.application.enums.ApplicationStatus;
import com.dacn.ATS.module.application.service.JobApplicationService;
import com.dacn.ATS.module.candidate.entity.Candidate;
import com.dacn.ATS.module.candidate.service.CandidateService;
import com.dacn.ATS.module.job.entity.Job;
import com.dacn.ATS.module.job.service.JobService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/applications")
public class JobApplicationController {

    @Autowired
    private JobApplicationService applicationService;

    @Autowired
    private JobService jobService;

    @Autowired
    private CandidateService candidateService;

    @Autowired
    private ApplicationScoreMapper applicationScoreMapper;

    @Autowired
    private ApplicationScoreDetailMapper applicationScoreDetailMapper;

    @Autowired
    private AiDecisionReviewMapper aiDecisionReviewMapper;

    @GetMapping
    public String listApplications(@RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Long jobId,
            @RequestParam(required = false) Long candidateId,
            @RequestParam(required = false) String status,
            Model model) {

        Page<JobApplication> appPage = applicationService.pageApplications(page, size, jobId, candidateId, status);

        model.addAttribute("appPage", appPage);
        model.addAttribute("jobId", jobId);
        model.addAttribute("candidateId", candidateId);
        model.addAttribute("status", status);
        model.addAttribute("jobs", jobService.pageJobs(1, 100, null).getRecords());
        model.addAttribute("candidates", candidateService.pageCandidates(1, 100, null).getRecords());

        return "application/list";
    }

    /**
     * Đợt 3:
     * Kanban board theo pipeline tuyển dụng.
     */
    @GetMapping("/kanban")
    public String kanban(
            @RequestParam(required = false) Long jobId,
            Model model) {

        List<JobApplication> applications = applicationService
                .pageApplications(1, 1000, jobId, null, null)
                .getRecords();

        Map<String, List<JobApplication>> board = new LinkedHashMap<>();

        for (ApplicationStatus status : ApplicationStatus.values()) {
            board.put(status.name(), new ArrayList<>());
        }

        for (JobApplication application : applications) {
            board.computeIfAbsent(application.getStatus(), key -> new ArrayList<>()).add(application);
        }

        Map<Long, String> candidateNameMap = new HashMap<>();
        Map<Long, String> candidateEmailMap = new HashMap<>();
        Map<Long, String> jobTitleMap = new HashMap<>();
        Map<Long, Integer> aiScoreMap = new HashMap<>();

        for (JobApplication application : applications) {
            if (application.getCandidateId() != null && !candidateNameMap.containsKey(application.getCandidateId())) {
                try {
                    Candidate candidate = candidateService.getCandidateById(application.getCandidateId());
                    candidateNameMap.put(application.getCandidateId(), candidate.getName());
                    candidateEmailMap.put(application.getCandidateId(), candidate.getEmail());
                } catch (Exception ignored) {
                    candidateNameMap.put(application.getCandidateId(), "Candidate #" + application.getCandidateId());
                    candidateEmailMap.put(application.getCandidateId(), "");
                }
            }

            if (application.getJobId() != null && !jobTitleMap.containsKey(application.getJobId())) {
                try {
                    Job job = jobService.getJobById(application.getJobId());
                    jobTitleMap.put(application.getJobId(), job.getTitle());
                } catch (Exception ignored) {
                    jobTitleMap.put(application.getJobId(), "Job #" + application.getJobId());
                }
            }

            ApplicationScore latestScore = getLatestScore(application.getId());
            if (latestScore != null && latestScore.getOverallScore() != null) {
                aiScoreMap.put(application.getId(), latestScore.getOverallScore());
            }
        }

        model.addAttribute("board", board);
        model.addAttribute("statuses", ApplicationStatus.values());
        model.addAttribute("candidateNameMap", candidateNameMap);
        model.addAttribute("candidateEmailMap", candidateEmailMap);
        model.addAttribute("jobTitleMap", jobTitleMap);
        model.addAttribute("aiScoreMap", aiScoreMap);
        model.addAttribute("jobs", jobService.pageJobs(1, 100, null).getRecords());
        model.addAttribute("selectedJobId", jobId);

        return "application/kanban";
    }

    @GetMapping("/create")
    public String showCreateForm(Model model) {
        model.addAttribute("application", new JobApplication());
        model.addAttribute("jobs", jobService.pageJobs(1, 100, null).getRecords());
        model.addAttribute("candidates", candidateService.pageCandidates(1, 100, null).getRecords());
        return "application/form";
    }

    @PostMapping("/create")
    public String createApplication(@ModelAttribute JobApplication application, RedirectAttributes redirectAttributes) {
        applicationService.createApplication(application);
        redirectAttributes.addFlashAttribute("success", "Application created successfully");
        return "redirect:/applications";
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Long id, Model model) {
        JobApplication application = applicationService.getApplicationById(id);
        model.addAttribute("application", application);
        model.addAttribute("jobs", jobService.pageJobs(1, 100, null).getRecords());
        model.addAttribute("candidates", candidateService.pageCandidates(1, 100, null).getRecords());
        return "application/form";
    }

    @PostMapping("/edit/{id}")
    public String updateApplication(@PathVariable Long id,
            @ModelAttribute JobApplication application,
            RedirectAttributes redirectAttributes) {

        application.setId(id);
        applicationService.updateApplication(application);

        redirectAttributes.addFlashAttribute("success", "Application updated");
        return "redirect:/applications/view/" + id;
    }

    @GetMapping("/delete/{id}")
    public String deleteApplication(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        applicationService.deleteApplication(id);
        redirectAttributes.addFlashAttribute("success", "Application deleted");
        return "redirect:/applications";
    }

    @GetMapping("/view/{id}")
    public String viewApplication(@PathVariable Long id, Model model) {
        var details = applicationService.getApplicationDetails(id);

        ApplicationScore latestScore = getLatestScore(id);
        List<ApplicationScoreDetail> scoreDetails = List.of();

        if (latestScore != null) {
            scoreDetails = applicationScoreDetailMapper.selectList(
                    new LambdaQueryWrapper<ApplicationScoreDetail>()
                            .eq(ApplicationScoreDetail::getApplicationScoreId, latestScore.getId())
                            .orderByAsc(ApplicationScoreDetail::getId));
        }

        AiDecisionReview latestAiReview = aiDecisionReviewMapper.selectOne(
                new LambdaQueryWrapper<AiDecisionReview>()
                        .eq(AiDecisionReview::getApplicationId, id)
                        .orderByDesc(AiDecisionReview::getReviewedAt)
                        .last("LIMIT 1"));

        model.addAttribute("details", details);
        model.addAttribute("latestScore", latestScore);
        model.addAttribute("scoreDetails", scoreDetails);
        model.addAttribute("latestAiReview", latestAiReview);

        return "application/view";
    }

    @PostMapping("/change-status/{id}")
    public String changeStatus(@PathVariable Long id,
            @RequestParam String status,
            @RequestParam(required = false) String hrNotes,
            @RequestParam(required = false) String redirectTo,
            RedirectAttributes redirectAttributes) {

        boolean ok = applicationService.changeStatus(id, status, hrNotes);

        if (ok) {
            redirectAttributes.addFlashAttribute("success", "Status changed to " + status);
        } else {
            redirectAttributes.addFlashAttribute("error", "Cannot change status");
        }

        if ("kanban".equals(redirectTo)) {
            return "redirect:/applications/kanban";
        }

        return "redirect:/applications/view/" + id;
    }

    @PostMapping("/{id}/verification-action")
    public String verificationAction(
            @PathVariable Long id,
            @RequestParam String action,
            @RequestParam(required = false) String note,
            RedirectAttributes redirectAttributes) {

        JobApplication existing = applicationService.getApplicationById(id);

        JobApplication application = new JobApplication();
        application.setId(id);

        String normalizedNote = StringUtils.hasText(note) ? note.trim() : "";

        switch (action) {
            case "CONFIRM_VERIFIED" -> {
                application.setVerificationStatus("VERIFIED");
                application.setMismatchScore(0);
                application.setMismatchSummary(
                        StringUtils.hasText(normalizedNote)
                                ? "HR confirmed CV/Form information. Note: " + normalizedNote
                                : "HR confirmed CV/Form information.");
                application.setHrNotes(appendHrNote(existing.getHrNotes(), "Verification confirmed", normalizedNote));
                redirectAttributes.addFlashAttribute("success", "CV/Form verification confirmed.");
            }

            case "REQUEST_UPDATE" -> {
                application.setVerificationStatus("NEEDS_REVIEW");
                application.setMismatchScore(50);
                application.setMismatchSummary(
                        StringUtils.hasText(normalizedNote)
                                ? "HR requested candidate to update or clarify information. Note: " + normalizedNote
                                : "HR requested candidate to update or clarify information.");
                application.setHrNotes(
                        appendHrNote(existing.getHrNotes(), "Verification requires update", normalizedNote));
                redirectAttributes.addFlashAttribute("success", "Verification marked as NEEDS_REVIEW.");
            }

            case "MARK_CONFLICT" -> {
                application.setVerificationStatus("IDENTITY_CONFLICT");
                application.setMismatchScore(90);
                application.setMismatchSummary(
                        StringUtils.hasText(normalizedNote)
                                ? "HR marked this application as identity/contact conflict. Note: " + normalizedNote
                                : "HR marked this application as identity/contact conflict.");
                application.setHrNotes(appendHrNote(existing.getHrNotes(), "Verification conflict", normalizedNote));
                redirectAttributes.addFlashAttribute("success", "Verification marked as IDENTITY_CONFLICT.");
            }

            default -> {
                redirectAttributes.addFlashAttribute("error", "Invalid verification action: " + action);
                return "redirect:/applications/view/" + id;
            }
        }

        applicationService.updateApplication(application);

        return "redirect:/applications/view/" + id;
    }

    @PostMapping("/{id}/ai-review")
    public String reviewAiRecommendation(
            @PathVariable Long id,
            @RequestParam String decision,
            @RequestParam(required = false) String reason,
            RedirectAttributes redirectAttributes) {

        JobApplication application = applicationService.getApplicationById(id);
        ApplicationScore latestScore = getLatestScore(id);

        if (latestScore == null) {
            redirectAttributes.addFlashAttribute("error",
                    "Please run AI Screening before reviewing AI recommendation.");
            return "redirect:/applications/view/" + id;
        }

        if (!isValidAiDecision(decision)) {
            redirectAttributes.addFlashAttribute("error", "Invalid AI review decision: " + decision);
            return "redirect:/applications/view/" + id;
        }

        AiDecisionReview review = new AiDecisionReview();
        review.setCompanyId(application.getCompanyId());
        review.setApplicationId(application.getId());
        review.setApplicationScoreId(latestScore.getId());
        review.setAiRecommendation(latestScore.getRecommendation());
        review.setDecision(decision);
        review.setReason(StringUtils.hasText(reason) ? reason.trim() : null);
        review.setReviewedBy(CurrentUserUtil.getCurrentUserId());
        review.setReviewedAt(LocalDateTime.now());
        review.setCreateTime(LocalDateTime.now());
        review.setDeleted(0);

        aiDecisionReviewMapper.insert(review);

        JobApplication update = new JobApplication();
        update.setId(id);
        update.setHrNotes(appendHrNote(
                application.getHrNotes(),
                "AI Review",
                buildAiReviewHrNote(decision, reason, latestScore.getRecommendation())));

        applicationService.updateApplication(update);

        redirectAttributes.addFlashAttribute("success", "AI recommendation reviewed: " + decision);
        return "redirect:/applications/view/" + id;
    }

    private ApplicationScore getLatestScore(Long applicationId) {
        return applicationScoreMapper.selectOne(
                new LambdaQueryWrapper<ApplicationScore>()
                        .eq(ApplicationScore::getApplicationId, applicationId)
                        .orderByDesc(ApplicationScore::getScoreTime)
                        .last("LIMIT 1"));
    }

    private boolean isValidAiDecision(String decision) {
        return "APPROVE_AI".equals(decision)
                || "REJECT_AI".equals(decision)
                || "OVERRIDE_TO_INTERVIEW".equals(decision)
                || "OVERRIDE_TO_REJECT".equals(decision);
    }

    private String buildAiReviewHrNote(String decision, String reason, String aiRecommendation) {
        StringBuilder builder = new StringBuilder();

        builder.append("AI recommendation: ").append(aiRecommendation == null ? "N/A" : aiRecommendation).append("\n");
        builder.append("HR decision: ").append(decision).append("\n");

        if (StringUtils.hasText(reason)) {
            builder.append("Reason: ").append(reason.trim()).append("\n");
        }

        return builder.toString();
    }

    private String appendHrNote(String oldNote, String title, String note) {
        StringBuilder builder = new StringBuilder();

        if (StringUtils.hasText(oldNote)) {
            builder.append(oldNote).append("\n\n");
        }

        builder.append("[").append(title).append("]");

        if (StringUtils.hasText(note)) {
            builder.append("\n").append(note);
        }

        return builder.toString();
    }
}