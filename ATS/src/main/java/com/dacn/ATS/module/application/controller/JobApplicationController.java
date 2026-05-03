package com.dacn.ATS.module.application.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dacn.ATS.module.application.entity.JobApplication;
import com.dacn.ATS.module.application.service.JobApplicationService;
import com.dacn.ATS.module.candidate.service.CandidateService;
import com.dacn.ATS.module.job.service.JobService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/applications")
public class JobApplicationController {

    @Autowired
    private JobApplicationService applicationService;
    @Autowired
    private JobService jobService;
    @Autowired
    private CandidateService candidateService;

    // Danh sách applications
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
        // Lấy danh sách jobs và candidates để lọc (tuỳ chọn)
        model.addAttribute("jobs", jobService.pageJobs(1, 100, null).getRecords());
        model.addAttribute("candidates", candidateService.pageCandidates(1, 100, null).getRecords());
        return "application/list";
    }

    // Form tạo mới application
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

    // Form chỉnh sửa application
    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Long id, Model model) {
        JobApplication application = applicationService.getApplicationById(id);
        model.addAttribute("application", application);
        model.addAttribute("jobs", jobService.pageJobs(1, 100, null).getRecords());
        model.addAttribute("candidates", candidateService.pageCandidates(1, 100, null).getRecords());
        return "application/form";
    }

    @PostMapping("/edit/{id}")
    public String updateApplication(@PathVariable Long id, @ModelAttribute JobApplication application,
            RedirectAttributes redirectAttributes) {
        application.setId(id);
        applicationService.updateApplication(application);
        redirectAttributes.addFlashAttribute("success", "Application updated");
        return "redirect:/applications";
    }

    // Xoá application
    @GetMapping("/delete/{id}")
    public String deleteApplication(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        applicationService.deleteApplication(id);
        redirectAttributes.addFlashAttribute("success", "Application deleted");
        return "redirect:/applications";
    }

    // Xem chi tiết application (kèm job và candidate)
    @GetMapping("/view/{id}")
    public String viewApplication(@PathVariable Long id, Model model) {
        var details = applicationService.getApplicationDetails(id);
        model.addAttribute("details", details);
        return "application/view";
    }

    // Thay đổi trạng thái application
    @PostMapping("/change-status/{id}")
    public String changeStatus(@PathVariable Long id, @RequestParam String status,
            @RequestParam(required = false) String hrNotes,
            RedirectAttributes redirectAttributes) {
        boolean ok = applicationService.changeStatus(id, status, hrNotes);
        if (ok) {
            redirectAttributes.addFlashAttribute("success", "Status changed to " + status);
        } else {
            redirectAttributes.addFlashAttribute("error", "Cannot change status");
        }
        return "redirect:/applications";
    }
}
