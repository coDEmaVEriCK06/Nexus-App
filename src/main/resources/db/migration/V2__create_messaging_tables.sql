CREATE TABLE conversations (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    type        VARCHAR(20)  NOT NULL,
    name        VARCHAR(100),
    direct_key  VARCHAR(64)  UNIQUE,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT conversations_type_check CHECK (type IN ('DIRECT', 'GROUP'))
);

CREATE TABLE conversation_members (
    id               BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    conversation_id  BIGINT       NOT NULL REFERENCES conversations(id) ON DELETE CASCADE,
    user_id          BIGINT       NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role             VARCHAR(20)  NOT NULL DEFAULT 'MEMBER',
    joined_at        TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT conversation_members_role_check CHECK (role IN ('MEMBER', 'ADMIN')),
    CONSTRAINT conversation_members_unique UNIQUE (conversation_id, user_id)
);

CREATE INDEX idx_conversation_members_user ON conversation_members(user_id);

CREATE TABLE messages (
    id               BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    conversation_id  BIGINT        NOT NULL REFERENCES conversations(id) ON DELETE CASCADE,
    sender_id        BIGINT        NOT NULL REFERENCES users(id),
    content          VARCHAR(8000) NOT NULL,
    created_at       TIMESTAMPTZ   NOT NULL DEFAULT now()
);

CREATE INDEX idx_messages_conversation_created ON messages(conversation_id, created_at);
