CREATE TABLE IF NOT EXISTS referral_requests (
                                                 id UUID PRIMARY KEY,
                                                 post_id UUID NOT NULL REFERENCES posts(id),
    requester_id UUID NOT NULL REFERENCES users(id),
    referrer_id UUID NOT NULL REFERENCES users(id),
    status VARCHAR(20) NOT NULL,
    message TEXT,
    volunteer_note TEXT,
    resume_snapshot_key VARCHAR(500),
    applicant_github_link VARCHAR(500),
    applicant_linkedin_link VARCHAR(500),
    expires_at TIMESTAMPTZ,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uk_referral_requests_unique
    UNIQUE (post_id, requester_id, referrer_id)
    );