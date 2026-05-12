package com.dacn.ATS.module.admin.controller;

import com.dacn.ATS.module.auth.entity.User;
import com.dacn.ATS.module.auth.service.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/users")
public class AdminUserController {

    private final UserService userService;

    public AdminUserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public String listUsers(Model model) {
        model.addAttribute("users", userService.findAllUsers());
        return "admin/users/list";
    }

    @GetMapping("/create")
    public String createForm() {
        return "admin/users/form";
    }

    @PostMapping("/create")
    public String createUser(
            @RequestParam String username,
            @RequestParam String password,
            @RequestParam String email,
            @RequestParam String role,
            RedirectAttributes redirectAttributes) {
        userService.createUserByAdmin(username, password, email, role);
        redirectAttributes.addFlashAttribute("success", "Tạo tài khoản thành công");
        return "redirect:/admin/users";
    }

    @PostMapping("/change-role/{id}")
    public String changeRole(
            @PathVariable Long id,
            @RequestParam String role,
            RedirectAttributes redirectAttributes) {
        userService.changeRole(id, role);
        redirectAttributes.addFlashAttribute("success", "Đổi quyền thành công");
        return "redirect:/admin/users";
    }

    @GetMapping("/delete/{id}")
    public String deleteUser(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        userService.deleteUser(id);
        redirectAttributes.addFlashAttribute("success", "Xóa user thành công");
        return "redirect:/admin/users";
    }
}