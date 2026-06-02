package com.dacn.ATS.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.dacn.ATS.module.auth.entity.User;
import com.dacn.ATS.module.auth.mapper.UserMapper;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class DataSeeder implements CommandLineRunner {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    public DataSeeder(UserMapper userMapper, PasswordEncoder passwordEncoder) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, "admin");

        User existing = userMapper.selectOne(wrapper);

        if (existing != null) {
            existing.setCompanyId(null);
            existing.setFullName("Platform Admin");
            existing.setEmail("admin@smartats.com");
            existing.setPhone("0900000000");
            existing.setPassword(passwordEncoder.encode("123456"));
            existing.setRole("PLATFORM_ADMIN");
            existing.setStatus("ACTIVE");
            existing.setDeleted(0);
            userMapper.updateById(existing);
            return;
        }

        User admin = new User();
        admin.setCompanyId(null);
        admin.setUsername("admin");
        admin.setFullName("Platform Admin");
        admin.setEmail("admin@smartats.com");
        admin.setPhone("0900000000");
        admin.setPassword(passwordEncoder.encode("123456"));
        admin.setRole("PLATFORM_ADMIN");
        admin.setStatus("ACTIVE");
        admin.setCreateTime(LocalDateTime.now());
        admin.setDeleted(0);

        userMapper.insert(admin);
    }
}