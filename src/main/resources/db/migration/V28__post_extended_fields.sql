-- V28: Extend posts table with all product-required fields
-- Adds title, resume snapshot, social links, visibility controls,
-- urgency, applicant/volunteer limits, and lifecycle status enum.

ALTER TABLE posts
    ADD COLUMN IF NOT EXISTS title               VARCHAR(200),
    ADD COLUMN IF NOT EXISTS resume_snapshot_key VARCHAR(500),
    ADD COLUMN IF NOT EXISTS github_link         VARCHAR(500),
    ADD COLUMN IF NOT EXISTS linkedin_link       VARCHAR(500),
    ADD COLUMN IF NOT EXISTS resume_visibility   VARCHAR(20) NOT NULL DEFAULT 'PUBLIC',
    ADD COLUMN IF NOT EXISTS urgency_deadline    TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS max_volunteers      INT         NOT NULL DEFAULT 10,
    ADD COLUMN IF NOT EXISTS max_applicants      INT         NOT NULL DEFAULT 20,
    ADD COLUMN IF NOT EXISTS post_status         VARCHAR(20) NOT NULL DEFAULT 'OPEN';

-- Index for filtering active+open posts efficiently
CREATE INDEX IF NOT EXISTS idx_posts_status ON posts(post_status);
CREATE INDEX IF NOT EXISTS idx_posts_visibility ON posts(resume_visibility);

-- Note: is_active is KEPT for backward-compatible soft-delete.
-- post_status drives lifecycle; is_active drives soft-delete.
