package com.dacn.ATS.module.auth.service;

import com.dacn.ATS.module.auth.entity.User;

public interface UserService {
    User register(String username, String password, String email);

    User findByUsername(String username);

    boolean checkUsernameExists(String username);

    boolean checkEmailExists(String email);
}