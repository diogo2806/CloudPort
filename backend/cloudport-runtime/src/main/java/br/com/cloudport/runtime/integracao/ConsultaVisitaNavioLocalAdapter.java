package br.com.cloudport.runtime.integracao;

import br.com.cloudport.serviconaviosiderurgico.repositorio.VisitaNavioRepositorio;
import br.com.cloudport.servicorail.ferrovia.locomotiva.porta.ConsultaVisitaNavioPorta;
import org.springframework.stereotype.Component;

@Component
public class ConsultaVisitaNavioLocalAdapter implements ConsultaVisitaNavioPorta {

    private final VisitaNavioRepositorio visitaNavioRepositorio;

    public ConsultaVisitaNavioLocalAdapter(VisitaNavioRepositorio visitaNavioRepositorio) {
        this.visitaNavioRepositorio = visitaNavioRepositorio;
    }

    @Override
    public boolean existe(Long visitaNavioId, String codigoVisitaNavio) {
        if (visitaNavioId == null || codigoVisitaNavio == null) {
            return false;
        }
        return visitaNavioRepositorio.findById(visitaNavioId)
                .map(visita -> codigoVisitaNavio.equalsIgnoreCase(visita.getCodigoVisita()))
                .orElse(false);
    }
}
