package com.dacn.ATS.module.resume.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dacn.ATS.common.util.CurrentUserUtil;
import com.dacn.ATS.module.resume.dto.ResumeConflictResult;
import com.dacn.ATS.module.resume.entity.Resume;
import com.dacn.ATS.module.resume.service.ResumeService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/resumes")
public class ResumeController {

    @Autowired
    private ResumeService resumeService;

    private Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return CurrentUserUtil.getCurrentUserId();
    }

    @GetMapping
    public String listResumes(@RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            Model model) {
        Page<Resume> resumePage = resumeService.pageResumes(page, size, keyword);
        model.addAttribute("resumePage", resumePage);
        model.addAttribute("keyword", keyword);
        return "resume/list";
    }

    @GetMapping("/upload")
    public String uploadForm() {
        return "resume/upload";
    }

    @PostMapping("/upload")
    public String uploadResume(@RequestParam("file") MultipartFile file,
            RedirectAttributes redirectAttributes) {
        try {
            Long userId = getCurrentUserId();
            Resume resume = resumeService.uploadResume(file, userId);

            redirectAttributes.addFlashAttribute(
                    "success",
                    "Upload successful. Parse status: " + resume.getParseStatus());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Upload failed: " + e.getMessage());
        }

        return "redirect:/resumes";
    }

    @PostMapping("/parse/{id}")
    public String parseResume(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            Resume resume = resumeService.parseResume(id);
            redirectAttributes.addFlashAttribute("success", "Parse status: " + resume.getParseStatus());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Parse failed: " + e.getMessage());
        }

        return "redirect:/resumes/view/" + id;
    }

    @GetMapping("/compare/candidate/{candidateId}")
    public String compareWithCandidate(@PathVariable Long candidateId, Model model) {
        ResumeConflictResult result = resumeService.compareResumeWithCandidate(candidateId);
        model.addAttribute("result", result);
        return "resume/compare";
    }

    @GetMapping("/view/{id}")
    public String viewResume(@PathVariable Long id, Model model) {
        Resume resume = resumeService.getResumeById(id);
        model.addAttribute("resume", resume);
        return "resume/view";
    }

    @GetMapping("/delete/{id}")
    public String deleteResume(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            resumeService.deleteResume(id);
            redirectAttributes.addFlashAttribute("success", "Resume deleted");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Delete failed: " + e.getMessage());
        }

        return "redirect:/resumes";
    }
}