ALTER TABLE chats
    ADD COLUMN last_message TEXT;

ALTER TABLE chats
    ADD COLUMN last_message_at TIMESTAMP;

-- Index for sorting chats by recent activity
CREATE INDEX idx_chats_last_message_at
    ON chats(last_message_at DESC);