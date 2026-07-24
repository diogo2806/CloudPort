package br.com.cloudport.servicocargageral.servico;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.cloudport.servicocargageral.dominio.OperacaoTransload;
import br.com.cloudport.servicocargageral.dto.TransloadDTOs.ExecutarTransloadRequest;
import br.com.cloudport.servicocargageral.dto.TransloadDTOs.ItemTransloadRequest;
import br.com.cloudport.servicocargageral.dto.TransloadDTOs.TransloadResposta;
import br.com.cloudport.servicocargageral.integracao.inventario.InventarioConteinerCliente;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class TransloadServicoTest {

    @Mock
    private TransloadTransacaoServico transacaoServico;

    @Mock
    private InventarioConteinerCliente inventarioConteinerCliente;

    private TransloadServico servico;

    @BeforeEach
    void configurar() {
        servico = new TransloadServico(transacaoServico, inventarioConteinerCliente);
    }

    @Test
    void deveRejeitarReexecucaoComMesmoCommandIdEDadosDiferentes() {
        ExecutarTransloadRequest requestOriginal = requestPadrao();
        OperacaoTransload operacao = operacaoConcluida(requestOriginal);
        ExecutarTransloadRequest requestDivergente = new ExecutarTransloadRequest(
                requestOriginal.commandId(),
                requestOriginal.unidadeOrigem(),
                "CONT-DESTINO-999",
                requestOriginal.lacreOrigem(),
                requestOriginal.lacreDestino(),
                requestOriginal.divergencia(),
                requestOriginal.codigoAvaria(),
                requestOriginal.descricaoAvaria(),
                requestOriginal.usuario(),
                requestOriginal.correlationId(),
                requestOriginal.itens());
        when(transacaoServico.iniciar(requestDivergente)).thenReturn(operacao);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> servico.executarTransload(requestDivergente));

        assertEquals(HttpStatus.CONFLICT, exception.getStatus());
        verify(inventarioConteinerCliente, never()).liberar(
                eq(operacao.getReservaOrigemId()),
                anyString(),
                anyString(),
                anyString());
    }

    @Test
    void deveRetomarOperacaoConcluidaELiberarReservasDeFormaIdempotente() {
        ExecutarTransloadRequest request = requestPadrao();
        OperacaoTransload operacao = operacaoConcluida(request);
        when(transacaoServico.iniciar(request)).thenReturn(operacao);

        TransloadResposta resposta = servico.executarTransload(request);

        assertEquals("Lacre rompido", resposta.descricaoAvaria());
        assertEquals(operacao.getReservaOrigemId(), resposta.reservaOrigemId());
        assertEquals(operacao.getReservaDestinoId(), resposta.reservaDestinoId());
        verify(inventarioConteinerCliente).liberar(
                operacao.getReservaOrigemId(),
                request.usuario(),
                "Transload concluído",
                "CONCLUIDA");
        verify(inventarioConteinerCliente).liberar(
                operacao.getReservaDestinoId(),
                request.usuario(),
                "Transload concluído",
                "CONCLUIDA");
    }

    @Test
    void deveManterOperacaoRecuperavelQuandoReservaFalhar() {
        ExecutarTransloadRequest request = requestPadrao();
        OperacaoTransload operacao = operacaoEmExecucao(request);
        when(transacaoServico.iniciar(request)).thenReturn(operacao);
        when(inventarioConteinerCliente.reservar(
                operacao.getUnidadeOrigem(),
                operacao.getReservaOrigemId(),
                operacao.getUsuario()))
                .thenReturn(null);
        when(inventarioConteinerCliente.reservar(
                operacao.getUnidadeDestino(),
                operacao.getReservaDestinoId(),
                operacao.getUsuario()))
                .thenThrow(new IllegalStateException("Destino indisponível"));

        assertThrows(IllegalStateException.class, () -> servico.executarTransload(request));

        verify(transacaoServico, never()).aplicar(operacao.getId(), request);
        verify(transacaoServico, never()).cancelar(eq(operacao.getId()), anyString());
        verify(inventarioConteinerCliente, never()).liberar(
                eq(operacao.getReservaOrigemId()),
                anyString(),
                anyString(),
                anyString());
    }

    @Test
    void deveCompensarAsDuasReservasQuandoAplicacaoAtomicaFalhar() {
        ExecutarTransloadRequest request = requestPadrao();
        OperacaoTransload operacao = operacaoEmExecucao(request);
        when(transacaoServico.iniciar(request)).thenReturn(operacao);
        when(transacaoServico.aplicar(operacao.getId(), request))
                .thenThrow(new ResponseStatusException(HttpStatus.CONFLICT, "Saldo insuficiente"));

        assertThrows(ResponseStatusException.class, () -> servico.executarTransload(request));

        verify(inventarioConteinerCliente).liberar(
                operacao.getReservaOrigemId(),
                request.usuario(),
                "Transload cancelado antes da atualização atômica",
                "CANCELADA");
        verify(inventarioConteinerCliente).liberar(
                operacao.getReservaDestinoId(),
                request.usuario(),
                "Transload cancelado antes da atualização atômica",
                "CANCELADA");
        verify(transacaoServico).cancelar(eq(operacao.getId()), anyString());
    }

    private ExecutarTransloadRequest requestPadrao() {
        UUID commandId = UUID.randomUUID();
        return new ExecutarTransloadRequest(
                commandId,
                "CONT-ORIGEM-001",
                "CONT-DESTINO-001",
                "LACRE-ORIGEM",
                "LACRE-DESTINO",
                "Diferença de conferência registrada",
                "SEAL",
                "Lacre rompido",
                "operador.teste",
                commandId.toString(),
                List.of(new ItemTransloadRequest(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        new BigDecimal("10.000"),
                        new BigDecimal("2.500"),
                        new BigDecimal("1200.000"))));
    }

    private OperacaoTransload operacaoConcluida(ExecutarTransloadRequest request) {
        OperacaoTransload operacao = operacaoEmExecucao(request);
        operacao.concluir();
        return operacao;
    }

    private OperacaoTransload operacaoEmExecucao(ExecutarTransloadRequest request) {
        OperacaoTransload operacao = new OperacaoTransload();
        ReflectionTestUtils.setField(operacao, "id", UUID.randomUUID());
        operacao.setCommandId(request.commandId());
        operacao.setUnidadeOrigem(request.unidadeOrigem());
        operacao.setUnidadeDestino(request.unidadeDestino());
        operacao.setReservaOrigemId(UUID.randomUUID());
        operacao.setReservaDestinoId(UUID.randomUUID());
        operacao.setLacreOrigem(request.lacreOrigem());
        operacao.setLacreDestino(request.lacreDestino());
        operacao.setDivergencia(request.divergencia());
        operacao.setCodigoAvaria(request.codigoAvaria());
        operacao.setDescricaoAvaria(request.descricaoAvaria());
        operacao.setUsuario(request.usuario());
        operacao.setCorrelationId(request.correlationId());
        request.itens().forEach(item -> operacao.adicionarItem(
                item.loteOrigemId(),
                item.loteDestinoId(),
                item.quantidade(),
                item.volumeM3(),
                item.pesoKg()));
        operacao.iniciar();
        return operacao;
    }
}
