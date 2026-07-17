-- Disponibiliza a operação Gate -> Navio sem passagem pelo pátio.
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
    ('gate/embarque-direto', 'Embarque direto', 'gate/embarque-direto', 'GATE',
     'ROLE_ADMIN_PORTO,ROLE_OPERADOR_GATE', FALSE, NULL, 35, FALSE)
ON CONFLICT (identificador) DO UPDATE SET
    rotulo = EXCLUDED.rotulo,
    rota = EXCLUDED.rota,
    grupo = EXCLUDED.grupo,
    roles_permitidos = EXCLUDED.roles_permitidos,
    desabilitado = EXCLUDED.desabilitado,
    mensagem_em_breve = EXCLUDED.mensagem_em_breve,
    ordem = EXCLUDED.ordem;
