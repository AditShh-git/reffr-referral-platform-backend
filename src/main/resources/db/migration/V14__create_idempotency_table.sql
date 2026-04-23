CREATE TABLE idempotency_records (
                                     key VARCHAR(255) PRIMARY KEY,
                                     created_at TIMESTAMP,
                                     response_body TEXT,
                                     status_code INT
);