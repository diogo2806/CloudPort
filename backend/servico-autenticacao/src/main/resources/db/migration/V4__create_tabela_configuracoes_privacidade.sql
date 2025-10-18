CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE IF NOT EXISTS configuracoes_privacidade (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    descricao VARCHAR(255) NOT NULL,
    ativo BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT uk_configuracoes_privacidade_descricao UNIQUE (descricao)
);

INSERT INTO configuracoes_privacidade (descricao, ativo)
VALUES
    ('Permitir compartilhamento de dados com parceiros', FALSE),
    ('Mostrar minha atividade para outros usuários', FALSE),
    ('Receber relatórios de auditoria mensais', TRUE)
ON CONFLICT (descricao) DO NOTHING;
