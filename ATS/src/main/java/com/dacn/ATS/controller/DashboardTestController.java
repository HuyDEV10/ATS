package com.dacn.ATS.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DashboardTestController {

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        model.addAttribute("totalJobs", 120);

        return "dashboard";
    }
}