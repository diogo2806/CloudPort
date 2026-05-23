-- A tela de plano de estiva passou a cobrir embarque e descarga; ajusta o rótulo
-- da aba para refletir as duas operações.
UPDATE configuracoes_navegacao
   SET rotulo = 'Planejamento de Embarque/Descarga'
 WHERE identificador = 'embarque/planejamento';
