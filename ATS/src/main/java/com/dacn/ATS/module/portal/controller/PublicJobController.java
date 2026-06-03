package com.dacn.ATS.module.portal.controller;

import com.dacn.ATS.module.company.entity.Company;
import com.dacn.ATS.module.company.service.CompanyService;
import com.dacn.ATS.module.job.entity.Job;
import com.dacn.ATS.module.job.service.JobService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/public/jobs")
public class PublicJobController {

    private final JobService jobService;
    private final CompanyService companyService;

    public PublicJobController(JobService jobService, CompanyService companyService) {
        this.jobService = jobService;
        this.companyService = companyService;
    }

    @GetMapping
    public String publicJobs(Model model) {
        model.addAttribute("jobs", jobService.listPublishedPublicJobs());
        return "public/jobs";
    }

    @GetMapping("/{id}")
    public String publicJobDetail(@PathVariable Long id, Model model) {
        Job job = jobService.getPublicPublishedJobById(id);

        Company company = null;
        if (job.getCompanyId() != null) {
            company = companyService.getCompanyById(job.getCompanyId());
        }

        model.addAttribute("job", job);
        model.addAttribute("company", company);

        return "public/job-detail";
    }
}