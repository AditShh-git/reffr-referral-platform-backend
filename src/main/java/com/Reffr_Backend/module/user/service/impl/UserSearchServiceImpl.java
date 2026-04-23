package com.Reffr_Backend.module.user.service.impl;

import com.Reffr_Backend.module.user.dto.UserDto;
import com.Reffr_Backend.module.user.infrastructure.FileStorageService;
import com.Reffr_Backend.module.user.repository.UserRepository;
import com.Reffr_Backend.module.user.service.UserSearchService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserSearchServiceImpl implements UserSearchService {

    private final UserRepository     userRepository;
    private final FileStorageService fileStorage;

    @Transactional(readOnly = true)
    public Page<UserDto.UserSummary> searchUsers(String query, Pageable pageable) {
        return userRepository.searchUsers(query, pageable)
                .map(UserDto.UserSummary::from);
    }

    /**
     * Fix 6: Cache key includes company name — only that company's page is evicted
     * when a verification changes, not every cached entry.
     */
    @Transactional(readOnly = true)
    @Cacheable(
            value  = "referrers",
            key    = "#company.toLowerCase() + ':' + #pageable.pageNumber",
            unless = "#result.isEmpty()"
    )
    public Page<UserDto.PublicProfileResponse> findVerifiedReferrers(
            String company, Pageable pageable) {
        return userRepository.findVerifiedReferrersAtCompany(company, pageable)
                .map(user -> UserDto.PublicProfileResponse.from(
                        user,
                        user.hasResume() ? fileStorage.generateAccessUrl(user.getResumeS3Key()) : null
                ));
    }
}