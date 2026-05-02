package com.Reffr_Backend.module.home.service.impl;

import com.Reffr_Backend.module.home.dto.HomeDto;
import com.Reffr_Backend.module.home.service.HomeService;
import com.Reffr_Backend.module.feed.repository.PostRepository;
import com.Reffr_Backend.module.user.repository.UserRepository;
import com.Reffr_Backend.module.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class HomeServiceImpl implements HomeService {

    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final NotificationRepository notificationRepository;
    private final com.Reffr_Backend.module.feed.repository.PostTagRepository postTagRepository;

    @Override
    @Transactional(readOnly = true)
    public List<HomeDto.TopReferrerResponse> getTopReferrers() {
        Pageable topTen = PageRequest.of(0, 10);
        return userRepository.findTopReferrers(topTen).stream()
                .map(HomeDto.TopReferrerResponse::from)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<HomeDto.CompanyStatsResponse> getTopHiringCompanies() {
        Pageable topFive = PageRequest.of(0, 5);
        List<Object[]> results = postRepository.findTopHiringCompanies(topFive);
        return results.stream()
                .map(res -> HomeDto.CompanyStatsResponse.builder()
                        .companyName((String) res[0])
                        .openReferralCount((Long) res[1])
                        .logoUrl(null) // Not yet implemented in schema
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<HomeDto.SkillStatsResponse> getTrendingSkills() {
        Pageable topSix = PageRequest.of(0, 6);
        List<Object[]> results = postTagRepository.findTrendingSkills(topSix);
        return results.stream()
                .map(res -> HomeDto.SkillStatsResponse.builder()
                        .skillName((String) res[0])
                        .frequencyCount((Long) res[1])
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<HomeDto.ActivityResponse> getRecentActivity(UUID userId) {
        Pageable topTen = PageRequest.of(0, 10);
        return notificationRepository.findByUserId(userId, topTen).stream()
                .map(n -> HomeDto.ActivityResponse.builder()
                        .type(n.getType().name())
                        .message(n.getTitle() + (n.getBody() != null ? ": " + n.getBody() : ""))
                        .createdAt(n.getCreatedAt().atZone(java.time.ZoneId.systemDefault()).toInstant())
                        .entityId(n.getEntityId())
                        .entityType(n.getEntityType())
                        .build())
                .collect(Collectors.toList());
    }
}
