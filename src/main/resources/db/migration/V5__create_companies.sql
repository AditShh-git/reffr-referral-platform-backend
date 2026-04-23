-- ── Companies ──────────────────────────────────────────────────────────
CREATE TABLE companies (
                           id          BIGSERIAL PRIMARY KEY,
                           name        VARCHAR(150) UNIQUE NOT NULL,
                           domain      VARCHAR(100) UNIQUE,
                           logo_url    VARCHAR(500),
                           website     VARCHAR(255),
                           industry    VARCHAR(100),
                           is_verified BOOLEAN DEFAULT FALSE,
                           created_at  TIMESTAMP DEFAULT NOW()
);

CREATE TYPE comp_verify_method AS ENUM ('EMAIL_DOMAIN', 'MANUAL_REVIEW');

CREATE TABLE company_verifications (
                                       id                  BIGSERIAL PRIMARY KEY,
                                       user_id             UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                                       company_id          BIGINT NOT NULL REFERENCES companies(id),
                                       method              comp_verify_method DEFAULT 'EMAIL_DOMAIN',
                                       work_email          VARCHAR(255),
                                       verification_code   VARCHAR(10),
                                       code_expires_at     TIMESTAMP,
                                       is_verified         BOOLEAN DEFAULT FALSE,
                                       verified_at         TIMESTAMP,
                                       created_at          TIMESTAMP DEFAULT NOW()
);

-- ── Notifications ──────────────────────────────────────────────────────
CREATE TYPE notification_type AS ENUM (
    'REFERRAL_REQUEST_RECEIVED',
    'REFERRAL_ACCEPTED',
    'REFERRAL_REJECTED',
    'REFERRAL_SUBMITTED',
    'REFERRAL_STATUS_UPDATE',
    'NEW_MESSAGE',
    'PROFILE_VIEWED'
);

CREATE TABLE notifications (
                               id          UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                               user_id     UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                               type        notification_type NOT NULL,
                               title       VARCHAR(200) NOT NULL,
                               body        VARCHAR(500),
                               entity_type VARCHAR(50),
                               entity_id   VARCHAR(100),
                               is_read     BOOLEAN DEFAULT FALSE,
                               read_at     TIMESTAMP,
                               created_at  TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_companies_name       ON companies(name);
CREATE INDEX idx_companies_domain     ON companies(domain);
CREATE INDEX idx_comp_verify_user     ON company_verifications(user_id);
CREATE INDEX idx_notifications_user   ON notifications(user_id, created_at DESC);
CREATE INDEX idx_notifications_unread ON notifications(user_id, is_read) WHERE is_read = FALSE;