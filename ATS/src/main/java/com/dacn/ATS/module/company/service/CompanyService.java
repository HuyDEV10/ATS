package com.dacn.ATS.module.company.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dacn.ATS.module.company.entity.Company;

public interface CompanyService {

    Company createCompany(Company company);

    Company getCompanyById(Long id);

    Company getCurrentCompany();

    Company updateCurrentCompanyProfile(Company company);

    Company updateCompanyByAdmin(Long id, Company company);

    Page<Company> pageCompanies(int page, int size, String keyword, String status);

    void approveCompany(Long id);

    void suspendCompany(Long id);

    void rejectCompany(Long id);
}
