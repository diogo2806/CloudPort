package br.com.cloudport.servicoyard.dispatch.modelo;

/**
 * Ciclo de vida de uma instrução de movimentação no fluxo de dispatch (VMT):
 * planejada → despachada (atribuída a um equipamento) → em execução (içada/fetch)
 * → concluída (posicionada no destino), com cancelamento possível antes da conclusão.
 */
public enum StatusInstrucaoMovimentacao {
    PLANEJADA,
    DESPACHADA,
    EM_EXECUCAO,
    CONCLUIDA,
    CANCELADA
}
