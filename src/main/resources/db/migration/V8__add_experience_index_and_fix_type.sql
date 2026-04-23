-- Fix type if needed
ALTER TABLE posts
ALTER COLUMN type TYPE VARCHAR(10);

DROP TYPE IF EXISTS post_type;

-- Add index
CREATE INDEX IF NOT EXISTS idx_posts_experience
    ON posts(min_experience, max_experience);