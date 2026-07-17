package br.com.cloudport.servicorail.ferrovia.servico;

import br.com.cloudport.servicorail.ferrovia.dto.VisitaTremRespostaDto;
import br.com.cloudport.servicorail.ferrovia.modelo.OperacaoConteinerVisita;
import br.com.cloudport.servicorail.ferrovia.modelo.StatusOperacaoConteinerVisita;
import br.com.cloudport.servicorail.ferrovia.modelo.StatusVisitaTrem;
import br.com.cloudport.servicorail.ferrovia.modelo.VisitaTrem;
import br.com.cloudport.servicorail.ferrovia.repositorio.VisitaTremRepositorio;
import java.util.ArrayList;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class PartidaTremServico {

    private final VisitaTremRepositorio visitaTremRepositorio;

    public PartidaTremServico(VisitaTremRepositorio visitaTremRepositorio) {
        this.visitaTremRepositorio = visitaTremRepositorio;
    }

    @Transactional
    public VisitaTremRespostaDto registrarPartida(Long idVisita) {
        if (idVisita == null || idVisita <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Identificador da visita inválido.");
        }
        VisitaTrem visita = visitaTremRepositorio.buscarPorIdComListas(idVisita)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Visita de trem não encontrada."));
        if (visita.getStatusVisita() != StatusVisitaTrem.CONCLUIDO) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "A partida só pode ser registrada após a conclusão de todas as movimentações.");
        }
        List<OperacaoConteinerVisita> operacoes = new ArrayList<>();
        operacoes.addAll(visita.getListaDescarga());
        operacoes.addAll(visita.getListaCarga());
        boolean possuiOperacaoPendente = operacoes.stream()
                .anyMatch(item -> item.getStatusOperacao() != StatusOperacaoConteinerVisita.CONCLUIDO);
        if (possuiOperacaoPendente) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Existem operações do manifesto ainda não concluídas.");
        }
        visita.setStatusVisita(StatusVisitaTrem.PARTIU);
        return VisitaTremRespostaDto.deEntidade(visitaTremRepositorio.save(visita));
    }
}
