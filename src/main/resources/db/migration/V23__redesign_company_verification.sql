-- 1. Create the new UserCompanyVerification table
CREATE TABLE user_company_verifications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    company VARCHAR(150) NOT NULL,
    job_role VARCHAR(150) NOT NULL,
    start_year SMALLINT,
    end_year SMALLINT,
    is_current BOOLEAN DEFAULT FALSE,
    
    proof_type VARCHAR(50), -- EMAIL, DOCUMENT, PUBLIC, PEER
    is_verified BOOLEAN DEFAULT FALSE,
    verification_status VARCHAR(50) NOT NULL, -- CURRENT, PAST, UNVERIFIED
    
    email VARCHAR(255),
    document_key VARCHAR(500),
    profile_url VARCHAR(500),
    
    verified_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- 2. Migrate existing work history from user_experiences
INSERT INTO user_company_verifications (
    user_id, company, job_role, start_year, end_year, is_current, 
    verification_status, is_verified, created_at, updated_at
)
SELECT 
    user_id, company, job_role, start_year, end_year, is_current,
    CASE WHEN is_current THEN 'CURRENT' ELSE 'PAST' END,
    FALSE, -- Initially mark legacy experiences as unverified
    NOW(), NOW()
FROM user_experiences;

-- 3. Migrate legacy verification data from users table
-- If a user has a verified_company, we create a verified record for it.
-- If they already had it in experiences, we might get duplicates, but the rule says "single source of truth".
-- We'll try to match by company name if possible, or just insert a new verified record.
INSERT INTO user_company_verifications (
    user_id, company, job_role, is_current, verification_status, 
    is_verified, email, verified_at, proof_type, created_at, updated_at
)
SELECT 
    id, verified_company, current_job_role, TRUE, 'CURRENT',
    TRUE, verification_email, NOW(), 'EMAIL', NOW(), NOW()
FROM users
WHERE verified_company IS NOT NULL AND verification_status = 'VERIFIED';

-- 4. Create index for performance
CREATE INDEX idx_user_company_verifications_user_id ON user_company_verifications(user_id);
CREATE INDEX idx_user_company_verifications_status ON user_company_verifications(verification_status);
