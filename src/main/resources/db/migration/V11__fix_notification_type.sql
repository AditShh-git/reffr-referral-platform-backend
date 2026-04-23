ALTER TABLE notifications
ALTER COLUMN type TYPE VARCHAR(50);

DROP TYPE IF EXISTS notification_type;