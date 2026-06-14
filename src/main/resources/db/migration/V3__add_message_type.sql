ALTER TABLE messages
    ADD COLUMN type VARCHAR(20) NOT NULL DEFAULT 'USER';

ALTER TABLE messages
    ADD CONSTRAINT messages_type_check CHECK (type IN ('USER', 'SYSTEM'));
