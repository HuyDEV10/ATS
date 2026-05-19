package com.dacn.ATS.module.verification.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dacn.ATS.module.candidate.entity.Candidate;
import com.dacn.ATS.module.candidate.service.CandidateService;
import com.dacn.ATS.module.job.service.JobService;
import com.dacn.ATS.module.verification.dto.SkillEvidenceRequest;
import com.dacn.ATS.module.verification.dto.SkillMatchResult;
import com.dacn.ATS.module.verification.entity.SkillVerification;
import com.dacn.ATS.module.verification.enums.VerificationSourceType;
import com.dacn.ATS.module.verification.service.VerificationService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class VerificationController {
    private final VerificationService verificationService;
    private final CandidateService candidateService;
    private final JobService jobService;

    public VerificationController(
            VerificationService verificationService,
            CandidateService candidateService,
            JobService jobService) {
        this.verificationService = verificationService;
        this.candidateService = candidateService;
        this.jobService = jobService;
    }

    @GetMapping("/verifications/candidate/{candidateId}")
    public String candidateVerifiedSkills(
            @PathVariable Long candidateId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Long jobId,
            Model model) {
        Candidate candidate = candidateService.getCandidateById(candidateId);
        model.addAttribute("candidate", candidate);
        model.addAttribute("verificationPage", verificationService.pageByCandidate(candidateId, page, size));
        model.addAttribute("verifiedSkillProfile", verificationService.buildVerifiedSkillProfile(candidateId));
        model.addAttribute("sourceTypes", VerificationSourceType.values());
        model.addAttribute("evidenceRequest", new SkillEvidenceRequest());
        model.addAttribute("jobs", jobService.pageJobs(1, 100, null).getRecords());
        if (jobId != null) {
            model.addAttribute("matchResult", verificationService.compareCandidateWithJob(candidateId, jobId));
            model.addAttribute("selectedJobId", jobId);
        }
        return "verification/candidate-skills";
    }

    @PostMapping("/verifications/candidate/{candidateId}/submit")
    public String submitEvidence(
            @PathVariable Long candidateId,
            @ModelAttribute SkillEvidenceRequest request,
            @RequestParam(required = false) MultipartFile certificateFile,
            RedirectAttributes redirectAttributes) {
        request.setCandidateId(candidateId);
        SkillVerification verification = verificationService.submitEvidence(request, certificateFile);
        redirectAttributes.addFlashAttribute("success", "Đã phân tích minh chứng kỹ năng với confidence "
                + verification.getConfidenceScore() + "%");
        return "redirect:/verifications/candidate/" + candidateId;
    }

    @PostMapping("/verifications/{id}/verify")
    public String verifyFromUi(
            @PathVariable Long id,
            @RequestParam Long candidateId,
            @RequestParam(required = false) String note,
            RedirectAttributes redirectAttributes) {
        verificationService.markVerified(id, note);
        redirectAttributes.addFlashAttribute("success", "Đã xác thực kỹ năng.");
        return "redirect:/verifications/candidate/" + candidateId;
    }

    @PostMapping("/verifications/{id}/reject")
    public String rejectFromUi(
            @PathVariable Long id,
            @RequestParam Long candidateId,
            @RequestParam(required = false) String note,
            RedirectAttributes redirectAttributes) {
        verificationService.reject(id, note);
        redirectAttributes.addFlashAttribute("success", "Đã từ chối minh chứng kỹ năng.");
        return "redirect:/verifications/candidate/" + candidateId;
    }

    @PostMapping("/api/verifications")
    @ResponseBody
    public SkillVerification submit(@RequestBody SkillVerification verification) {
        return verificationService.submit(verification);
    }

    @PostMapping("/api/verifications/evidence")
    @ResponseBody
    public SkillVerification submitEvidenceApi(
            @ModelAttribute SkillEvidenceRequest request,
            @RequestParam(required = false) MultipartFile certificateFile) {
        return verificationService.submitEvidence(request, certificateFile);
    }

    @PostMapping("/api/verifications/{id}/verify")
    @ResponseBody
    public SkillVerification verify(@PathVariable Long id, @RequestParam(required = false) String note) {
        return verificationService.markVerified(id, note);
    }

    @PostMapping("/api/verifications/{id}/reject")
    @ResponseBody
    public SkillVerification reject(@PathVariable Long id, @RequestParam(required = false) String note) {
        return verificationService.reject(id, note);
    }

    @GetMapping("/api/verifications/candidate/{candidateId}")
    @ResponseBody
    public Page<SkillVerification> listByCandidate(
            @PathVariable Long candidateId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return verificationService.pageByCandidate(candidateId, page, size);
    }

    @GetMapping("/api/verifications/candidate/{candidateId}/profile")
    @ResponseBody
    public Object verifiedSkillProfile(@PathVariable Long candidateId) {
        return verificationService.buildVerifiedSkillProfile(candidateId);
    }

    @GetMapping("/api/verifications/candidate/{candidateId}/jobs/{jobId}/match")
    @ResponseBody
    public SkillMatchResult compareCandidateWithJob(@PathVariable Long candidateId, @PathVariable Long jobId) {
        return verificationService.compareCandidateWithJob(candidateId, jobId);
    }
}