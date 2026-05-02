package com.Reffr_Backend.module.home.service;

import com.Reffr_Backend.module.home.dto.HomeDto;
import java.util.List;
import java.util.UUID;

public interface HomeService {
    List<HomeDto.ActivityResponse> getRecentActivity(UUID userId);
    List<HomeDto.CompanyStatsResponse> getTopHiringCompanies();
    List<HomeDto.SkillStatsResponse> getTrendingSkills();
    List<HomeDto.TopReferrerResponse> getTopReferrers();
}
