package com.dacn.ATS.module.dashboard.controller;

import com.dacn.ATS.module.dashboard.service.DashboardService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/dashboard/platform-admin")
    public String platformAdminDashboard(Model model) {
        addDashboardData(model);
        return "dashboard/platform-admin";
    }

    @GetMapping("/dashboard/company-owner")
    public String companyOwnerDashboard(Model model) {
        addDashboardData(model);
        return "dashboard/company-owner";
    }

    @GetMapping("/dashboard/hr")
    public String hrDashboard(Model model) {
        addDashboardData(model);
        return "dashboard/hr";
    }

    @GetMapping("/dashboard/interviewer")
    public String interviewerDashboard(Model model) {
        addDashboardData(model);
        return "dashboard/interviewer";
    }

    @GetMapping("/dashboard/viewer")
    public String viewerDashboard(Model model) {
        addDashboardData(model);
        return "dashboard/viewer";
    }

    /**
     * Tạm giữ route cũ để tránh lỗi nếu còn link /dashboard/admin.
     * Sau khi sửa hết menu, có thể xóa route này.
     */
    @GetMapping("/dashboard/admin")
    public String oldAdminDashboardRedirect() {
        return "redirect:/dashboard/platform-admin";
    }

    private void addDashboardData(Model model) {
        model.addAttribute("stats", dashboardService.getStats());
        model.addAttribute("funnel", dashboardService.getRecruitmentFunnel());
        model.addAttribute("scoreDistribution", dashboardService.getAiScoreDistribution());
    }
}