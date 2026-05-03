package com.dacn.ATS.module.candidate.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dacn.ATS.module.candidate.entity.Candidate;

import java.util.List;

public interface CandidateService {
    Candidate createCandidate(Candidate candidate, Long currentUserId);

    Candidate updateCandidate(Candidate candidate);

    void deleteCandidate(Long id);

    Candidate getCandidateById(Long id);

    Page<Candidate> pageCandidates(int page, int size, String keyword);

    List<Candidate> listCandidatesByCreatedBy(Long createdBy);
}
