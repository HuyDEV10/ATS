package com.dacn.ATS.module.company.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dacn.ATS.common.enums.ResultCodeEnum;
import com.dacn.ATS.common.util.CurrentUserUtil;
import com.dacn.ATS.exception.BusinessException;
import com.dacn.ATS.module.company.entity.Company;
import com.dacn.ATS.module.company.enums.CompanyStatus;
import com.dacn.ATS.module.company.mapper.CompanyMapper;
import com.dacn.ATS.module.company.service.CompanyService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.lang.reflect.Method;

@Service
public class CompanyServiceImpl implements CompanyService {

    private final CompanyMapper companyMapper;

    public CompanyServiceImpl(CompanyMapper companyMapper) {
        this.companyMapper = companyMapper;
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
        Long companyId = null;
        try {
            Method m = CurrentUserUtil.class.getMethod("getCurrentCompanyId");
            companyId = (Long) m.invoke(null);
        } catch (NoSuchMethodException e) {
            try {
                Method getUser = CurrentUserUtil.class.getMethod("getCurrentUser");
                Object user = getUser.invoke(null);
                if (user != null) {
                    Method getCompanyId = user.getClass().getMethod("getCompanyId");
                    companyId = (Long) getCompanyId.invoke(user);
                }
            } catch (Exception ex) {
                // fallback to null
            }
        } catch (Exception e) {
            // fallback to null
        }

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
            wrapper.like(Company::getName, keyword)
                    .or()
                    .like(Company::getIndustry, keyword);
        }

        if (StringUtils.hasText(status)) {
            wrapper.eq(Company::getStatus, status);
        }

        wrapper.orderByDesc(Company::getCreatedAt);
        return companyMapper.selectPage(pageObj, wrapper);
    }

    @Override
    public void approveCompany(Long id) {
        changeStatus(id, CompanyStatus.ACTIVE);
    }

    @Override
    public void suspendCompany(Long id) {
        changeStatus(id, CompanyStatus.SUSPENDED);
    }

    @Override
    public void rejectCompany(Long id) {
        changeStatus(id, CompanyStatus.REJECTED);
    }

    private void changeStatus(Long id, CompanyStatus status) {
        Company company = getCompanyById(id);
        company.setStatus(status.name());
        company.setUpdatedAt(LocalDateTime.now());
        companyMapper.updateById(company);
    }
}
 