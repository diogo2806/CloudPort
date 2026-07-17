package br.com.cloudport.servicoyard.integracao.navio;

public record ContextoPlanejamentoNavio(
        NavioPlanejamento navio,
        VisitaPlanejamento visita,
        String codigoViagem) {
}
