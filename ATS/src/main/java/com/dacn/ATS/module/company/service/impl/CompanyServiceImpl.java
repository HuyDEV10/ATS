package com.dacn.ATS.module.company.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dacn.ATS.common.enums.ResultCodeEnum;
import com.dacn.ATS.common.util.CurrentUserUtil;
import com.dacn.ATS.exception.BusinessException;
import com.dacn.ATS.module.auth.entity.User;
import com.dacn.ATS.module.auth.enums.UserStatus;
import com.dacn.ATS.module.auth.mapper.UserMapper;
import com.dacn.ATS.module.company.entity.Company;
import com.dacn.ATS.module.company.enums.CompanyStatus;
import com.dacn.ATS.module.company.mapper.CompanyMapper;
import com.dacn.ATS.module.company.service.CompanyService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

@Service
public class CompanyServiceImpl implements CompanyService {

    private final CompanyMapper companyMapper;
    private final UserMapper userMapper;

    public CompanyServiceImpl(CompanyMapper companyMapper, UserMapper userMapper) {
        this.companyMapper = companyMapper;
        this.userMapper = userMapper;
    }

    @Override
    public Company createCompany(Company company) {
        company.setId(null);
        company.setStatus(CompanyStatus.PENDING.name());
        company.setCreatedAt(LocalDateTime.now());
        company.setUpdatedAt(LocalDateTime.now());
        company.setDeleted(0);
        companyMapper.insert(company);
        return company;
    }

    @Override
    public Company getCompanyById(Long id) {
        Company company = companyMapper.selectById(id);
        if (company == null) {
            throw new BusinessException(ResultCodeEnum.NOT_FOUND, "Company not found");
        }
        return company;
    }

    @Override
    public Company getCurrentCompany() {
        Long companyId = CurrentUserUtil.getCurrentCompanyId();

        if (companyId == null) {
            throw new BusinessException(ResultCodeEnum.FORBIDDEN, "Current user does not belong to a company");
        }

        return getCompanyById(companyId);
    }

    @Override
    public Company updateCurrentCompanyProfile(Company company) {
        Company existing = getCurrentCompany();

        existing.setName(company.getName());
        existing.setLogoUrl(company.getLogoUrl());
        existing.setIndustry(company.getIndustry());
        existing.setCompanySize(company.getCompanySize());
        existing.setWebsite(company.getWebsite());
        existing.setAddress(company.getAddress());
        existing.setUpdatedAt(LocalDateTime.now());

        companyMapper.updateById(existing);
        return companyMapper.selectById(existing.getId());
    }

    @Override
    public Company updateCompanyByAdmin(Long id, Company company) {
        Company existing = getCompanyById(id);

        existing.setName(company.getName());
        existing.setLogoUrl(company.getLogoUrl());
        existing.setIndustry(company.getIndustry());
        existing.setCompanySize(company.getCompanySize());
        existing.setWebsite(company.getWebsite());
        existing.setAddress(company.getAddress());
        existing.setUpdatedAt(LocalDateTime.now());

        companyMapper.updateById(existing);
        return companyMapper.selectById(id);
    }

    @Override
    public Page<Company> pageCompanies(int page, int size, String keyword, String status) {
        Page<Company> pageObj = new Page<>(page, size);
        LambdaQueryWrapper<Company> wrapper = new LambdaQueryWrapper<>();

        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(Company::getName, keyword)
                    .or()
                    .like(Company::getIndustry, keyword));
        }

        if (StringUtils.hasText(status)) {
            wrapper.eq(Company::getStatus, status);
        }

        wrapper.orderByDesc(Company::getCreatedAt);
        return companyMapper.selectPage(pageObj, wrapper);
    }

    @Override
    public void approveCompany(Long id) {
        Company company = getCompanyById(id);
        company.setStatus(CompanyStatus.ACTIVE.name());
        company.setUpdatedAt(LocalDateTime.now());
        companyMapper.updateById(company);

        LambdaUpdateWrapper<User> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(User::getCompanyId, id)
                .eq(User::getStatus, UserStatus.PENDING.name())
                .set(User::getStatus, UserStatus.ACTIVE.name());

        userMapper.update(null, wrapper);
    }

    @Override
    public void suspendCompany(Long id) {
        Company company = getCompanyById(id);
        company.setStatus(CompanyStatus.SUSPENDED.name());
        company.setUpdatedAt(LocalDateTime.now());
        companyMapper.updateById(company);

        LambdaUpdateWrapper<User> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(User::getCompanyId, id)
                .set(User::getStatus, UserStatus.LOCKED.name());

        userMapper.update(null, wrapper);
    }

    @Override
    public void rejectCompany(Long id) {
        Company company = getCompanyById(id);
        company.setStatus(CompanyStatus.REJECTED.name());
        company.setUpdatedAt(LocalDateTime.now());
        companyMapper.updateById(company);

        LambdaUpdateWrapper<User> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(User::getCompanyId, id)
                .eq(User::getStatus, UserStatus.PENDING.name())
                .set(User::getStatus, UserStatus.LOCKED.name());

        userMapper.update(null, wrapper);
    }
}