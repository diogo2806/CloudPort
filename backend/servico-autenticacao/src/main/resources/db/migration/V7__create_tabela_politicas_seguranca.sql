CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE IF NOT EXISTS politicas_seguranca (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    titulo VARCHAR(150) NOT NULL,
    descricao VARCHAR(2000) NOT NULL,
    versao VARCHAR(25) NOT NULL,
    ordem_exibicao INTEGER NOT NULL,
    ativo BOOLEAN NOT NULL DEFAULT FALSE
);
