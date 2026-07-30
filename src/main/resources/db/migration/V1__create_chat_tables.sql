CREATE TABLE chat_conversations (
    id BIGINT NOT NULL AUTO_INCREMENT,
    conversation_date DATE NOT NULL,
    title VARCHAR(160) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_chat_conversations_date UNIQUE (conversation_date)
);

CREATE TABLE chat_messages (
    id BIGINT NOT NULL AUTO_INCREMENT,
    conversation_id BIGINT NOT NULL,
    role VARCHAR(16) NOT NULL,
    content LONGTEXT NOT NULL,
    model_name VARCHAR(120),
    created_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_chat_messages_conversation
        FOREIGN KEY (conversation_id)
        REFERENCES chat_conversations (id)
        ON DELETE CASCADE
);

CREATE INDEX idx_chat_messages_conversation_created
    ON chat_messages (conversation_id, created_at);
