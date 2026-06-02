package com.dacn.ATS.module.company.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dacn.ATS.common.enums.ResultCodeEnum;
import com.dacn.ATS.common.util.CurrentUserUtil;
import com.dacn.ATS.exception.BusinessException;
import com.dacn.ATS.module.auth.entity.User;
import com.dacn.ATS.module.auth.enums.UserRole;
import com.dacn.ATS.module.auth.enums.UserStatus;
import com.dacn.ATS.module.auth.mapper.UserMapper;
import com.dacn.ATS.module.company.dto.CreateCompanyUserRequest;
import com.dacn.ATS.module.company.entity.Company;
import com.dacn.ATS.module.company.enums.CompanyStatus;
import com.dacn.ATS.module.company.mapper.CompanyMapper;
import com.dacn.ATS.module.company.service.CompanyUserService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Set;

@Service
public class CompanyUserServiceImpl implements CompanyUserService {

    private static final Set<String> MANAGEABLE_ROLES = Set.of(
            UserRole.HR.name(),
            UserRole.INTERVIEWER.name(),
            UserRole.VIEWER.name());

    private final UserMapper userMapper;
    private final CompanyMapper companyMapper;
    private final PasswordEncoder passwordEncoder;

    public CompanyUserServiceImpl(
            UserMapper userMapper,
            CompanyMapper companyMapper,
            PasswordEncoder passwordEncoder) {
        this.userMapper = userMapper;
        this.companyMapper = companyMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public User createCompanyUser(CreateCompanyUserRequest request) {
        requireCompanyOwner();

        Long companyId = requireCurrentCompanyId();
        Company company = requireActiveCompany(companyId);

        validateCreateRequest(request);

        LambdaQueryWrapper<User> duplicateWrapper = new LambdaQueryWrapper<>();
        duplicateWrapper.eq(User::getUsername, request.getUsername())
                .or()
                .eq(User::getEmail, request.getEmail());

        if (userMapper.selectCount(duplicateWrapper) > 0) {
            throw new BusinessException(
                    ResultCodeEnum.BAD_REQUEST,
                    "Username or email already exists");
        }

        User user = new User();
        user.setId(null);
        user.setCompanyId(company.getId());
        user.setUsername(request.getUsername().trim());
        user.setFullName(request.getFullName());
        user.setEmail(request.getEmail().trim());
        user.setPhone(request.getPhone());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(request.getRole());
        user.setStatus(UserStatus.ACTIVE.name());
        user.setCreateTime(LocalDateTime.now());
        user.setDeleted(0);

        userMapper.insert(user);

        return user;
    }

    @Override
    public Page<User> pageCurrentCompanyUsers(
            int page,
            int size,
            String keyword,
            String role,
            String status) {

        requireCompanyOwner();

        Long companyId = requireCurrentCompanyId();

        Page<User> pageObj = new Page<>(page, size);
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();

        wrapper.eq(User::getCompanyId, companyId);
        wrapper.ne(User::getRole, UserRole.COMPANY_OWNER.name());
        wrapper.ne(User::getRole, UserRole.PLATFORM_ADMIN.name());

        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(User::getUsername, keyword)
                    .or()
                    .like(User::getFullName, keyword)
                    .or()
                    .like(User::getEmail, keyword)
                    .or()
                    .like(User::getPhone, keyword));
        }

        if (StringUtils.hasText(role)) {
            wrapper.eq(User::getRole, role);
        }

        if (StringUtils.hasText(status)) {
            wrapper.eq(User::getStatus, status);
        }

        wrapper.orderByDesc(User::getCreateTime);

        return userMapper.selectPage(pageObj, wrapper);
    }

    @Override
    public void lockCompanyUser(Long userId) {
        User user = getManagedCompanyUser(userId);
        user.setStatus(UserStatus.LOCKED.name());
        userMapper.updateById(user);
    }

    @Override
    public void unlockCompanyUser(Long userId) {
        User user = getManagedCompanyUser(userId);
        user.setStatus(UserStatus.ACTIVE.name());
        userMapper.updateById(user);
    }

    @Override
    public void deleteCompanyUser(Long userId) {
        User user = getManagedCompanyUser(userId);
        userMapper.deleteById(user.getId());
    }

    private User getManagedCompanyUser(Long userId) {
        requireCompanyOwner();

        Long companyId = requireCurrentCompanyId();

        User user = userMapper.selectById(userId);

        if (user == null) {
            throw new BusinessException(ResultCodeEnum.NOT_FOUND, "User not found");
        }

        if (!companyId.equals(user.getCompanyId())) {
            throw new BusinessException(
                    ResultCodeEnum.FORBIDDEN,
                    "Cannot manage user from another company");
        }

        if (!MANAGEABLE_ROLES.contains(user.getRole())) {
            throw new BusinessException(
                    ResultCodeEnum.FORBIDDEN,
                    "Company owner can only manage HR, Interviewer and Viewer");
        }

        return user;
    }

    private void validateCreateRequest(CreateCompanyUserRequest request) {
        if (request == null) {
            throw new BusinessException(ResultCodeEnum.BAD_REQUEST, "Request is required");
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

        if (!StringUtils.hasText(request.getRole())) {
            throw new BusinessException(ResultCodeEnum.BAD_REQUEST, "Role is required");
        }

        if (!MANAGEABLE_ROLES.contains(request.getRole())) {
            throw new BusinessException(
                    ResultCodeEnum.BAD_REQUEST,
                    "Role must be HR, INTERVIEWER or VIEWER");
        }
    }

    private void requireCompanyOwner() {
        if (!CurrentUserUtil.isCompanyOwner()) {
            throw new BusinessException(
                    ResultCodeEnum.FORBIDDEN,
                    "Only company owner can manage company users");
        }
    }

    private Long requireCurrentCompanyId() {
        Long companyId = CurrentUserUtil.getCurrentCompanyId();

        if (companyId == null) {
            throw new BusinessException(
                    ResultCodeEnum.FORBIDDEN,
                    "Current user does not belong to a company");
        }

        return companyId;
    }

    private Company requireActiveCompany(Long companyId) {
        Company company = companyMapper.selectById(companyId);

        if (company == null) {
            throw new BusinessException(ResultCodeEnum.NOT_FOUND, "Company not found");
        }

        if (!CompanyStatus.ACTIVE.name().equals(company.getStatus())) {
            throw new BusinessException(
                    ResultCodeEnum.FORBIDDEN,
                    "Company is not active");
        }

        return company;
    }
}