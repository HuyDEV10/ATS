package com.dacn.ATS.module.auth.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.dacn.ATS.exception.BusinessException;
import com.dacn.ATS.module.auth.entity.User;
import com.dacn.ATS.module.auth.service.UserService;

@Controller
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private UserService userService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // Hiển thị trang đăng nhập
    @GetMapping("/login")
    public String loginPage(@RequestParam(value = "error", required = false) String error,
            @RequestParam(value = "logout", required = false) String logout,
            Model model) {
        if (error != null)
            model.addAttribute("error", "Invalid credentials");
        if (logout != null)
            model.addAttribute("message", "Logged out");
        return "login"; // trả về đúng tên view
    }

    // Hiển thị trang đăng ký
    @GetMapping("/register")
    public String registerPage() {
        return "register";
    }

    // Xử lý đăng ký (không dùng Spring Security form tự động)
    @PostMapping("/doRegister")
    public String doRegister(@RequestParam String username,
            @RequestParam String password,
            @RequestParam String email,
            Model model) {
        try {
            userService.register(username, password, email);
            return "redirect:/auth/login?registered=true";
        } catch (BusinessException e) {
            model.addAttribute("error", e.getMessage());
            return "register";
        }
    }

    // Spring Security sẽ tự động xử lý /auth/doLogin, chỉ cần cung cấp trang lỗi
    // Trang home sau khi đăng nhập thành công
    @GetMapping("/home")
    public String home(Model model, HttpServletRequest request) {
        // Lấy thông tin user từ SecurityContext
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String username = (principal instanceof UserDetails) ? ((UserDetails) principal).getUsername()
                : principal.toString();
        User user = userService.findByUsername(username);
        model.addAttribute("user", user);
        return "home";
    }
}