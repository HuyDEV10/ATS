package com.dacn.ATS.module.company.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dacn.ATS.module.company.entity.Company;
import com.dacn.ATS.module.company.service.CompanyService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/companies")
public class AdminCompanyController {

    private final CompanyService companyService;

    public AdminCompanyController(CompanyService companyService) {
        this.companyService = companyService;
    }

    @GetMapping
    public String listCompanies(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            Model model) {

        Page<Company> companyPage = companyService.pageCompanies(page, size, keyword, status);

        model.addAttribute("companyPage", companyPage);
        model.addAttribute("keyword", keyword);
        model.addAttribute("status", status);

        return "admin/company-list";
    }

    @GetMapping("/create")
    public String createCompanyForm(Model model) {
        model.addAttribute("company", new Company());
        return "admin/company-form";
    }

    @PostMapping("/create")
    public String createCompany(
            @ModelAttribute Company company,
            RedirectAttributes redirectAttributes) {

        try {
            companyService.createCompany(company);
            redirectAttributes.addFlashAttribute("success", "Company created successfully");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Create company failed: " + e.getMessage());
        }

        return "redirect:/admin/companies";
    }

    @GetMapping("/{id}")
    public String viewCompany(@PathVariable Long id, Model model) {
        model.addAttribute("company", companyService.getCompanyById(id));
        return "admin/company-view";
    }

    @GetMapping("/{id}/edit")
    public String editCompanyForm(@PathVariable Long id, Model model) {
        model.addAttribute("company", companyService.getCompanyById(id));
        return "admin/company-form";
    }

    @PostMapping("/{id}/edit")
    public String updateCompany(
            @PathVariable Long id,
            @ModelAttribute Company company,
            RedirectAttributes redirectAttributes) {

        try {
            companyService.updateCompanyByAdmin(id, company);
            redirectAttributes.addFlashAttribute("success", "Company updated successfully");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Update company failed: " + e.getMessage());
        }

        return "redirect:/admin/companies/" + id;
    }

    @PostMapping("/{id}/approve")
    public String approve(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            companyService.approveCompany(id);
            redirectAttributes.addFlashAttribute("success", "Company approved");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Approve failed: " + e.getMessage());
        }

        return "redirect:/admin/companies/" + id;
    }

    @PostMapping("/{id}/suspend")
    public String suspend(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            companyService.suspendCompany(id);
            redirectAttributes.addFlashAttribute("success", "Company suspended");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Suspend failed: " + e.getMessage());
        }

        return "redirect:/admin/companies/" + id;
    }

    @PostMapping("/{id}/reject")
    public String reject(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            companyService.rejectCompany(id);
            redirectAttributes.addFlashAttribute("success", "Company rejected");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Reject failed: " + e.getMessage());
        }

        return "redirect:/admin/companies/" + id;
    }
}