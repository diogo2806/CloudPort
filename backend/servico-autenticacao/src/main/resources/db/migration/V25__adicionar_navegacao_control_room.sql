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
) VALUES (
    'control-room-equipamentos',
    'Equipamentos e telemetria',
    'control-room',
    'CONTROL ROOM',
    'ROLE_ADMIN_PORTO,ROLE_PLANEJADOR,ROLE_OPERADOR_PATIO,ROLE_OPERADOR_GATE',
    FALSE,
    NULL,
    15,
    FALSE
)
ON CONFLICT (identificador) DO UPDATE SET
    rotulo = EXCLUDED.rotulo,
    rota = EXCLUDED.rota,
    grupo = EXCLUDED.grupo,
    roles_permitidos = EXCLUDED.roles_permitidos,
    desabilitado = EXCLUDED.desabilitado,
    mensagem_em_breve = EXCLUDED.mensagem_em_breve,
    ordem = EXCLUDED.ordem;
