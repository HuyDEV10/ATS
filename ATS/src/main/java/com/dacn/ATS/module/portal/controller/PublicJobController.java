package com.dacn.ATS.module.portal.controller;

import com.dacn.ATS.module.job.service.JobService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/public/jobs")
public class PublicJobController {

    private final JobService jobService;

    public PublicJobController(JobService jobService) {
        this.jobService = jobService;
    }

    @GetMapping
    public String publicJobs(Model model) {
        model.addAttribute("jobs", jobService.listPublishedPublicJobs());
        return "public/jobs";
    }

    @GetMapping("/{id}")
    public String publicJobDetail(@PathVariable Long id, Model model) {
        model.addAttribute("job", jobService.getPublicPublishedJobById(id));
        return "public/job-detail";
    }
}