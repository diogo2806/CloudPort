package br.com.cloudport.servicoyard.edi.modelo;

public enum StatusProcessamentoEdi {
    RECEBIDO,
    PROCESSANDO,
    AGUARDANDO_REPROCESSAMENTO,
    CONCLUIDO,
    REJEITADO,
    QUARENTENA,
    CANCELADO
}
