package br.com.cloudport.visibilidade.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.cloudport.visibilidade.entity.ConteinerLocalizacao;
import br.com.cloudport.visibilidade.entity.HistoricoMovimento;
import br.com.cloudport.visibilidade.repository.ConteinerLocalizacaoRepository;
import br.com.cloudport.visibilidade.repository.HistoricoMovimentoRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MovimentoConteinerServiceTest {

    @Mock
    private ConteinerLocalizacaoRepository localizacaoRepository;

    @Mock
    private HistoricoMovimentoRepository historicoRepository;

    @Test
    void devePersistirLocalizacaoEHistoricoAoArmazenarNoPatio() {
        when(localizacaoRepository.findByContainerId("CONT001")).thenReturn(Optional.empty());
        MovimentoConteinerService service = new MovimentoConteinerService(
                localizacaoRepository, historicoRepository);

        service.registrarArmazenagemYard(
                "evt-yard-1", "CONT001", "A01", "01-02-03", "RTG-01", "operador");

        ArgumentCaptor<ConteinerLocalizacao> localizacaoCaptor =
                ArgumentCaptor.forClass(ConteinerLocalizacao.class);
        verify(localizacaoRepository).save(localizacaoCaptor.capture());
        ConteinerLocalizacao localizacao = localizacaoCaptor.getValue();
        assertEquals("CONT001", localizacao.getContainerId());
        assertEquals(MovimentoConteinerService.STATUS_NO_YARD, localizacao.getStatusAtual());
        assertEquals("A01", localizacao.getZona());
        assertEquals("01-02-03", localizacao.getPosicao());
        assertNotNull(localizacao.getDataAtualizacao());

        ArgumentCaptor<HistoricoMovimento> historicoCaptor =
                ArgumentCaptor.forClass(HistoricoMovimento.class);
        verify(historicoRepository).save(historicoCaptor.capture());
        HistoricoMovimento historico = historicoCaptor.getValue();
        assertEquals("evt-yard-1", historico.getEventoId());
        assertEquals(MovimentoConteinerService.TIPO_ARMAZENAGEM_YARD, historico.getTipo());
        assertEquals("A01-01-02-03", historico.getLocalizacao());
        assertEquals("RTG-01", historico.getEquipamentoUsado());
        assertEquals("operador", historico.getResponsavel());
    }

    @Test
    void deveRegistrarMovimentoRailMesmoQuandoConteinerAindaNaoExisteNaProjecao() {
        when(localizacaoRepository.findByContainerId("CONT002")).thenReturn(Optional.empty());
        when(localizacaoRepository.save(any(ConteinerLocalizacao.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        MovimentoConteinerService service = new MovimentoConteinerService(
                localizacaoRepository, historicoRepository);

        service.registrarMovimentoRail(
                "evt-rail-2", "CONT002", "LINHA-1", "PATIO-B", "LOCO-7", null);

        ArgumentCaptor<ConteinerLocalizacao> localizacaoCaptor =
                ArgumentCaptor.forClass(ConteinerLocalizacao.class);
        verify(localizacaoRepository).save(localizacaoCaptor.capture());
        assertEquals(MovimentoConteinerService.STATUS_EM_TRANSITO_RAIL,
                localizacaoCaptor.getValue().getStatusAtual());
        assertEquals("PATIO-B", localizacaoCaptor.getValue().getZona());

        ArgumentCaptor<HistoricoMovimento> historicoCaptor =
                ArgumentCaptor.forClass(HistoricoMovimento.class);
        verify(historicoRepository).save(historicoCaptor.capture());
        assertEquals("evt-rail-2", historicoCaptor.getValue().getEventoId());
        assertEquals("LINHA-1 -> PATIO-B", historicoCaptor.getValue().getLocalizacao());
        assertEquals(MovimentoConteinerService.TIPO_MOVIMENTO_RAIL,
                historicoCaptor.getValue().getTipo());
    }
}
