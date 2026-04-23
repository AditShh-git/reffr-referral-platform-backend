-- V18: email preferences, per-type throttle timestamps, and failed_emails audit table

ALTER TABLE users
    ADD COLUMN IF NOT EXISTS email_notifications_enabled BOOLEAN     NOT NULL DEFAULT TRUE,
    ADD COLUMN IF NOT EXISTS last_chat_email_at          TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS last_referral_email_at      TIMESTAMPTZ;

CREATE TABLE IF NOT EXISTS failed_emails (
    id              UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    recipient_email TEXT        NOT NULL,
    subject         VARCHAR(255),
    body            TEXT,
    error_message   TEXT,
    retry_count     INT         NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_failed_emails_created_at ON failed_emails (created_at ASC);
