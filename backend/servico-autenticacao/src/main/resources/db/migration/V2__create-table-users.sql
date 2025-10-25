CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    login VARCHAR(120) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    nome VARCHAR(120),
    transportadora_documento VARCHAR(20),
    transportadora_nome VARCHAR(120)
);
