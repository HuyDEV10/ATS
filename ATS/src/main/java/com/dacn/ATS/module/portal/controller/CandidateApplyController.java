package com.dacn.ATS.module.portal.controller;

import com.dacn.ATS.module.portal.dto.PublicApplyRequest;
import com.dacn.ATS.module.portal.dto.PublicApplyResult;
import com.dacn.ATS.module.portal.service.PublicApplyService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class CandidateApplyController {

    private final PublicApplyService publicApplyService;

    public CandidateApplyController(PublicApplyService publicApplyService) {
        this.publicApplyService = publicApplyService;
    }

    @PostMapping("/public/apply/{jobId}")
    public String apply(
            @PathVariable Long jobId,
            @RequestParam String name,
            @RequestParam String email,
            @RequestParam(required = false) String phone,
            @RequestParam(required = false) String skills,
            @RequestParam(required = false, defaultValue = "0") Integer experienceYears,
            @RequestParam("file") MultipartFile file,
            RedirectAttributes redirectAttributes) {

        try {
            PublicApplyRequest request = new PublicApplyRequest();
            request.setName(name);
            request.setEmail(email);
            request.setPhone(phone);
            request.setSkills(skills);
            request.setExperienceYears(experienceYears);
            request.setFile(file);

            PublicApplyResult result = publicApplyService.applyToJob(jobId, request);

            redirectAttributes.addFlashAttribute("result", result);

            return "redirect:/public/apply/success";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/public/jobs/" + jobId;
        }
    }

    @GetMapping("/public/apply/success")
    public String success(Model model) {
        return "public/apply-success";
    }
}