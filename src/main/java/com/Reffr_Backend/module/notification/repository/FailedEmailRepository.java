package com.Reffr_Backend.module.notification.repository;

import com.Reffr_Backend.module.notification.entity.FailedEmail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface FailedEmailRepository extends JpaRepository<FailedEmail, UUID> {

    List<FailedEmail> findTop10ByOrderByCreatedAtAsc();
}
