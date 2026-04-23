-- V12__add_message_status.sql

ALTER TABLE messages
    ADD COLUMN status VARCHAR(20) DEFAULT 'SENT';

CREATE INDEX idx_messages_status ON messages(status);