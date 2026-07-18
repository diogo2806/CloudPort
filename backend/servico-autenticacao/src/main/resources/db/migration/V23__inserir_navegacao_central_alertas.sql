INSERT INTO configuracao_navegacao (
    identificador,
    rotulo,
    grupo,
    rota,
    ordem,
    desabilitado,
    roles_permitidos
)
SELECT
    'central-alertas',
    'Central de alertas',
    'Visão geral',
    ARRAY['alertas'],
    20,
    FALSE,
    ARRAY[]::VARCHAR[]
WHERE NOT EXISTS (
    SELECT 1
    FROM configuracao_navegacao
    WHERE identificador = 'central-alertas'
);
