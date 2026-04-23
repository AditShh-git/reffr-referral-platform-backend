-- v2

CREATE TYPE post_type AS ENUM ('REQUEST', 'OFFER');

CREATE TABLE posts (
                       id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                       author_id       UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                       type            post_type NOT NULL,
                       content         TEXT NOT NULL,
                       company         VARCHAR(150),
                       role            VARCHAR(150),
                       location        VARCHAR(150),
                       is_active       BOOLEAN DEFAULT TRUE,
                       views_count     INT DEFAULT 0,
                       referral_count  INT DEFAULT 0,
                       expires_at      TIMESTAMP DEFAULT (NOW() + INTERVAL '30 days'),
                       created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
                       updated_at      TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE post_tags (
                           id          BIGSERIAL PRIMARY KEY,
                           post_id     UUID NOT NULL REFERENCES posts(id) ON DELETE CASCADE,
                           tag         VARCHAR(80) NOT NULL,
                           UNIQUE(post_id, tag)
);

CREATE INDEX idx_posts_author      ON posts(author_id);
CREATE INDEX idx_posts_type        ON posts(type);
CREATE INDEX idx_posts_company     ON posts(company);
CREATE INDEX idx_posts_active      ON posts(is_active, created_at DESC);
CREATE INDEX idx_post_tags_post    ON post_tags(post_id);
CREATE INDEX idx_post_tags_tag     ON post_tags(tag);