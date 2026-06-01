package com.dacn.ATS.module.company.controller;

import com.dacn.ATS.common.util.CurrentUserUtil;
import com.dacn.ATS.module.auth.entity.User;
import com.dacn.ATS.module.auth.service.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/company/users")
public class CompanyUserController {

    private final UserService userService;

    public CompanyUserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public String listCompanyUsers(Model model) {
        Long companyId = CurrentUserUtil.getCurrentCompanyId();

        List<User> users = userService.findUsersByCompanyId(companyId);

        model.addAttribute("users", users);
        return "company/users";
    }

    @GetMapping("/create")
    public String showCreateForm(Model model) {
        model.addAttribute("user", new User());
        return "company/user-form";
    }

    @PostMapping("/create")
    public String createCompanyUser(
            @RequestParam String username,
            @RequestParam String password,
            @RequestParam String email,
            @RequestParam(required = false) String fullName,
            @RequestParam(required = false) String phone,
            @RequestParam String role,
            RedirectAttributes redirectAttributes) {

        Long companyId = CurrentUserUtil.getCurrentCompanyId();

        userService.createCompanyUser(
                companyId,
                username,
                password,
                email,
                fullName,
                phone,
                role);

        redirectAttributes.addFlashAttribute("success", "Member created successfully");
        return "redirect:/company/users";
    }

    @PostMapping("/{id}/role")
    public String changeRole(
            @PathVariable Long id,
            @RequestParam String role,
            RedirectAttributes redirectAttributes) {

        userService.changeRole(id, role);
        redirectAttributes.addFlashAttribute("success", "Role updated successfully");
        return "redirect:/company/users";
    }

    @PostMapping("/{id}/delete")
    public String deleteUser(
            @PathVariable Long id,
            RedirectAttributes redirectAttributes) {

        userService.deleteUser(id);
        redirectAttributes.addFlashAttribute("success", "Member deleted successfully");
        return "redirect:/company/users";
    }
}