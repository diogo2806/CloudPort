UPDATE configuracoes_navegacao
SET ordem = ordem + 1
WHERE ordem >= 12;

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
    'gate/pessoas',
    'Controle de Pessoas',
    'gate/pessoas',
    'GATE',
    'ROLE_ADMIN_PORTO,ROLE_PLANEJADOR,ROLE_OPERADOR_GATE',
    FALSE,
    NULL,
    12,
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
