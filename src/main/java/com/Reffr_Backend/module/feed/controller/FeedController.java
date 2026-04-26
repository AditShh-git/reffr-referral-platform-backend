package com.Reffr_Backend.module.feed.controller;

import com.Reffr_Backend.common.dto.CursorPagedResponse;
import com.Reffr_Backend.common.response.ApiResponse;
import com.Reffr_Backend.common.security.annotation.RequiresOnboarding;
import com.Reffr_Backend.common.util.PaginationUtils;
import com.Reffr_Backend.common.util.SecurityUtils;
import com.Reffr_Backend.module.feed.dto.PostDto;
import com.Reffr_Backend.module.feed.service.PostService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/feed")
@RequiredArgsConstructor
@Tag(name = "Feed", description = "Personalized feed operations")
@SecurityRequirement(name = "bearerAuth")
public class FeedController {

    private final PostService postService;

    @GetMapping
    @Operation(summary = "Get personalized feed", description = "Returns active posts ranked by matching skills, OFFERs, and verified users.")
    @RequiresOnboarding
    public ResponseEntity<ApiResponse<CursorPagedResponse<PostDto.FeedResponse>>> getFeed(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {
        
        Pageable pageable = PaginationUtils.of(page, size);
        CursorPagedResponse<PostDto.FeedResponse> result = postService.getPersonalizedFeed(pageable, SecurityUtils.getCurrentUserId());
        return ResponseEntity.ok(ApiResponse.success("Feed retrieved successfully", result));
    }
}
