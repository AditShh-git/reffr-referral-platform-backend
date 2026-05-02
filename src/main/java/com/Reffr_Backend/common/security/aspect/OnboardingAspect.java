package com.Reffr_Backend.common.security.aspect;

import com.Reffr_Backend.common.exception.ForbiddenException;
import com.Reffr_Backend.common.exception.ErrorCodes;
import com.Reffr_Backend.common.exception.NotFoundException;
import com.Reffr_Backend.common.util.SecurityUtils;
import com.Reffr_Backend.module.user.entity.User;
import com.Reffr_Backend.module.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Aspect
@Component
@RequiredArgsConstructor
public class OnboardingAspect {

    private final UserRepository userRepository;
    private final jakarta.servlet.http.HttpServletRequest request;

    @Before("@annotation(com.Reffr_Backend.common.security.annotation.RequiresOnboarding) || @within(com.Reffr_Backend.common.security.annotation.RequiresOnboarding)")
    public void checkOnboardingStatus() {
        String path = request.getRequestURI();

        // Whitelist
        if (path.startsWith("/api/v1/auth/") ||
            path.equals("/api/v1/users/me/onboard") ||
            path.equals("/api/v1/users/me/onboarding-status") ||
            path.equals("/api/v1/users/me/company/send-otp") ||
            path.equals("/api/v1/users/me/company/verify") ||
            path.equals("/api/v1/users/me/company/public") ||
            path.equals("/api/v1/users/me/company/document") ||
            path.equals("/api/v1/users/me/company/document-upload") ||
            path.equals("/api/v1/users/me/experience")) {
            return;
        }

        UUID currentUserId;
        try {
            currentUserId = SecurityUtils.getCurrentUserId();
        } catch (IllegalStateException e) {
            return;
        }

        User user = userRepository.findById(currentUserId)
                .orElseThrow(() -> new NotFoundException(ErrorCodes.USER_NOT_FOUND, "User not found"));

        if (!user.isOnboardingCompleted() || com.Reffr_Backend.module.user.domain.UserDomain.isInvalidPrimaryEmail(user.getPrimaryEmail())) {
            throw new ForbiddenException(ErrorCodes.EMAIL_UPDATE_REQUIRED, 
                    "You must update your email to continue.");
        }

    }
}
