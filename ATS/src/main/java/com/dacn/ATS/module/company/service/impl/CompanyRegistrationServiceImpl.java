package com.dacn.ATS.module.company.service.impl;

import com.dacn.ATS.common.enums.ResultCodeEnum;
import com.dacn.ATS.exception.BusinessException;
import com.dacn.ATS.module.auth.entity.User;
import com.dacn.ATS.module.auth.enums.UserRole;
import com.dacn.ATS.module.auth.enums.UserStatus;
import com.dacn.ATS.module.auth.mapper.UserMapper;
import com.dacn.ATS.module.auth.service.UserService;
import com.dacn.ATS.module.company.dto.CompanyRegisterRequest;
import com.dacn.ATS.module.company.entity.Company;
import com.dacn.ATS.module.company.enums.CompanyStatus;
import com.dacn.ATS.module.company.mapper.CompanyMapper;
import com.dacn.ATS.module.company.service.CompanyRegistrationService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

@Service
public class CompanyRegistrationServiceImpl implements CompanyRegistrationService {

    private final CompanyMapper companyMapper;
    private final UserMapper userMapper;
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;

    public CompanyRegistrationServiceImpl(
            CompanyMapper companyMapper,
            UserMapper userMapper,
            UserService userService,
            PasswordEncoder passwordEncoder) {
        this.companyMapper = companyMapper;
        this.userMapper = userMapper;
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void registerCompany(CompanyRegisterRequest request) {
        validate(request);

        if (userService.checkUsernameExists(request.getUsername())) {
            throw new BusinessException(ResultCodeEnum.USERNAME_EXISTS);
        }

        if (userService.checkEmailExists(request.getEmail())) {
            throw new BusinessException(ResultCodeEnum.EMAIL_EXISTS);
        }

        Company company = new Company();
        company.setName(request.getCompanyName());
        company.setLogoUrl(request.getLogoUrl());
        company.setIndustry(request.getIndustry());
        company.setCompanySize(request.getCompanySize());
        company.setWebsite(request.getWebsite());
        company.setAddress(request.getAddress());
        company.setStatus(CompanyStatus.PENDING.name());
        company.setCreatedAt(LocalDateTime.now());
        company.setUpdatedAt(LocalDateTime.now());
        company.setDeleted(0);

        companyMapper.insert(company);

        User owner = new User();
        owner.setCompanyId(company.getId());
        owner.setUsername(request.getUsername());
        owner.setFullName(request.getOwnerFullName());
        owner.setPassword(passwordEncoder.encode(request.getPassword()));
        owner.setEmail(request.getEmail());
        owner.setPhone(request.getPhone());
        owner.setRole(UserRole.COMPANY_OWNER.name());
        owner.setStatus(UserStatus.PENDING.name());
        owner.setCreateTime(LocalDateTime.now());
        owner.setDeleted(0);

        userMapper.insert(owner);
    }

    private void validate(CompanyRegisterRequest request) {
        if (!StringUtils.hasText(request.getCompanyName())) {
            throw new BusinessException(ResultCodeEnum.BAD_REQUEST, "Company name is required");
        }

        if (!StringUtils.hasText(request.getOwnerFullName())) {
            throw new BusinessException(ResultCodeEnum.BAD_REQUEST, "Owner full name is required");
        }

        if (!StringUtils.hasText(request.getUsername())) {
            throw new BusinessException(ResultCodeEnum.BAD_REQUEST, "Username is required");
        }

        if (!StringUtils.hasText(request.getEmail())) {
            throw new BusinessException(ResultCodeEnum.BAD_REQUEST, "Email is required");
        }

        if (!StringUtils.hasText(request.getPassword())) {
            throw new BusinessException(ResultCodeEnum.BAD_REQUEST, "Password is required");
        }
    }
}