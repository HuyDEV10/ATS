package com.dacn.ATS.module.company.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dacn.ATS.module.auth.entity.User;
import com.dacn.ATS.module.company.dto.CreateCompanyUserRequest;
import com.dacn.ATS.module.company.service.CompanyUserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/company/users")
public class CompanyUserController {

    private final CompanyUserService companyUserService;

    public CompanyUserController(CompanyUserService companyUserService) {
        this.companyUserService = companyUserService;
    }

    @GetMapping
    public String listCompanyUsers(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String status,
            Model model) {

        Page<User> userPage = companyUserService.pageCurrentCompanyUsers(
                page,
                size,
                keyword,
                role,
                status);

        model.addAttribute("userPage", userPage);
        model.addAttribute("keyword", keyword);
        model.addAttribute("role", role);
        model.addAttribute("status", status);

        return "company/users";
    }

    @GetMapping("/create")
    public String createCompanyUserForm(Model model) {
        model.addAttribute("request", new CreateCompanyUserRequest());
        return "company/user-form";
    }

    @PostMapping("/create")
    public String createCompanyUser(
            @ModelAttribute CreateCompanyUserRequest request,
            RedirectAttributes redirectAttributes) {

        try {
            companyUserService.createCompanyUser(request);
            redirectAttributes.addFlashAttribute("success", "Company user created successfully");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Create user failed: " + e.getMessage());
        }

        return "redirect:/company/users";
    }

    @PostMapping("/lock/{id}")
    public String lockCompanyUser(
            @PathVariable Long id,
            RedirectAttributes redirectAttributes) {

        try {
            companyUserService.lockCompanyUser(id);
            redirectAttributes.addFlashAttribute("success", "User locked");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lock user failed: " + e.getMessage());
        }

        return "redirect:/company/users";
    }

    @PostMapping("/unlock/{id}")
    public String unlockCompanyUser(
            @PathVariable Long id,
            RedirectAttributes redirectAttributes) {

        try {
            companyUserService.unlockCompanyUser(id);
            redirectAttributes.addFlashAttribute("success", "User unlocked");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Unlock user failed: " + e.getMessage());
        }

        return "redirect:/company/users";
    }

    @PostMapping("/delete/{id}")
    public String deleteCompanyUser(
            @PathVariable Long id,
            RedirectAttributes redirectAttributes) {

        try {
            companyUserService.deleteCompanyUser(id);
            redirectAttributes.addFlashAttribute("success", "User deleted");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Delete user failed: " + e.getMessage());
        }

        return "redirect:/company/users";
    }
}