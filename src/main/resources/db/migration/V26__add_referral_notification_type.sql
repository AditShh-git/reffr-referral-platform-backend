DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM pg_type
        WHERE typname = 'notification_type'
    ) THEN
ALTER TYPE notification_type
    ADD VALUE IF NOT EXISTS 'REFERRAL';
END IF;
END $$;