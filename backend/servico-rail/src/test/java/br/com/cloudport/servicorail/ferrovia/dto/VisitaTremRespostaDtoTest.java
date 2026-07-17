package br.com.cloudport.servicorail.ferrovia.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.cloudport.servicorail.ferrovia.modelo.OperacaoConteinerVisita;
import br.com.cloudport.servicorail.ferrovia.modelo.StatusOperacaoConteinerVisita;
import br.com.cloudport.servicorail.ferrovia.modelo.StatusVisitaTrem;
import br.com.cloudport.servicorail.ferrovia.modelo.VagaoVisita;
import br.com.cloudport.servicorail.ferrovia.modelo.VisitaTrem;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class VisitaTremRespostaDtoTest {

    @Test
    void devePreservarTotaisMesmoQuandoAListagemNaoExpandeAsColecoes() {
        VisitaTrem visita = new VisitaTrem();
        visita.setId(19L);
        visita.setIdentificadorTrem("MRS-019");
        visita.setOperadoraFerroviaria("MRS Logística");
        visita.setHoraChegadaPrevista(LocalDateTime.of(2026, 7, 18, 8, 0));
        visita.setHoraPartidaPrevista(LocalDateTime.of(2026, 7, 18, 18, 0));
        visita.setStatusVisita(StatusVisitaTrem.CHEGOU);
        visita.definirListaVagoes(List.of(
                new VagaoVisita(1, "VAG-001", "PLATAFORMA"),
                new VagaoVisita(2, "VAG-002", "PLATAFORMA")
        ));
        visita.definirListaDescarga(List.of(
                new OperacaoConteinerVisita("MSCU0000001", StatusOperacaoConteinerVisita.PENDENTE, "VAG-001"),
                new OperacaoConteinerVisita("MSCU0000002", StatusOperacaoConteinerVisita.PENDENTE, "VAG-002")
        ));
        visita.definirListaCarga(List.of(
                new OperacaoConteinerVisita("MSCU0000003", StatusOperacaoConteinerVisita.PENDENTE, "VAG-001")
        ));

        VisitaTremRespostaDto resposta = VisitaTremRespostaDto.deEntidadeSemListas(visita);

        assertEquals(2, resposta.getQuantidadeVagoes());
        assertEquals(2, resposta.getQuantidadeDescarga());
        assertEquals(1, resposta.getQuantidadeCarga());
        assertTrue(resposta.getListaVagoes().isEmpty());
        assertTrue(resposta.getListaDescarga().isEmpty());
        assertTrue(resposta.getListaCarga().isEmpty());
    }
}
