-- V20: referral feedback and reputation counters

CREATE TABLE IF NOT EXISTS referral_feedback (
                                                 id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    referral_id UUID NOT NULL UNIQUE
    REFERENCES referrals(id) ON DELETE CASCADE,

    reviewer_id UUID NOT NULL
    REFERENCES users(id),

    referrer_id UUID NOT NULL
    REFERENCES users(id),

    rating VARCHAR(10) NOT NULL,
    comment VARCHAR(200),

    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
    );

CREATE INDEX IF NOT EXISTS idx_feedback_referrer
    ON referral_feedback(referrer_id);

-- Add feedback counters to users
ALTER TABLE users
    ADD COLUMN IF NOT EXISTS positive_feedback_count INT NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS negative_feedback_count INT NOT NULL DEFAULT 0;