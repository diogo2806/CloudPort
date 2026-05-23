package br.com.cloudport.servicoyard.dispatch.modelo;

/**
 * Tipos de movimentação ("move kind") alinhados ao Navis N4 / VMT:
 * RECV (recebimento no gate), DLCV (entrega no gate), DSCH (descarga de navio),
 * LOAD (embarque em navio), RDSC (descarga ferroviária), RLOD (embarque ferroviário)
 * e YARD (movimentação interna de pátio / housekeeping).
 */
public enum TipoMoveVmt {
    RECEBIMENTO,
    ENTREGA,
    DESCARGA_NAVIO,
    EMBARQUE_NAVIO,
    DESCARGA_FERROVIA,
    EMBARQUE_FERROVIA,
    MOVIMENTACAO_PATIO
}
