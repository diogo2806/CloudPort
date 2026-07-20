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
    ('navio/cadastros', 'Navios', 'navio/cadastros', 'CADASTROS', 'ROLE_ADMIN_PORTO,ROLE_PLANEJADOR', FALSE, NULL, 8, FALSE),
    ('patio/bercos', 'Berços Portuários', 'patio/bercos', 'CADASTROS', 'ROLE_ADMIN_PORTO,ROLE_PLANEJADOR', FALSE, NULL, 9, FALSE),
    ('gate/configuracao', 'Instalações, Gates e Pistas', 'gate/configuracao', 'CADASTROS', 'ROLE_ADMIN_PORTO', FALSE, NULL, 10, FALSE),
    ('patio/tipos-equipamentos', 'Tipos e Prefixos de Equipamentos', 'patio/tipos-equipamentos', 'CADASTROS', 'ROLE_ADMIN_PORTO,ROLE_PLANEJADOR', FALSE, NULL, 11, FALSE)
ON CONFLICT (identificador) DO UPDATE SET
    rotulo = EXCLUDED.rotulo,
    rota = EXCLUDED.rota,
    grupo = EXCLUDED.grupo,
    roles_permitidos = EXCLUDED.roles_permitidos,
    desabilitado = EXCLUDED.desabilitado,
    mensagem_em_breve = EXCLUDED.mensagem_em_breve;

UPDATE configuracoes_navegacao
SET grupo = 'CADASTROS',
    rotulo = CASE identificador
        WHEN 'patio/inventario' THEN 'Contêineres, Chassis e Carretas'
        WHEN 'patio/mapa' THEN 'Pátios e Posições'
        WHEN 'ferrovia/visitas' THEN 'Trens e Composições'
        ELSE rotulo
    END
WHERE identificador IN ('patio/inventario', 'patio/mapa', 'ferrovia/visitas');
