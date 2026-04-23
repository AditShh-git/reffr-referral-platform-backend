-- V19: extend chat into a structured referral workflow

-- Track lifecycle stage within each chat
ALTER TABLE chats
    ADD COLUMN IF NOT EXISTS workflow_status VARCHAR(30) NOT NULL DEFAULT 'ACCEPTED';

-- Distinguish user messages from auto-generated system events
ALTER TABLE messages
    ADD COLUMN IF NOT EXISTS message_type VARCHAR(10) NOT NULL DEFAULT 'TEXT',
    ALTER COLUMN sender_id DROP NOT NULL;   -- SYSTEM messages have no human sender

CREATE INDEX IF NOT EXISTS idx_chats_workflow_status  ON chats    (workflow_status);
CREATE INDEX IF NOT EXISTS idx_messages_message_type  ON messages (message_type);
