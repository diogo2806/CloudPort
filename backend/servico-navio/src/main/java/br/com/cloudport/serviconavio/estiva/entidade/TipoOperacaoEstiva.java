package br.com.cloudport.serviconavio.estiva.entidade;

/**
 * Natureza da operação de uma atribuição de estiva.
 * EMBARQUE: carga sai do pátio e vai para a célula do navio (baixa do estoque).
 * DESCARGA: carga sai da célula do navio e vai para o pátio (entrada no estoque).
 */
public enum TipoOperacaoEstiva {
    EMBARQUE,
    DESCARGA
}
