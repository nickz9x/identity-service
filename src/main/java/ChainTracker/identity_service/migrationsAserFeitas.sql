-- 1. Tabela para os VÍNCULOS DEFINITIVOS com o Telegram
CREATE TABLE user_telegram_links (
                                     user_id VARCHAR(36) NOT NULL PRIMARY KEY, -- UUID do usuário no Keycloak (claim 'sub')
                                     telegram_chat_id BIGINT NOT NULL UNIQUE,  -- ID do chat/usuário no Telegram
                                     linked_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 2. Tabela para os CÓDIGOS TEMPORÁRIOS de verificação
CREATE TABLE telegram_link_codes (
                                     id SERIAL PRIMARY KEY,
                                     user_id VARCHAR(36) NOT NULL,              -- UUID do usuário no Keycloak
                                     code VARCHAR(10) NOT NULL,                 -- O código gerado (ex: "839201")
                                     expires_at TIMESTAMP NOT NULL,             -- Data/hora limite de validade
                                     created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                     CONSTRAINT uq_user_code UNIQUE (user_id, code)
);

-- Índice para acelerar a busca rápida do código enviada pelo Bot
CREATE INDEX idx_telegram_code ON telegram_link_codes(code);