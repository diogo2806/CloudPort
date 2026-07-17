package br.com.cloudport.servicoyard.integracao.navio;

public record VisitaPlanejamento(Long identificador, Long navioCadastroId, String codigoVisita,
        String viagemEntrada, String viagemSaida, String fase, Long versao) {
}
