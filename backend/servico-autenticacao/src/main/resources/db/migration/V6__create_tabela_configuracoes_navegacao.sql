CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE IF NOT EXISTS configuracoes_navegacao (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    identificador VARCHAR(160) NOT NULL UNIQUE,
    rotulo VARCHAR(180) NOT NULL,
    rota VARCHAR(200) NOT NULL,
    grupo VARCHAR(80) NOT NULL,
    roles_permitidos VARCHAR(400) NOT NULL,
    desabilitado BOOLEAN NOT NULL DEFAULT FALSE,
    mensagem_em_breve VARCHAR(180),
    ordem INTEGER NOT NULL,
    padrao BOOLEAN NOT NULL DEFAULT FALSE
);

INSERT INTO configuracoes_navegacao (
    identificador,
    rotulo,
    rota,
    grupo,
    roles_permitidos,
    desabilitado,
    mensagem_em_breve,
    ordem,
    padrao
) VALUES
    ('role', 'Perfis de Acesso', 'role', 'CONFIGURACOES', 'ROLE_ADMIN_PORTO,ROLE_PLANEJADOR', FALSE, NULL, 1, TRUE),
    ('seguranca', 'Políticas de Segurança', 'seguranca', 'CONFIGURACOES', 'ROLE_ADMIN_PORTO', FALSE, NULL, 2, FALSE),
    ('notificacoes', 'Centro de Notificações', 'notificacoes', 'CONFIGURACOES', 'ROLE_ADMIN_PORTO,ROLE_PLANEJADOR', FALSE, NULL, 3, FALSE),
    ('privacidade', 'Preferências de Privacidade', 'privacidade', 'CONFIGURACOES', 'ROLE_ADMIN_PORTO', FALSE, NULL, 4, FALSE),
    ('catalogo-de-exames', 'Catálogo de Exames', 'catalogo-de-exames', 'CONFIGURACOES', 'ROLE_ADMIN_PORTO,ROLE_PLANEJADOR', TRUE, 'Em breve', 5, FALSE),
    ('medicos', 'Médicos', 'medicos', 'CONFIGURACOES', 'ROLE_ADMIN_PORTO,ROLE_PLANEJADOR', TRUE, 'Em breve', 6, FALSE),
    ('lista-de-usuarios', 'Lista de Usuários', 'lista-de-usuarios', 'USUARIOS', 'ROLE_ADMIN_PORTO', FALSE, NULL, 7, FALSE),
    ('gate/dashboard', 'Painel do Gate', 'gate/dashboard', 'GATE', 'ROLE_ADMIN_PORTO,ROLE_PLANEJADOR,ROLE_OPERADOR_GATE', FALSE, NULL, 8, FALSE),
    ('gate/agendamentos', 'Agendamentos do Gate', 'gate/agendamentos', 'GATE', 'ROLE_ADMIN_PORTO,ROLE_PLANEJADOR,ROLE_OPERADOR_GATE', FALSE, NULL, 9, FALSE),
    ('gate/janelas', 'Janelas de Atendimento', 'gate/janelas', 'GATE', 'ROLE_ADMIN_PORTO,ROLE_PLANEJADOR,ROLE_OPERADOR_GATE', FALSE, NULL, 10, FALSE),
    ('gate/relatorios', 'Relatórios do Gate', 'gate/relatorios', 'GATE', 'ROLE_ADMIN_PORTO,ROLE_PLANEJADOR,ROLE_OPERADOR_GATE', FALSE, NULL, 11, FALSE),
    ('gate/operador/console', 'Console do Operador', 'gate/operador/console', 'GATE', 'ROLE_ADMIN_PORTO,ROLE_PLANEJADOR,ROLE_OPERADOR_GATE', FALSE, NULL, 12, FALSE),
    ('gate/operador/eventos', 'Eventos do Operador', 'gate/operador/eventos', 'GATE', 'ROLE_ADMIN_PORTO,ROLE_PLANEJADOR,ROLE_OPERADOR_GATE', FALSE, NULL, 13, FALSE),
    ('ferrovia/visitas', 'Visitas de Trem', 'ferrovia/visitas', 'FERROVIA', 'ROLE_ADMIN_PORTO,ROLE_PLANEJADOR', FALSE, NULL, 14, FALSE),
    ('ferrovia/visitas/importar', 'Importar Manifesto de Visita', 'ferrovia/visitas/importar', 'FERROVIA', 'ROLE_ADMIN_PORTO,ROLE_PLANEJADOR', FALSE, NULL, 15, FALSE),
    ('patio/mapa', 'Mapa do Pátio', 'patio/mapa', 'PATIO', 'ROLE_ADMIN_PORTO,ROLE_PLANEJADOR', FALSE, NULL, 16, FALSE),
    ('patio/lista-trabalho', 'Lista de Trabalho do Pátio', 'patio/lista-trabalho', 'PATIO', 'ROLE_ADMIN_PORTO,ROLE_PLANEJADOR,ROLE_OPERADOR_PATIO', FALSE, NULL, 17, FALSE),
    ('patio/posicoes', 'Posições do Pátio', 'patio/posicoes', 'PATIO', 'ROLE_ADMIN_PORTO,ROLE_PLANEJADOR', FALSE, NULL, 18, FALSE),
    ('patio/movimentacoes', 'Movimentações do Pátio', 'patio/movimentacoes', 'PATIO', 'ROLE_ADMIN_PORTO,ROLE_PLANEJADOR', FALSE, NULL, 19, FALSE)
ON CONFLICT (identificador) DO NOTHING;
