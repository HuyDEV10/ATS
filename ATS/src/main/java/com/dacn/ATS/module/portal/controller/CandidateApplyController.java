package com.dacn.ATS.module.portal.controller;

import com.dacn.ATS.module.portal.service.CandidateApplyService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/public/apply")
public class CandidateApplyController {

    private final CandidateApplyService candidateApplyService;

    public CandidateApplyController(CandidateApplyService candidateApplyService) {
        this.candidateApplyService = candidateApplyService;
    }

    @PostMapping("/{jobId}")
    public String apply(
            @PathVariable Long jobId,
            @RequestParam String name,
            @RequestParam String email,
            @RequestParam String phone,
            @RequestParam String skills,
            @RequestParam Integer experienceYears,
            @RequestParam("file") MultipartFile file,
            RedirectAttributes redirectAttributes) {
        candidateApplyService.apply(
                jobId,
                name,
                email,
                phone,
                skills,
                experienceYears,
                file);

        redirectAttributes.addFlashAttribute("success", "Ứng tuyển thành công");
        return "redirect:/public/jobs/" + jobId;
    }
}