package com.Reffr_Backend.module.user.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "user_company_verifications")
@Getter @Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserCompanyVerification {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 150)
    private String company;

    @Column(name = "job_role", nullable = false, length = 150)
    private String role;

    @Column(name = "start_year")
    private Short startYear;

    @Column(name = "end_year")
    private Short endYear;

    @Column(name = "is_current")
    @Builder.Default
    private boolean current = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "proof_type", length = 50)
    private ProofType proofType;

    @Column(name = "is_verified")
    @Builder.Default
    private boolean verified = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "verification_status", nullable = false, length = 50)
    private VerificationStatus verificationStatus;

    @Column(length = 255)
    private String email;

    @Column(name = "document_key", length = 500)
    private String documentKey;

    @Column(name = "profile_url", length = 500)
    private String profileUrl;

    @Column(name = "verified_at")
    private Instant verifiedAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;
}
