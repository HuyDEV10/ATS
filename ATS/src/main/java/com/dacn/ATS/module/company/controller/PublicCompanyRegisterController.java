package com.dacn.ATS.module.company.controller;

import com.dacn.ATS.module.company.dto.CompanyRegisterRequest;
import com.dacn.ATS.module.company.service.CompanyRegistrationService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/public/company")
public class PublicCompanyRegisterController {

    private final CompanyRegistrationService companyRegistrationService;

    public PublicCompanyRegisterController(CompanyRegistrationService companyRegistrationService) {
        this.companyRegistrationService = companyRegistrationService;
    }

    @GetMapping("/register")
    public String showRegisterForm(Model model) {
        model.addAttribute("request", new CompanyRegisterRequest());
        return "public/company-register";
    }

    @PostMapping("/register")
    public String registerCompany(
            @ModelAttribute("request") CompanyRegisterRequest request,
            Model model) {

        try {
            companyRegistrationService.registerCompany(request);
            return "public/company-register-success";
        } catch (Exception ex) {
            model.addAttribute("error", ex.getMessage());
            model.addAttribute("request", request);
            return "public/company-register";
        }
    }
}