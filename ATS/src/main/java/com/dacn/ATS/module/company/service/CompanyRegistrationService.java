package com.dacn.ATS.module.company.service;

import com.dacn.ATS.module.company.dto.CompanyRegisterRequest;

public interface CompanyRegistrationService {
    void registerCompany(CompanyRegisterRequest request);
}