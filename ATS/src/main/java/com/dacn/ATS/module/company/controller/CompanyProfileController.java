
package com.dacn.ATS.module.company.controller;

import com.dacn.ATS.module.company.entity.Company;
import com.dacn.ATS.module.company.service.CompanyService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/company")
public class CompanyProfileController {

    private final CompanyService companyService;

    public CompanyProfileController(CompanyService companyService) {
        this.companyService = companyService;
    }

    @GetMapping("/profile")
    public String profile(Model model) {
        model.addAttribute("company", companyService.getCurrentCompany());
        return "company/profile";
    }

    @PostMapping("/profile")
    public String updateProfile(@ModelAttribute Company company, RedirectAttributes redirectAttributes) {
        companyService.updateCurrentCompanyProfile(company);
        redirectAttributes.addFlashAttribute("success", "Company profile updated");
        return "redirect:/company/profile";
    }
}
