package com.Reffr_Backend.module.feed.controller;

import com.Reffr_Backend.common.response.ApiResponse;
import com.Reffr_Backend.common.security.annotation.RequiresOnboarding;
import com.Reffr_Backend.common.util.PaginationUtils;
import com.Reffr_Backend.common.util.SecurityUtils;
import com.Reffr_Backend.module.feed.dto.PostQuestionDto;
import com.Reffr_Backend.module.feed.service.PostQuestionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/posts")
@RequiredArgsConstructor
@Tag(name = "Post Questions", description = "Async Q&A on posts — no chat required")
@RequiresOnboarding
public class PostQuestionController {

    private final PostQuestionService questionService;

    @PostMapping("/{postId}/questions")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
            summary = "Ask a question on a post",
            description = "Rate-limited: max 3 questions per user per post per day.")
    public ResponseEntity<ApiResponse<PostQuestionDto.Response>> ask(
            @PathVariable UUID postId,
            @Valid @RequestBody PostQuestionDto.AskRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success("Question submitted",
                questionService.ask(postId, SecurityUtils.getCurrentUserId(), request)));
    }

    @PatchMapping("/{postId}/questions/{questionId}/answer")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
            summary = "Answer a question (post owner only)",
            description = "Only the post author can answer their post's questions.")
    public ResponseEntity<ApiResponse<PostQuestionDto.Response>> answer(
            @PathVariable UUID postId,
            @PathVariable UUID questionId,
            @Valid @RequestBody PostQuestionDto.AnswerRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success("Answer posted",
                questionService.answer(questionId, SecurityUtils.getCurrentUserId(), request)));
    }

    @GetMapping("/{postId}/questions")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "List questions for a post (public, chronological)")
    public ResponseEntity<ApiResponse<Page<PostQuestionDto.Response>>> listByPost(
            @PathVariable UUID postId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PaginationUtils.of(page, size);
        return ResponseEntity.ok(ApiResponse.success(questionService.listByPost(postId, pageable)));
    }

    @GetMapping("/questions/unanswered")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
            summary = "My unanswered questions (post author)",
            description = "Returns questions from all of the current user's posts that haven't been answered yet.")
    public ResponseEntity<ApiResponse<Page<PostQuestionDto.Response>>> listUnanswered(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PaginationUtils.of(page, size);
        return ResponseEntity.ok(ApiResponse.success(
                questionService.listUnanswered(SecurityUtils.getCurrentUserId(), pageable)));
    }
}
