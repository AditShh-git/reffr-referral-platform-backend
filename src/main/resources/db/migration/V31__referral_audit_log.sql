-- V31: Referral audit log

CREATE TABLE IF NOT EXISTS referral_audit_log (
                                                  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    referral_id UUID NOT NULL,

    actor_id UUID REFERENCES users(id) ON DELETE SET NULL,
    action VARCHAR(30) NOT NULL,
    old_status VARCHAR(20),
    new_status VARCHAR(20),
    note TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
    );

-- Add FK dynamically depending on which table exists
DO $$
BEGIN
    -- If new table exists → use it
    IF EXISTS (
        SELECT 1
        FROM information_schema.tables
        WHERE table_schema='public'
        AND table_name='referral_requests'
    ) THEN
ALTER TABLE referral_audit_log
    ADD CONSTRAINT fk_audit_referral_requests
        FOREIGN KEY (referral_id)
            REFERENCES referral_requests(id)
            ON DELETE CASCADE;

-- fallback for older DBs
ELSIF EXISTS (
        SELECT 1
        FROM information_schema.tables
        WHERE table_schema='public'
        AND table_name='referrals'
    ) THEN
ALTER TABLE referral_audit_log
    ADD CONSTRAINT fk_audit_referrals
        FOREIGN KEY (referral_id)
            REFERENCES referrals(id)
            ON DELETE CASCADE;
END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_audit_referral
    ON referral_audit_log(referral_id, created_at);

CREATE INDEX IF NOT EXISTS idx_audit_actor
    ON referral_audit_log(actor_id);