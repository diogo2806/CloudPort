package br.com.cloudport.servicoyard.edi.modelo;

public enum StatusProcessamentoEdi {
    RECEBIDO,
    PROCESSANDO,
    AGUARDANDO_RETENTATIVA,
    CONCLUIDO,
    REJEITADO,
    QUARENTENA
}
