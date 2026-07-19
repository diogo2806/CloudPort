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
    ('integracoes/edi', 'Painel EDI', 'integracoes/edi', 'INTEGRACOES', 'ROLE_ADMIN_PORTO,ROLE_PLANEJADOR', FALSE, NULL, 90, FALSE),
    ('integracoes/api-publica', 'Diagnóstico da API Pública', 'integracoes/api-publica', 'INTEGRACOES', 'ROLE_ADMIN_PORTO', FALSE, NULL, 91, FALSE)
ON CONFLICT (identificador) DO UPDATE SET
    rotulo = EXCLUDED.rotulo,
    rota = EXCLUDED.rota,
    grupo = EXCLUDED.grupo,
    roles_permitidos = EXCLUDED.roles_permitidos,
    desabilitado = EXCLUDED.desabilitado,
    mensagem_em_breve = EXCLUDED.mensagem_em_breve,
    ordem = EXCLUDED.ordem;
