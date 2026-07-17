package br.com.cloudport.servicoyard.integracao.navio;

public interface ConsultaPlanejamentoNavioPorta {
    NavioPlanejamento buscarNavioPorId(Long identificador);
    NavioPlanejamento buscarNavioPorImo(String codigoImo);
    VisitaPlanejamento buscarVisitaPorId(Long identificador);
}
