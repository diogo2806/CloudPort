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
    'ferrovia/locomotivas',
    'Locomotivas para Navio',
    'ferrovia/locomotivas',
    'FERROVIA',
    'ROLE_ADMIN_PORTO,ROLE_PLANEJADOR,ROLE_OPERADOR_PATIO',
    FALSE,
    NULL,
    16,
    FALSE
)
ON CONFLICT (identificador) DO UPDATE SET
    rotulo = EXCLUDED.rotulo,
    rota = EXCLUDED.rota,
    grupo = EXCLUDED.grupo,
    roles_permitidos = EXCLUDED.roles_permitidos,
    desabilitado = EXCLUDED.desabilitado,
    mensagem_em_breve = EXCLUDED.mensagem_em_breve;

UPDATE configuracoes_navegacao
SET ordem = ordem + 1
WHERE grupo = 'PATIO'
  AND ordem >= 16;
