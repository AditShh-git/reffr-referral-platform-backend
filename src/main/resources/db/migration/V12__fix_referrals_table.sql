-- Drop duplicate table
DROP TABLE IF EXISTS referral_requests;

-- Fix nullable column
ALTER TABLE referrals
    ALTER COLUMN target_company DROP NOT NULL;

-- Remove unused columns
ALTER TABLE referrals
DROP COLUMN IF EXISTS target_role,
DROP COLUMN IF EXISTS job_url,
DROP COLUMN IF EXISTS status_timeline,
DROP COLUMN IF EXISTS rejection_reason,
DROP COLUMN IF EXISTS expires_at;

-- Fix enum → varchar
ALTER TABLE referrals
ALTER COLUMN status TYPE VARCHAR(20);

-- DROP TYPE IF EXISTS referral_status;

-- Unique constraint
ALTER TABLE referrals
DROP CONSTRAINT IF EXISTS uk_referral_unique;

ALTER TABLE referrals
    ADD CONSTRAINT uk_referral_unique
        UNIQUE (post_id, seeker_id, referrer_id);