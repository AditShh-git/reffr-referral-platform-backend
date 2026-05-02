-- Remove old wrong FK
ALTER TABLE referral_audit_log
DROP CONSTRAINT IF EXISTS fk_audit_referrals;

ALTER TABLE referral_audit_log
DROP CONSTRAINT IF EXISTS referral_audit_log_referral_id_fkey;

ALTER TABLE referral_audit_log
DROP CONSTRAINT IF EXISTS fk_audit_referral_requests;

-- Recreate correct FK
ALTER TABLE referral_audit_log
    ADD CONSTRAINT fk_audit_referral_requests
        FOREIGN KEY (referral_id)
            REFERENCES referral_requests(id)
            ON DELETE CASCADE;