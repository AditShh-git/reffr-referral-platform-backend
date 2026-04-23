CREATE TYPE referral_status AS ENUM (
    'PENDING',
    'ACCEPTED',
    'REJECTED',
    'SUBMITTED',
    'INTERVIEWING',
    'OFFERED',
    'CLOSED',
    'EXPIRED'
);

CREATE TABLE referrals (
                           id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                           seeker_id       UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                           referrer_id     UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                           post_id         UUID REFERENCES posts(id) ON DELETE SET NULL,
                           target_company  VARCHAR(150) NOT NULL,
                           target_role     VARCHAR(150),
                           job_url         VARCHAR(500),
                           message         TEXT,
                           status          referral_status DEFAULT 'PENDING',
                           status_timeline JSONB DEFAULT '[]'::jsonb,
                           rejection_reason VARCHAR(500),
                           expires_at      TIMESTAMP DEFAULT (NOW() + INTERVAL '14 days'),
                           created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
                           updated_at      TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_referrals_seeker   ON referrals(seeker_id);
CREATE INDEX idx_referrals_referrer ON referrals(referrer_id);
CREATE INDEX idx_referrals_status   ON referrals(status);
CREATE INDEX idx_referrals_company  ON referrals(target_company);
CREATE INDEX idx_referrals_created  ON referrals(created_at DESC);