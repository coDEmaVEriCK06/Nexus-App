ALTER TABLE conversations
    ADD COLUMN last_message_at      TIMESTAMPTZ,
    ADD COLUMN last_message_preview VARCHAR(8000),
    ADD COLUMN last_message_sender  VARCHAR(50),
    ADD COLUMN last_message_type    VARCHAR(20);

-- backfill the snapshot from the newest message in each conversation
UPDATE conversations c SET
    last_message_at      = lm.created_at,
    last_message_preview = lm.content,
    last_message_sender  = u.username,
    last_message_type    = lm.type
FROM (
    SELECT DISTINCT ON (m.conversation_id)
        m.conversation_id, m.content, m.created_at, m.sender_id, m.type
    FROM messages m
    ORDER BY m.conversation_id, m.created_at DESC, m.id DESC
) lm
JOIN users u ON u.id = lm.sender_id
WHERE c.id = lm.conversation_id;
