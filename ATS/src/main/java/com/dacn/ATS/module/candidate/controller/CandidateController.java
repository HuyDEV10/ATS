package com.dacn.ATS.module.candidate.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dacn.ATS.common.util.CurrentUserUtil;
import com.dacn.ATS.module.candidate.entity.Candidate;
import com.dacn.ATS.module.candidate.service.CandidateService;
import com.dacn.ATS.module.verification.service.VerificationService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/candidates")
public class CandidateController {

    @Autowired
    private CandidateService candidateService;

    @Autowired
    private VerificationService verificationService;

    // Lấy userId hiện tại (tạm thời, sẽ thay bằng lấy từ principal)
    private Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        // TODO: lấy từ custom UserDetails, hiện tại trả về 1L
        return CurrentUserUtil.getCurrentUserId();
    }

    // Danh sách candidates có phân trang và tìm kiếm
    @GetMapping
    public String listCandidates(@RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            Model model) {
        Page<Candidate> candidatePage = candidateService.pageCandidates(page, size, keyword);
        model.addAttribute("candidatePage", candidatePage);
        model.addAttribute("keyword", keyword);
        return "candidate/list";
    }

    // Form tạo mới candidate
    @GetMapping("/create")
    public String showCreateForm(Model model) {
        model.addAttribute("candidate", new Candidate());
        return "candidate/form";
    }

    @PostMapping("/create")
    public String createCandidate(@ModelAttribute Candidate candidate, RedirectAttributes redirectAttributes) {
        Long currentUserId = getCurrentUserId();
        candidateService.createCandidate(candidate, currentUserId);
        redirectAttributes.addFlashAttribute("success", "Candidate created successfully");
        return "redirect:/candidates";
    }

    // Form chỉnh sửa candidate
    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Long id, Model model) {
        Candidate candidate = candidateService.getCandidateById(id);
        model.addAttribute("candidate", candidate);
        return "candidate/form";
    }

    @PostMapping("/edit/{id}")
    public String updateCandidate(@PathVariable Long id, @ModelAttribute Candidate candidate,
            RedirectAttributes redirectAttributes) {
        candidate.setId(id);
        candidateService.updateCandidate(candidate);
        redirectAttributes.addFlashAttribute("success", "Candidate updated");
        return "redirect:/candidates";
    }

    // Xoá candidate
    @GetMapping("/delete/{id}")
    public String deleteCandidate(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        candidateService.deleteCandidate(id);
        redirectAttributes.addFlashAttribute("success", "Candidate deleted");
        return "redirect:/candidates";
    }

    // Xem chi tiết candidate
    @GetMapping("/view/{id}")
    public String viewCandidate(@PathVariable Long id, Model model) {
        Candidate candidate = candidateService.getCandidateById(id);
        model.addAttribute("candidate", candidate);
        model.addAttribute("verifiedSkillProfile", verificationService.buildVerifiedSkillProfile(id));
        return "candidate/view";
    }
}