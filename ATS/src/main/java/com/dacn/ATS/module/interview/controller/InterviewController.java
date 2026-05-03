package com.dacn.ATS.module.interview.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dacn.ATS.module.interview.entity.InterviewRecord;
import com.dacn.ATS.module.interview.service.InterviewService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/interviews")
public class InterviewController {

    @Autowired
    private InterviewService interviewService;

    // Lấy userId hiện tại (tạm thời)
    private Long getCurrentUserId() {
        return 1L; // TODO: lấy từ SecurityContext
    }

    // Danh sách interview (có filter theo applicationId)
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

    // Form tạo mới interview (thường gắn với applicationId)
    @GetMapping("/create")
    public String showCreateForm(@RequestParam Long applicationId, Model model) {
        InterviewRecord interview = new InterviewRecord();
        interview.setApplicationId(applicationId);
        model.addAttribute("interview", interview);
        return "interview/form";
    }

    @PostMapping("/create")
    public String scheduleInterview(@ModelAttribute InterviewRecord interview, RedirectAttributes redirectAttributes) {
        interview.setInterviewerId(getCurrentUserId()); // hoặc lấy từ form
        interviewService.scheduleInterview(interview);
        redirectAttributes.addFlashAttribute("success", "Interview scheduled");
        return "redirect:/interviews?applicationId=" + interview.getApplicationId();
    }

    // Form chỉnh sửa (thường để cập nhật kết quả)
    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Long id, Model model) {
        InterviewRecord interview = interviewService.getInterviewById(id);
        model.addAttribute("interview", interview);
        return "interview/form";
    }

    @PostMapping("/edit/{id}")
    public String updateInterview(@PathVariable Long id, @ModelAttribute InterviewRecord interview,
            RedirectAttributes redirectAttributes) {
        interview.setId(id);
        interviewService.updateInterviewResult(interview);
        redirectAttributes.addFlashAttribute("success", "Interview updated");
        return "redirect:/interviews?applicationId=" + interview.getApplicationId();
    }

    // Hoàn thành phỏng vấn (ghi nhận feedback, score)
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

    // Hủy phỏng vấn
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

    // Xem chi tiết interview
    @GetMapping("/view/{id}")
    public String viewInterview(@PathVariable Long id, Model model) {
        InterviewRecord interview = interviewService.getInterviewById(id);
        model.addAttribute("interview", interview);
        return "interview/view";
    }
}
