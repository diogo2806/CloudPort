package br.com.cloudport.serviconavio.estiva.entidade;

/**
 * Estados do plano de estiva (stowage/loading plan) de uma escala.
 * RASCUNHO -> CONFIRMADO -> EM_EXECUCAO -> CONCLUIDO.
 */
public enum StatusPlanoEstiva {
    RASCUNHO,
    CONFIRMADO,
    EM_EXECUCAO,
    CONCLUIDO
}
