package br.com.cloudport.servicorail.ferrovia.locomotiva.porta;

public interface ConsultaVisitaNavioPorta {

    boolean existe(Long visitaNavioId, String codigoVisitaNavio);
}
