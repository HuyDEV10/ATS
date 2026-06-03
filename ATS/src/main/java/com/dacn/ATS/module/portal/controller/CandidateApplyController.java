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

            // Optional Skill Evidence 1
            @RequestParam(required = false) String evidenceSkill1,
            @RequestParam(required = false) String evidenceType1,
            @RequestParam(required = false) String evidenceProvider1,
            @RequestParam(required = false) String evidenceUrl1,

            // Optional Skill Evidence 2
            @RequestParam(required = false) String evidenceSkill2,
            @RequestParam(required = false) String evidenceType2,
            @RequestParam(required = false) String evidenceProvider2,
            @RequestParam(required = false) String evidenceUrl2,

            // Optional Skill Evidence 3
            @RequestParam(required = false) String evidenceSkill3,
            @RequestParam(required = false) String evidenceType3,
            @RequestParam(required = false) String evidenceProvider3,
            @RequestParam(required = false) String evidenceUrl3,

            RedirectAttributes redirectAttributes) {

        try {
            PublicApplyRequest request = new PublicApplyRequest();

            request.setName(name);
            request.setEmail(email);
            request.setPhone(phone);
            request.setSkills(skills);
            request.setExperienceYears(experienceYears);
            request.setFile(file);

            request.setEvidenceSkill1(evidenceSkill1);
            request.setEvidenceType1(evidenceType1);
            request.setEvidenceProvider1(evidenceProvider1);
            request.setEvidenceUrl1(evidenceUrl1);

            request.setEvidenceSkill2(evidenceSkill2);
            request.setEvidenceType2(evidenceType2);
            request.setEvidenceProvider2(evidenceProvider2);
            request.setEvidenceUrl2(evidenceUrl2);

            request.setEvidenceSkill3(evidenceSkill3);
            request.setEvidenceType3(evidenceType3);
            request.setEvidenceProvider3(evidenceProvider3);
            request.setEvidenceUrl3(evidenceUrl3);

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