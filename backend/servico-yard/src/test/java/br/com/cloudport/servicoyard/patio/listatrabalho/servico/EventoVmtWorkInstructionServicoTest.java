package br.com.cloudport.servicoyard.patio.listatrabalho.servico;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.cloudport.servicoyard.patio.listatrabalho.dto.EventoVmtWorkInstructionRequest;
import br.com.cloudport.servicoyard.patio.listatrabalho.modelo.EventoVmtWorkInstruction;
import br.com.cloudport.servicoyard.patio.listatrabalho.modelo.HistoricoOperacaoPatio;
import br.com.cloudport.servicoyard.patio.listatrabalho.modelo.OrdemTrabalhoPatio;
import br.com.cloudport.servicoyard.patio.listatrabalho.modelo.StatusConfirmacaoVmt;
import br.com.cloudport.servicoyard.patio.listatrabalho.modelo.StatusOrdemTrabalhoPatio;
import br.com.cloudport.servicoyard.patio.listatrabalho.repositorio.EventoVmtWorkInstructionRepositorio;
import br.com.cloudport.servicoyard.patio.listatrabalho.repositorio.HistoricoWorkInstructionRepositorio;
import br.com.cloudport.servicoyard.patio.listatrabalho.repositorio.OrdemTrabalhoPatioRepositorio;
import br.com.cloudport.servicoyard.patio.modelo.TipoEventoVmt;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class EventoVmtWorkInstructionServicoTest {

    @Mock
    private EventoVmtWorkInstructionRepositorio eventoRepositorio;
    @Mock
    private OrdemTrabalhoPatioRepositorio ordemRepositorio;
    @Mock
    private HistoricoWorkInstructionRepositorio historicoRepositorio;
    @Mock
    private ConfirmacaoTransferenciaFisicaServico confirmacaoTransferenciaFisicaServico;

    private EventoVmtWorkInstructionServico servico;

    @BeforeEach
    void configurar() {
        servico = new EventoVmtWorkInstructionServico(
                eventoRepositorio,
                ordemRepositorio,
                historicoRepositorio,
                confirmacaoTransferenciaFisicaServico);
    }

    @Test
    void deveAceitarInstrucaoUmaUnicaVez() {
        OrdemTrabalhoPatio ordem = ordemEmExecucao(StatusConfirmacaoVmt.PENDENTE);
        LocalDateTime timestamp = LocalDateTime.of(2026, 7, 18, 10, 0);
        prepararConsulta(ordem);
        prepararPersistencia();

        servico.processar(10L, request("vmt-aceite-1", TipoEventoVmt.ACEITE,
                StatusConfirmacaoVmt.PENDENTE, timestamp));

        assertEquals(StatusConfirmacaoVmt.ACEITA, ordem.getStatusConfirmacaoVmt());
        assertEquals(timestamp, ordem.getVmtAceitoEm());
        assertEquals("vmt-aceite-1", ordem.getUltimoEventoVmtId());
        verify(eventoRepositorio).saveAndFlush(any(EventoVmtWorkInstruction.class));
        verify(historicoRepositorio).save(any(HistoricoOperacaoPatio.class));
        verify(confirmacaoTransferenciaFisicaServico, never()).confirmar(any(), any());
    }

    @Test
    void deveRejeitarEventIdDuplicadoAntesDeAlterarInstrucao() {
        EventoVmtWorkInstruction existente = new EventoVmtWorkInstruction();
        existente.setEventId("vmt-duplicado");
        when(eventoRepositorio.findByEventId("vmt-duplicado")).thenReturn(Optional.of(existente));

        ResponseStatusException excecao = assertThrows(ResponseStatusException.class,
                () -> servico.processar(10L, request("vmt-duplicado", TipoEventoVmt.ACEITE,
                        StatusConfirmacaoVmt.PENDENTE, LocalDateTime.now())));

        assertEquals(HttpStatus.CONFLICT, excecao.getStatus());
        verify(ordemRepositorio, never()).saveAndFlush(any(OrdemTrabalhoPatio.class));
        verify(confirmacaoTransferenciaFisicaServico, never()).confirmar(any(), any());
    }

    @Test
    void deveRejeitarInicioSemAceite() {
        OrdemTrabalhoPatio ordem = ordemEmExecucao(StatusConfirmacaoVmt.PENDENTE);
        prepararConsulta(ordem);

        ResponseStatusException excecao = assertThrows(ResponseStatusException.class,
                () -> servico.processar(10L, request("vmt-inicio-fora-sequencia", TipoEventoVmt.INICIO,
                        StatusConfirmacaoVmt.PENDENTE, LocalDateTime.now())));

        assertEquals(HttpStatus.CONFLICT, excecao.getStatus());
        verify(ordemRepositorio, never()).saveAndFlush(any(OrdemTrabalhoPatio.class));
        verify(eventoRepositorio, never()).saveAndFlush(any(EventoVmtWorkInstruction.class));
        verify(confirmacaoTransferenciaFisicaServico, never()).confirmar(any(), any());
    }

    @Test
    void deveConcluirInstrucaoSomenteAposConfirmacaoFisicaOrdenada() {
        OrdemTrabalhoPatio ordem = ordemEmExecucao(StatusConfirmacaoVmt.EM_EXECUCAO);
        LocalDateTime timestamp = LocalDateTime.of(2026, 7, 18, 10, 15);
        EventoVmtWorkInstructionRequest request = request("vmt-conclusao-1", TipoEventoVmt.CONCLUSAO,
                StatusConfirmacaoVmt.EM_EXECUCAO, timestamp);
        prepararConsulta(ordem);
        prepararPersistencia();

        servico.processar(10L, request);

        verify(confirmacaoTransferenciaFisicaServico).confirmar(ordem, request);
        assertEquals(StatusConfirmacaoVmt.CONCLUIDA, ordem.getStatusConfirmacaoVmt());
        assertEquals(StatusOrdemTrabalhoPatio.CONCLUIDA, ordem.getStatusOrdem());
        assertEquals(timestamp, ordem.getVmtConcluidoEm());
        assertEquals(timestamp, ordem.getConcluidoEm());
    }

    private void prepararConsulta(OrdemTrabalhoPatio ordem) {
        when(eventoRepositorio.findByEventId(any(String.class))).thenReturn(Optional.empty());
        when(ordemRepositorio.findOneById(10L)).thenReturn(Optional.of(ordem));
        when(eventoRepositorio
                .findFirstByOrdemTrabalhoPatioIdOrderByOcorridoEmDescProcessadoEmDesc(10L))
                .thenReturn(Optional.empty());
    }

    private void prepararPersistencia() {
        when(ordemRepositorio.saveAndFlush(any(OrdemTrabalhoPatio.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(eventoRepositorio.saveAndFlush(any(EventoVmtWorkInstruction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    private OrdemTrabalhoPatio ordemEmExecucao(StatusConfirmacaoVmt statusVmt) {
        OrdemTrabalhoPatio ordem = new OrdemTrabalhoPatio();
        ordem.setId(10L);
        ordem.setWorkQueueId(20L);
        ordem.setCodigoConteiner("CONT-10");
        ordem.setStatusOrdem(StatusOrdemTrabalhoPatio.EM_EXECUCAO);
        ordem.setStatusConfirmacaoVmt(statusVmt);
        ordem.setCriadoEm(LocalDateTime.now());
        ordem.setAtualizadoEm(LocalDateTime.now());
        return ordem;
    }

    private EventoVmtWorkInstructionRequest request(String eventId,
                                                     TipoEventoVmt tipoEvento,
                                                     StatusConfirmacaoVmt estadoEsperado,
                                                     LocalDateTime timestamp) {
        EventoVmtWorkInstructionRequest request = new EventoVmtWorkInstructionRequest();
        request.setEventId(eventId);
        request.setTipoEvento(tipoEvento);
        request.setStatusEsperado(estadoEsperado);
        request.setTimestamp(timestamp);
        request.setResultado("OK");
        request.setOperador("vmt-teste");
        request.setCorrelationId("corr-vmt");
        return request;
    }
}
