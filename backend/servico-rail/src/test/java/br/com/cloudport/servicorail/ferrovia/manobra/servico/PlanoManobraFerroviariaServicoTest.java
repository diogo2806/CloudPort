package br.com.cloudport.servicorail.ferrovia.manobra.servico;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import br.com.cloudport.servicorail.ferrovia.manobra.dto.PlanoManobraFerroviariaDto.Criacao;
import br.com.cloudport.servicorail.ferrovia.manobra.dto.PlanoManobraFerroviariaDto.Resposta;
import br.com.cloudport.servicorail.ferrovia.manobra.modelo.PlanoManobraFerroviaria;
import br.com.cloudport.servicorail.ferrovia.manobra.modelo.PlanoManobraFerroviaria.StatusPlanoManobra;
import br.com.cloudport.servicorail.ferrovia.manobra.repositorio.PlanoManobraFerroviariaRepositorio;
import br.com.cloudport.servicorail.ferrovia.modelo.VisitaTrem;
import br.com.cloudport.servicorail.ferrovia.repositorio.VisitaTremRepositorio;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PlanoManobraFerroviariaServicoTest {

    @Mock
    private PlanoManobraFerroviariaRepositorio planoRepositorio;

    @Mock
    private VisitaTremRepositorio visitaTremRepositorio;

    private PlanoManobraFerroviariaServico servico;

    @BeforeEach
    void configurar() {
        servico = new PlanoManobraFerroviariaServico(planoRepositorio, visitaTremRepositorio);
    }

    @Test
    void deveReservarTrechoQuandoNaoExisteConflito() {
        VisitaTrem visita = visita();
        Criacao criacao = criacao(LocalDateTime.of(2026, 7, 20, 14, 0),
                LocalDateTime.of(2026, 7, 20, 15, 0));
        prepararCriacao(visita, Collections.emptyList());

        Resposta resposta = servico.criar(19L, criacao);

        assertEquals(StatusPlanoManobra.PLANEJADA, resposta.getStatus());
        assertEquals("LINHA 1", resposta.getLinha());
        assertEquals("PATIO A-MOEGA", resposta.getTrecho());
    }

    @Test
    void deveBloquearPlanoQuandoTrechoJaEstaReservado() {
        VisitaTrem visita = visita();
        PlanoManobraFerroviaria existente = new PlanoManobraFerroviaria();
        existente.setVisitaTrem(visita);
        existente.setSequencia(1);
        existente.setLinha("LINHA 1");
        existente.setTrecho("PATIO A-MOEGA");
        existente.setInicioPrevisto(LocalDateTime.of(2026, 7, 20, 14, 0));
        existente.setFimPrevisto(LocalDateTime.of(2026, 7, 20, 15, 0));
        existente.setStatus(StatusPlanoManobra.AUTORIZADA);
        Criacao criacao = criacao(LocalDateTime.of(2026, 7, 20, 14, 30),
                LocalDateTime.of(2026, 7, 20, 15, 30));
        prepararCriacao(visita, List.of(existente));

        Resposta resposta = servico.criar(19L, criacao);

        assertEquals(StatusPlanoManobra.BLOQUEADA_CONFLITO, resposta.getStatus());
        assertNotNull(resposta.getConflitoDescricao());
    }

    private void prepararCriacao(VisitaTrem visita, List<PlanoManobraFerroviaria> existentes) {
        when(planoRepositorio.existsByVisitaTremIdAndSequencia(19L, 2)).thenReturn(false);
        when(visitaTremRepositorio.findOneById(19L)).thenReturn(Optional.of(visita));
        when(planoRepositorio.findByLinhaIgnoreCaseAndStatusInOrderByInicioPrevistoAsc(
                any(String.class), any())).thenReturn(existentes);
        when(planoRepositorio.saveAndFlush(any(PlanoManobraFerroviaria.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    private Criacao criacao(LocalDateTime inicio, LocalDateTime fim) {
        Criacao dto = new Criacao();
        dto.setSequencia(2);
        dto.setOrigem("Pátio A");
        dto.setDestino("Moega");
        dto.setComposicao("LOC-01 · 12 vagões");
        dto.setLinha("Linha 1");
        dto.setTrecho("Pátio A-Moega");
        dto.setInicioPrevisto(inicio);
        dto.setFimPrevisto(fim);
        return dto;
    }

    private VisitaTrem visita() {
        VisitaTrem visita = new VisitaTrem();
        visita.setId(19L);
        visita.setIdentificadorTrem("MRS-019");
        visita.setOperadoraFerroviaria("MRS Logística");
        return visita;
    }
}
