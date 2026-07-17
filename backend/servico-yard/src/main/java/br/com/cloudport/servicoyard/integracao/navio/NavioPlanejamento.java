package br.com.cloudport.servicoyard.integracao.navio;

public record NavioPlanejamento(
        Long identificador,
        String nome,
        String codigoImo,
        String callSign,
        Long versao) {
}
