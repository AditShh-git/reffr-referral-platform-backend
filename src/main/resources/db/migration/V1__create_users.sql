-- v1
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pg_trgm";

-- ── Enums ──────────────────────────────────────────────────────────────
CREATE TYPE verification_status AS ENUM ('UNVERIFIED', 'PENDING', 'VERIFIED');
CREATE TYPE user_role            AS ENUM ('USER', 'ADMIN');

-- ── Users ──────────────────────────────────────────────────────────────
CREATE TABLE users (
                       id                      UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                       github_id               VARCHAR(100) UNIQUE NOT NULL,
                       github_username         VARCHAR(100) UNIQUE NOT NULL,
                       github_url              VARCHAR(255),
                       email                   VARCHAR(255) UNIQUE,
                       name                    VARCHAR(150) NOT NULL,
                       avatar_url              VARCHAR(500),
                       bio                     TEXT,
                       location                VARCHAR(150),

    -- Resume (stored in S3, key saved here)
                       resume_s3_key           VARCHAR(500),
                       resume_original_name    VARCHAR(255),
                       resume_uploaded_at      TIMESTAMP,

    -- Current job
                       current_company         VARCHAR(150),
                       current_job_role        VARCHAR(150),
                       years_of_experience     SMALLINT DEFAULT 0,

    -- Company verification
                       verified_company        VARCHAR(150),
                       verification_status     verification_status DEFAULT 'UNVERIFIED',
                       verification_email      VARCHAR(255),

    -- Referral reputation score
                       total_referrals_given   INT DEFAULT 0,
                       successful_referrals    INT DEFAULT 0,

    -- System fields
                       user_role               user_role DEFAULT 'USER',
                       is_active               BOOLEAN DEFAULT TRUE,
                       last_seen_at            TIMESTAMP,
                       created_at              TIMESTAMP NOT NULL DEFAULT NOW(),
                       updated_at              TIMESTAMP NOT NULL DEFAULT NOW()
);

-- ── User Skills ────────────────────────────────────────────────────────
CREATE TABLE user_skills (
                             id          BIGSERIAL PRIMARY KEY,
                             user_id     UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                             skill       VARCHAR(100) NOT NULL,
                             created_at  TIMESTAMP DEFAULT NOW(),
                             UNIQUE(user_id, skill)
);

-- ── User Work History ──────────────────────────────────────────────────
CREATE TABLE user_experiences (
                                  id          BIGSERIAL PRIMARY KEY,
                                  user_id     UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                                  company     VARCHAR(150) NOT NULL,
                                  job_role    VARCHAR(150) NOT NULL,
                                  start_year  SMALLINT,
                                  end_year    SMALLINT,
                                  is_current  BOOLEAN DEFAULT FALSE,
                                  created_at  TIMESTAMP DEFAULT NOW()
);

-- ── Indexes ────────────────────────────────────────────────────────────
CREATE INDEX idx_users_github_id        ON users(github_id);
CREATE INDEX idx_users_github_username  ON users(github_username);
CREATE INDEX idx_users_email            ON users(email);
CREATE INDEX idx_users_current_company  ON users(current_company);
CREATE INDEX idx_users_active           ON users(is_active);
CREATE INDEX idx_users_name_trgm        ON users USING gin(name gin_trgm_ops);
CREATE INDEX idx_user_skills_user       ON user_skills(user_id);
CREATE INDEX idx_user_skills_skill      ON user_skills(skill);
CREATE INDEX idx_user_experiences_user  ON user_experiences(user_id);