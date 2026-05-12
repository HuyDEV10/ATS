package com.dacn.ATS.module.auth.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.dacn.ATS.common.enums.ResultCodeEnum;
import com.dacn.ATS.exception.BusinessException;
import com.dacn.ATS.module.auth.entity.User;
import com.dacn.ATS.module.auth.mapper.UserMapper;
import com.dacn.ATS.module.auth.service.UserService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class UserServiceImpl implements UserService {
    @Override
    public List<User> findAllUsers() {
        return userMapper.selectList(null);
    }

    @Override
    public User createUserByAdmin(String username, String password, String email, String role) {
        if (checkUsernameExists(username)) {
            throw new BusinessException(ResultCodeEnum.USERNAME_EXISTS);
        }

        if (checkEmailExists(email)) {
            throw new BusinessException(ResultCodeEnum.EMAIL_EXISTS);
        }

        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setEmail(email);
        user.setRole(role);
        user.setCreateTime(LocalDateTime.now());
        user.setDeleted(0);

        userMapper.insert(user);
        return user;
    }

    @Override
    public void changeRole(Long userId, String role) {
        User user = userMapper.selectById(userId);

        if (user == null) {
            throw new BusinessException(ResultCodeEnum.NOT_FOUND, "User not found");
        }

        user.setRole(role);
        userMapper.updateById(user);
    }

    @Override
    public void deleteUser(Long userId) {
        userMapper.deleteById(userId);
    }

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public User register(String username, String password, String email) {
        // Kiểm tra trùng lặp
        if (checkUsernameExists(username)) {
            throw new BusinessException(ResultCodeEnum.USERNAME_EXISTS);
        }
        if (checkEmailExists(email)) {
            throw new BusinessException(ResultCodeEnum.EMAIL_EXISTS);
        }

        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setEmail(email);
        user.setRole("HR");// Mặc định là HR, có thể thay đổi sau
        user.setCreateTime(LocalDateTime.now());
        user.setDeleted(0);

        userMapper.insert(user);
        return user;
    }

    @Override
    public User findByUsername(String username) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, username);
        return userMapper.selectOne(wrapper);
    }

    @Override
    public boolean checkUsernameExists(String username) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, username);
        return userMapper.selectCount(wrapper) > 0;
    }

    @Override
    public boolean checkEmailExists(String email) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getEmail, email);
        return userMapper.selectCount(wrapper) > 0;
    }
}