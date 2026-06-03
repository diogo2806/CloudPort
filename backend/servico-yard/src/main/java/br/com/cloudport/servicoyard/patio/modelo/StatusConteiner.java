package br.com.cloudport.servicoyard.patio.modelo;

/**
 * Ciclo de vida unificado de um contêiner no pátio.
 * Substitui StatusOperacionalConteiner (ALOCADO/INSPECIONADO/LIBERADO)
 * e os valores operacionais anteriores (AGUARDANDO_RETIRADA, etc.).
 */
public enum StatusConteiner {
    ALOCADO,
    ARMAZENADO,
    RESERVADO,
    INSPECIONANDO,
    INSPECIONADO,
    AGUARDANDO_RETIRADA,
    LIBERADO,
    DESPACHADO,
    RETIDO,
    DANIFICADO
}
