UPDATE configuracoes_navegacao
SET grupo = 'CADASTROS'
WHERE identificador IN (
    'role',
    'lista-de-usuarios',
    'gate/janelas',
    'patio/posicoes',
    'patio/recursos'
);
