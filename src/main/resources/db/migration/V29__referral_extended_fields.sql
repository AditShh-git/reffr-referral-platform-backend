-- V29: Extend referrals with OFFER apply data and expiry support

ALTER TABLE referrals
    ADD COLUMN IF NOT EXISTS volunteer_note TEXT,
    ADD COLUMN IF NOT EXISTS resume_snapshot_key VARCHAR(500),
    ADD COLUMN IF NOT EXISTS applicant_github_link VARCHAR(500),
    ADD COLUMN IF NOT EXISTS applicant_linkedin_link VARCHAR(500),
    ADD COLUMN IF NOT EXISTS expires_at TIMESTAMPTZ;

-- Backfill expiry for existing pending referrals
UPDATE referrals
SET expires_at = created_at + INTERVAL '7 days'
WHERE status = 'PENDING'
  AND expires_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_ref_expires
    ON referrals(expires_at)
    WHERE status = 'PENDING';