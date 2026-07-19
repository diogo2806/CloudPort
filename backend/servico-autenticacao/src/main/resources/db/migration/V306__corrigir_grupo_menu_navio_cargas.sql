-- Corrige instalações em que o título do grupo de navegação foi persistido
-- de forma truncada e passou a aparecer apenas como "N" no menu lateral.
UPDATE configuracoes_navegacao
SET grupo = 'Navio e cargas'
WHERE identificador IN (
    'navio/line-up',
    'embarque/planejamento',
    'embarque/steel-coils'
)
AND grupo IS DISTINCT FROM 'Navio e cargas';
