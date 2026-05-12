package com.dacn.ATS.module.job.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dacn.ATS.common.util.CurrentUserUtil;
import com.dacn.ATS.module.job.entity.Job;
import com.dacn.ATS.module.job.service.JobService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/jobs")
public class JobController {
    // @GetMapping("/test")
    // public String test() {
    // return "test";
    // }

    @Autowired
    private JobService jobService;

    // Lấy userId và role từ SecurityContext (giả sử bạn đã có UserDetails)
    private Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        // Tuỳ cách bạn lưu userId trong principal, ví dụ custom UserDetails
        // Ở đây tạm lấy name nhưng thực tế bạn cần trả về ID
        return CurrentUserUtil.getCurrentUserId(); // TODO: thay bằng lấy từ principal
    }

    private String getCurrentUserRole() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth.getAuthorities().iterator().next().getAuthority();
    }

    // Danh sách job (có phân trang và tìm kiếm)
    @GetMapping
    public String listJobs(@RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            Model model) {
        Page<Job> jobPage = jobService.pageJobs(page, size, keyword);
        model.addAttribute("jobPage", jobPage);
        model.addAttribute("keyword", keyword);
        return "job/list";
    }

    // Form tạo mới job
    @GetMapping("/create")
    public String showCreateForm(Model model) {
        model.addAttribute("job", new Job());
        return "job/form";
    }

    @PostMapping("/create")
    public String createJob(@ModelAttribute Job job, RedirectAttributes redirectAttributes) {
        Long currentUserId = getCurrentUserId();
        jobService.createJob(job, currentUserId);
        redirectAttributes.addFlashAttribute("success", "Job created successfully");
        return "redirect:/jobs";
    }

    // Form chỉnh sửa job
    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Long id, Model model) {
        Job job = jobService.getJobById(id);
        // Kiểm tra quyền (có thể check trong service)
        model.addAttribute("job", job);
        return "job/form";
    }

    @PostMapping("/edit/{id}")
    public String updateJob(@PathVariable Long id, @ModelAttribute Job job, RedirectAttributes redirectAttributes) {
        job.setId(id);
        jobService.updateJob(job);
        redirectAttributes.addFlashAttribute("success", "Job updated");
        return "redirect:/jobs";
    }

    // Xoá job
    @GetMapping("/delete/{id}")
    public String deleteJob(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        jobService.deleteJob(id);
        redirectAttributes.addFlashAttribute("success", "Job deleted");
        return "redirect:/jobs";
    }

    // Xem chi tiết job
    @GetMapping("/view/{id}")
    public String viewJob(@PathVariable Long id, Model model) {
        Job job = jobService.getJobById(id);
        model.addAttribute("job", job);
        return "job/view";
    }

    // Thay đổi trạng thái (publish/close)
    @PostMapping("/change-status/{id}")
    public String changeStatus(@PathVariable Long id, @RequestParam String status,
            RedirectAttributes redirectAttributes) {
        Long userId = getCurrentUserId();
        String role = getCurrentUserRole();
        boolean ok = jobService.changeStatus(id, status, userId, role);
        if (ok) {
            redirectAttributes.addFlashAttribute("success", "Status changed to " + status);
        } else {
            redirectAttributes.addFlashAttribute("error", "Cannot change status");
        }
        return "redirect:/jobs";
    }
}