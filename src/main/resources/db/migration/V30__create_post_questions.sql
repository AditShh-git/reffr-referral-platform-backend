-- V30: Post Questions — lightweight async Q&A on posts
-- Replaces need for chat just to ask "is this still open?"

CREATE TABLE IF NOT EXISTS post_questions (
    id            UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    post_id       UUID        NOT NULL REFERENCES posts(id) ON DELETE CASCADE,
    asker_id      UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    question_text TEXT        NOT NULL,
    answer        TEXT,
    answered_at   TIMESTAMPTZ,
    is_visible    BOOLEAN     NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_pq_post        ON post_questions(post_id, created_at ASC);
CREATE INDEX IF NOT EXISTS idx_pq_asker       ON post_questions(asker_id);
CREATE INDEX IF NOT EXISTS idx_pq_unanswered  ON post_questions(post_id) WHERE answer IS NULL;
