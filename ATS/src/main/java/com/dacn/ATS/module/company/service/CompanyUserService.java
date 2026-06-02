package com.dacn.ATS.module.company.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dacn.ATS.module.auth.entity.User;
import com.dacn.ATS.module.company.dto.CreateCompanyUserRequest;

public interface CompanyUserService {

    User createCompanyUser(CreateCompanyUserRequest request);

    Page<User> pageCurrentCompanyUsers(int page, int size, String keyword, String role, String status);

    void lockCompanyUser(Long userId);

    void unlockCompanyUser(Long userId);

    void deleteCompanyUser(Long userId);
}