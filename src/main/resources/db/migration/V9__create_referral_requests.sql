CREATE TABLE referral_requests (
                                   id UUID PRIMARY KEY,

                                   post_id UUID NOT NULL,
                                   requester_id UUID NOT NULL,
                                   referrer_id UUID NOT NULL,

                                   status VARCHAR(20) NOT NULL,
                                   message TEXT,

                                   created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                   updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

                                   CONSTRAINT fk_ref_post FOREIGN KEY (post_id) REFERENCES posts(id),
                                   CONSTRAINT fk_ref_requester FOREIGN KEY (requester_id) REFERENCES users(id),
                                   CONSTRAINT fk_ref_referrer FOREIGN KEY (referrer_id) REFERENCES users(id),

                                   CONSTRAINT uk_referral_unique UNIQUE (post_id, requester_id, referrer_id)
);

-- Indexes
CREATE INDEX idx_ref_post ON referral_requests(post_id);
CREATE INDEX idx_ref_requester ON referral_requests(requester_id);
CREATE INDEX idx_ref_referrer ON referral_requests(referrer_id);
CREATE INDEX idx_ref_status ON referral_requests(status);