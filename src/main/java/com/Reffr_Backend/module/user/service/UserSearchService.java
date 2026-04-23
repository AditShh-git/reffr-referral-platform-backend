package com.Reffr_Backend.module.user.service;


import com.Reffr_Backend.module.user.dto.UserDto;
import com.Reffr_Backend.module.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

public interface UserSearchService {

    Page<UserDto.UserSummary> searchUsers(String query, Pageable pageable);

    Page<UserDto.PublicProfileResponse> findVerifiedReferrers(
            String company, Pageable pageable);
}