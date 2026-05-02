-- Remove any old FK still pointing to referrals table
ALTER TABLE chats
DROP CONSTRAINT IF EXISTS chats_referral_id_fkey;

ALTER TABLE chats
DROP CONSTRAINT IF EXISTS fk_chats_referrals;

ALTER TABLE chats
DROP CONSTRAINT IF EXISTS fk_chats_referral_requests;

-- Add correct FK
ALTER TABLE chats
    ADD CONSTRAINT fk_chats_referral_requests
        FOREIGN KEY (referral_id)
            REFERENCES referral_requests(id)
            ON DELETE CASCADE;