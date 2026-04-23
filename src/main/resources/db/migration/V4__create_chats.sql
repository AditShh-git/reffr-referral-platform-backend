CREATE TABLE chats (
                       id          UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                       referral_id UUID NOT NULL UNIQUE REFERENCES referrals(id) ON DELETE CASCADE,
                       seeker_id   UUID NOT NULL REFERENCES users(id),
                       referrer_id UUID NOT NULL REFERENCES users(id),
                       is_active   BOOLEAN DEFAULT TRUE,
                       created_at  TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE messages (
                          id          UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                          chat_id     UUID NOT NULL REFERENCES chats(id) ON DELETE CASCADE,
                          sender_id   UUID NOT NULL REFERENCES users(id),
                          content     TEXT NOT NULL,
                          is_read     BOOLEAN DEFAULT FALSE,
                          read_at     TIMESTAMP,
                          created_at  TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_chats_referral   ON chats(referral_id);
CREATE INDEX idx_chats_seeker     ON chats(seeker_id);
CREATE INDEX idx_chats_referrer   ON chats(referrer_id);
CREATE INDEX idx_messages_chat    ON messages(chat_id, created_at DESC);
CREATE INDEX idx_messages_sender  ON messages(sender_id);
CREATE INDEX idx_messages_unread  ON messages(chat_id, is_read) WHERE is_read = FALSE;