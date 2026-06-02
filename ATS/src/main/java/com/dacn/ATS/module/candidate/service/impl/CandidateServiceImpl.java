package com.dacn.ATS.module.candidate.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dacn.ATS.common.enums.ResultCodeEnum;
import com.dacn.ATS.common.util.CurrentUserUtil;
import com.dacn.ATS.exception.BusinessException;
import com.dacn.ATS.module.auth.entity.User;
import com.dacn.ATS.module.auth.mapper.UserMapper;
import com.dacn.ATS.module.candidate.entity.Candidate;
import com.dacn.ATS.module.candidate.mapper.CandidateMapper;
import com.dacn.ATS.module.candidate.service.CandidateService;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class CandidateServiceImpl implements CandidateService {

    private final CandidateMapper candidateMapper;
    private final UserMapper userMapper;

    public CandidateServiceImpl(
            CandidateMapper candidateMapper,
            UserMapper userMapper) {
        this.candidateMapper = candidateMapper;
        this.userMapper = userMapper;
    }

    @Override
    public Candidate createCandidate(Candidate candidate, Long currentUserId) {
        User user = userMapper.selectById(currentUserId);

        if (user == null) {
            throw new BusinessException(
                    ResultCodeEnum.NOT_FOUND,
                    "Current user not found");
        }

        if (!isPlatformAdmin(user.getRole()) && user.getCompanyId() == null) {
            throw new BusinessException(
                    ResultCodeEnum.FORBIDDEN,
                    "Current user has no company scope");
        }

        candidate.setId(null);
        candidate.setCreatedBy(currentUserId);
        candidate.setCompanyId(user.getCompanyId());
        candidate.setCreateTime(LocalDateTime.now());
        candidate.setUpdateTime(LocalDateTime.now());
        candidate.setDeleted(0);

        if (!StringUtils.hasText(candidate.getSource())) {
            candidate.setSource("manual");
        }

        candidateMapper.insert(candidate);

        return candidate;
    }

    @Override
    public Candidate createPublicCandidate(Candidate candidate, Long companyId) {
        if (companyId == null) {
            throw new BusinessException(
                    ResultCodeEnum.BAD_REQUEST,
                    "Company id is required for public candidate");
        }

        candidate.setId(null);
        candidate.setCreatedBy(null);
        candidate.setCompanyId(companyId);
        candidate.setSource("public_apply");
        candidate.setCreateTime(LocalDateTime.now());
        candidate.setUpdateTime(LocalDateTime.now());
        candidate.setDeleted(0);

        candidateMapper.insert(candidate);

        return candidate;
    }

    @Override
    public Candidate updateCandidate(Candidate candidate) {
        Candidate existing = getCandidateById(candidate.getId());

        checkCompanyAccess(existing);

        existing.setName(candidate.getName());
        existing.setEmail(candidate.getEmail());
        existing.setPhone(candidate.getPhone());
        existing.setSkills(candidate.getSkills());
        existing.setExperienceYears(candidate.getExperienceYears());
        existing.setResumeId(candidate.getResumeId());
        existing.setSource(candidate.getSource());
        existing.setUpdateTime(LocalDateTime.now());

        candidateMapper.updateById(existing);

        return candidateMapper.selectById(existing.getId());
    }

    @Override
    public void deleteCandidate(Long id) {
        Candidate candidate = getCandidateById(id);
        checkCompanyAccess(candidate);
        candidateMapper.deleteById(id);
    }

    @Override
    public Candidate getCandidateById(Long id) {
        Candidate candidate = candidateMapper.selectById(id);

        if (candidate == null) {
            throw new BusinessException(ResultCodeEnum.NOT_FOUND, "Candidate not found");
        }

        checkCompanyAccess(candidate);

        return candidate;
    }

    @Override
    public Page<Candidate> pageCandidates(
            int page,
            int size,
            String keyword) {

        Page<Candidate> pageObj = new Page<>(page, size);
        LambdaQueryWrapper<Candidate> wrapper = buildKeywordWrapper(keyword);

        if (!CurrentUserUtil.isPlatformAdmin()) {
            Long companyId = CurrentUserUtil.getCurrentCompanyId();

            if (companyId == null) {
                throw new BusinessException(
                        ResultCodeEnum.FORBIDDEN,
                        "Current user has no company scope");
            }

            wrapper.eq(Candidate::getCompanyId, companyId);
        }

        wrapper.orderByDesc(Candidate::getCreateTime);

        return candidateMapper.selectPage(pageObj, wrapper);
    }

    @Override
    public Page<Candidate> pageCandidatesForCurrentUser(
            int page,
            int size,
            String keyword,
            Long currentUserId,
            String currentUserRole) {

        Page<Candidate> pageObj = new Page<>(page, size);
        LambdaQueryWrapper<Candidate> wrapper = buildKeywordWrapper(keyword);

        if (!isPlatformAdmin(currentUserRole)) {
            User user = userMapper.selectById(currentUserId);

            if (user == null || user.getCompanyId() == null) {
                throw new BusinessException(
                        ResultCodeEnum.FORBIDDEN,
                        "User has no company scope");
            }

            wrapper.eq(Candidate::getCompanyId, user.getCompanyId());
        }

        wrapper.orderByDesc(Candidate::getCreateTime);

        return candidateMapper.selectPage(pageObj, wrapper);
    }

    @Override
    public List<Candidate> listCandidatesByCreatedBy(Long createdBy) {
        LambdaQueryWrapper<Candidate> wrapper = new LambdaQueryWrapper<>();

        wrapper.eq(Candidate::getCreatedBy, createdBy);

        if (!CurrentUserUtil.isPlatformAdmin()) {
            Long companyId = CurrentUserUtil.getCurrentCompanyId();

            if (companyId == null) {
                throw new BusinessException(
                        ResultCodeEnum.FORBIDDEN,
                        "Current user has no company scope");
            }

            wrapper.eq(Candidate::getCompanyId, companyId);
        }

        wrapper.orderByDesc(Candidate::getCreateTime);

        return candidateMapper.selectList(wrapper);
    }

    private LambdaQueryWrapper<Candidate> buildKeywordWrapper(String keyword) {
        LambdaQueryWrapper<Candidate> wrapper = new LambdaQueryWrapper<>();

        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(Candidate::getName, keyword)
                    .or()
                    .like(Candidate::getEmail, keyword)
                    .or()
                    .like(Candidate::getPhone, keyword)
                    .or()
                    .like(Candidate::getSkills, keyword));
        }

        return wrapper;
    }

    private void checkCompanyAccess(Candidate candidate) {
        if (CurrentUserUtil.isPlatformAdmin()) {
            return;
        }

        Long currentCompanyId = CurrentUserUtil.getCurrentCompanyId();

        if (currentCompanyId == null
                || candidate.getCompanyId() == null
                || !currentCompanyId.equals(candidate.getCompanyId())) {

            throw new BusinessException(
                    ResultCodeEnum.FORBIDDEN,
                    "Cannot access candidate from another company");
        }
    }

    private boolean isPlatformAdmin(String role) {
        return "PLATFORM_ADMIN".equals(role) || "ROLE_PLATFORM_ADMIN".equals(role);
    }
}