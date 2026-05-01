ALTER TABLE user_skills
    ADD COLUMN IF NOT EXISTS is_verified BOOLEAN NOT NULL DEFAULT FALSE;

-- Only update rows if column was just added or still false
UPDATE user_skills
SET is_verified = TRUE
WHERE is_verified = FALSE;