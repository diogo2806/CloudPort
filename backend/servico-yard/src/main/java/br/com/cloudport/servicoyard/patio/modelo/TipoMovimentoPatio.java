package br.com.cloudport.servicoyard.patio.modelo;

/**
 * Tipos de operação sobre contêineres no pátio.
 * Unifica TipoMovimentoPatio e TipoOperacaoConteiner em uma única enumeração.
 */
public enum TipoMovimentoPatio {
    ALOCACAO,
    ATUALIZACAO,
    ATUALIZACAO_CADASTRAL,
    REMOCAO,
    REMANEJAMENTO,
    TRANSFERENCIA,
    INSPECAO,
    LIBERACAO
}
