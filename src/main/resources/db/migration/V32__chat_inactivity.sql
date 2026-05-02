-- V32: Chat inactivity tracking
-- last_activity_at allows the inactivity scheduler to flag dead chats.

ALTER TABLE chats
    ADD COLUMN IF NOT EXISTS last_activity_at TIMESTAMPTZ;

-- Backfill: prefer last_message_at if available, else created_at
UPDATE chats
    SET last_activity_at = COALESCE(last_message_at, created_at);

CREATE INDEX IF NOT EXISTS idx_chats_last_activity ON chats(last_activity_at)
    WHERE workflow_status = 'ACCEPTED';
