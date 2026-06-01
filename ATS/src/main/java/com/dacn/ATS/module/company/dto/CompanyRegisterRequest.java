package com.dacn.ATS.module.company.dto;

public class CompanyRegisterRequest {

    private String companyName;
    private String logoUrl;
    private String industry;
    private String companySize;
    private String website;
    private String address;

    private String ownerFullName;
    private String username;
    private String email;
    private String phone;
    private String password;

    public String getCompanyName() {
        return companyName;
    }

    public String getLogoUrl() {
        return logoUrl;
    }

    public String getIndustry() {
        return industry;
    }

    public String getCompanySize() {
        return companySize;
    }

    public String getWebsite() {
        return website;
    }

    public String getAddress() {
        return address;
    }

    public String getOwnerFullName() {
        return ownerFullName;
    }

    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
    }

    public String getPhone() {
        return phone;
    }

    public String getPassword() {
        return password;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public void setLogoUrl(String logoUrl) {
        this.logoUrl = logoUrl;
    }

    public void setIndustry(String industry) {
        this.industry = industry;
    }

    public void setCompanySize(String companySize) {
        this.companySize = companySize;
    }

    public void setWebsite(String website) {
        this.website = website;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public void setOwnerFullName(String ownerFullName) {
        this.ownerFullName = ownerFullName;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}