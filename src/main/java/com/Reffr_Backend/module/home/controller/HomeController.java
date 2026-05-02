package com.Reffr_Backend.module.home.controller;

import com.Reffr_Backend.common.response.ApiResponse;
import com.Reffr_Backend.common.util.SecurityUtils;
import com.Reffr_Backend.module.home.dto.HomeDto;
import com.Reffr_Backend.module.home.service.HomeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class HomeController {

    private final HomeService homeService;

    @GetMapping("/activity/recent")
    public ResponseEntity<ApiResponse<List<HomeDto.ActivityResponse>>> getRecentActivity() {
        UUID userId = SecurityUtils.getCurrentUserId();
        List<HomeDto.ActivityResponse> activities = homeService.getRecentActivity(userId);
        return ResponseEntity.ok(ApiResponse.success(activities));
    }

    @GetMapping("/companies/top-hiring")
    public ResponseEntity<ApiResponse<List<HomeDto.CompanyStatsResponse>>> getTopHiringCompanies() {
        List<HomeDto.CompanyStatsResponse> companies = homeService.getTopHiringCompanies();
        return ResponseEntity.ok(ApiResponse.success(companies));
    }

    @GetMapping("/skills/trending")
    public ResponseEntity<ApiResponse<List<HomeDto.SkillStatsResponse>>> getTrendingSkills() {
        List<HomeDto.SkillStatsResponse> skills = homeService.getTrendingSkills();
        return ResponseEntity.ok(ApiResponse.success(skills));
    }

    @GetMapping("/users/top-referrers")
    public ResponseEntity<ApiResponse<List<HomeDto.TopReferrerResponse>>> getTopReferrers() {
        List<HomeDto.TopReferrerResponse> referrers = homeService.getTopReferrers();
        return ResponseEntity.ok(ApiResponse.success(referrers));
    }
}
