package com.Reffr_Backend.module.user.repository;

import com.Reffr_Backend.module.user.entity.UserCompanyVerification;
import com.Reffr_Backend.module.user.entity.VerificationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserCompanyVerificationRepository extends JpaRepository<UserCompanyVerification, UUID> {
    List<UserCompanyVerification> findByUserId(UUID userId);
    Optional<UserCompanyVerification> findByUserIdAndVerificationStatus(UUID userId, VerificationStatus status);
}
