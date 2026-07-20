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
    ('patio/tipos-equipamentos', 'Tipos e Prefixos de Equipamentos', 'patio/tipos-equipamentos', 'CADASTROS', 'ROLE_ADMIN_PORTO,ROLE_PLANEJADOR', FALSE, NULL, 11, FALSE),
    ('cadastros/unidades-equipamentos', 'Contêineres, Chassis e Carretas', 'patio/inventario', 'CADASTROS', 'ROLE_ADMIN_PORTO,ROLE_PLANEJADOR,ROLE_OPERADOR_PATIO', FALSE, NULL, 12, FALSE),
    ('cadastros/patios-posicoes', 'Pátios e Posições', 'patio/mapa', 'CADASTROS', 'ROLE_ADMIN_PORTO,ROLE_PLANEJADOR,ROLE_OPERADOR_PATIO', FALSE, NULL, 13, FALSE),
    ('cadastros/trens-composicoes', 'Trens e Composições', 'ferrovia/visitas', 'CADASTROS', 'ROLE_ADMIN_PORTO,ROLE_PLANEJADOR,ROLE_OPERADOR_PATIO', FALSE, NULL, 14, FALSE)
ON CONFLICT (identificador) DO UPDATE SET
    rotulo = EXCLUDED.rotulo,
    rota = EXCLUDED.rota,
    grupo = EXCLUDED.grupo,
    roles_permitidos = EXCLUDED.roles_permitidos,
    desabilitado = FALSE,
    mensagem_em_breve = NULL,
    ordem = EXCLUDED.ordem,
    padrao = EXCLUDED.padrao;
