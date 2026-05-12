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

    @GetMapping("/dashboard/admin")
    public String adminDashboard(Model model) {
        model.addAttribute("stats", dashboardService.getStats());
        return "dashboard/admin";
    }

    @GetMapping("/dashboard/hr")
    public String hrDashboard(Model model) {
        model.addAttribute("stats", dashboardService.getStats());
        return "dashboard/hr";
    }

    @GetMapping("/dashboard/interviewer")
    public String interviewerDashboard(Model model) {
        model.addAttribute("stats", dashboardService.getStats());
        return "dashboard/interviewer";
    }
}