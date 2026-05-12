package com.dacn.ATS.module.auth.service;

import com.dacn.ATS.module.auth.entity.User;

import java.util.List;

public interface UserService {

    User register(String username, String password, String email);

    User findByUsername(String username);

    boolean checkUsernameExists(String username);

    boolean checkEmailExists(String email);

    List<User> findAllUsers();

    User createUserByAdmin(
            String username,
            String password,
            String email,
            String role);

    void changeRole(Long userId, String role);

    void deleteUser(Long userId);
}