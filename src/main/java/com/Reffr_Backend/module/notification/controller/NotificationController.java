package com.Reffr_Backend.module.notification.controller;

import com.Reffr_Backend.common.response.ApiResponse;
import com.Reffr_Backend.common.util.SecurityUtils;
import com.Reffr_Backend.module.notification.dto.NotificationDto;
import com.Reffr_Backend.module.notification.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import com.Reffr_Backend.common.dto.CursorPagedResponse;
import org.springframework.data.domain.Page;
import com.Reffr_Backend.common.util.PaginationUtils;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;
import java.time.Instant;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Notifications", description = "In-app notification bell")
public class NotificationController {

    private final NotificationService notificationService;

    // ── All notifications ─────────────────────────────────────────────

    @GetMapping
    @Operation(summary = "Get all notifications — latest first")
    public ResponseEntity<ApiResponse<CursorPagedResponse<NotificationDto.Response>>> getAll(
            @RequestParam(required = false) Instant lastCreatedAt,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {

        UUID userId = SecurityUtils.getCurrentUserId();
        Pageable pageable = PaginationUtils.of(page, size);
        return ResponseEntity.ok(ApiResponse.success(
                notificationService.getAll(userId, lastCreatedAt, pageable)));
    }

    // ── Unread only ───────────────────────────────────────────────────

    @GetMapping("/unread")
    @Operation(summary = "Get unread notifications only")
    public ResponseEntity<ApiResponse<CursorPagedResponse<NotificationDto.Response>>> getUnread(
            @RequestParam(required = false) Instant lastCreatedAt,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {

        UUID userId = SecurityUtils.getCurrentUserId();
        Pageable pageable = PaginationUtils.of(page, size);
        return ResponseEntity.ok(ApiResponse.success(
                notificationService.getUnread(userId, lastCreatedAt, pageable)));
    }

    // ── Unread count (bell badge) ─────────────────────────────────────

    @GetMapping("/unread-count")
    @Operation(summary = "Unread notification count — for bell badge")
    public ResponseEntity<ApiResponse<NotificationDto.UnreadSummary>> getUnreadCount() {
        long count = notificationService.getUnreadCount(SecurityUtils.getCurrentUserId());
        return ResponseEntity.ok(ApiResponse.success(
                NotificationDto.UnreadSummary.builder().unreadCount(count).build()));
    }

    // ── Mark one as read ──────────────────────────────────────────────

    @PatchMapping("/{notificationId}/read")
    @Operation(summary = "Mark a single notification as read")
    public ResponseEntity<ApiResponse<Void>> markOneRead(
            @PathVariable UUID notificationId) {

        notificationService.markOneRead(notificationId, SecurityUtils.getCurrentUserId());
        return ResponseEntity.ok(ApiResponse.success("Notification marked as read", null));
    }

    // ── Mark all as read ──────────────────────────────────────────────

    @PatchMapping("/read-all")
    @Operation(summary = "Mark all notifications as read")
    public ResponseEntity<ApiResponse<Void>> markAllRead() {
        notificationService.markAllRead(SecurityUtils.getCurrentUserId());
        return ResponseEntity.ok(ApiResponse.success("All notifications marked as read", null));
    }
}
