CREATE TABLE IF NOT EXISTS canais_notificacao (
    id BIGSERIAL PRIMARY KEY,
    nome_canal VARCHAR(120) NOT NULL UNIQUE,
    habilitado BOOLEAN NOT NULL DEFAULT TRUE
);

INSERT INTO canais_notificacao (nome_canal, habilitado) VALUES
    ('E-mail', TRUE),
    ('SMS', TRUE),
    ('Aplicativo móvel', TRUE);
