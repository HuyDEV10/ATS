package com.dacn.ATS.module.dashboard.service;

import com.dacn.ATS.module.dashboard.dto.AiScoreDistributionDTO;
import com.dacn.ATS.module.dashboard.dto.DashboardStatsDTO;
import com.dacn.ATS.module.dashboard.dto.RecruitmentFunnelDTO;

public interface DashboardService {
    DashboardStatsDTO getStats();

    RecruitmentFunnelDTO getRecruitmentFunnel();

    AiScoreDistributionDTO getAiScoreDistribution();
}