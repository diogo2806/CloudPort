package br.com.cloudport.servicocargageral.servico;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.cloudport.servicocargageral.dominio.ItemConhecimentoCarga;
import br.com.cloudport.servicocargageral.dominio.LoteCarga;
import br.com.cloudport.servicocargageral.dominio.OperacaoTransload;
import br.com.cloudport.servicocargageral.dto.TransloadDTOs.ExecutarTransloadRequest;
import br.com.cloudport.servicocargageral.dto.TransloadDTOs.ItemTransloadRequest;
import br.com.cloudport.servicocargageral.repositorio.LoteCargaRepositorio;
import br.com.cloudport.servicocargageral.repositorio.OperacaoTransloadRepositorio;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
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
class TransloadTransacaoServicoTest {

    @Mock
    private LoteCargaRepositorio loteRepositorio;

    @Mock
    private OperacaoTransloadRepositorio transloadRepositorio;

    @Mock
    private LoteCarga loteOrigem;

    @Mock
    private LoteCarga loteDestinoUm;

    @Mock
    private LoteCarga loteDestinoDois;

    @Mock
    private ItemConhecimentoCarga itemConhecimento;

    private TransloadTransacaoServico servico;

    @BeforeEach
    void configurar() {
        servico = new TransloadTransacaoServico(loteRepositorio, transloadRepositorio);
    }

    @Test
    void deveValidarSomatorioDosItensAntesDeAlterarQualquerSaldo() {
        UUID operacaoId = UUID.randomUUID();
        UUID loteOrigemId = UUID.randomUUID();
        UUID loteDestinoUmId = UUID.randomUUID();
        UUID loteDestinoDoisId = UUID.randomUUID();
        UUID itemId = UUID.randomUUID();
        OperacaoTransload operacao = novaOperacao(operacaoId);
        ExecutarTransloadRequest request = new ExecutarTransloadRequest(
                operacao.getCommandId(),
                operacao.getUnidadeOrigem(),
                operacao.getUnidadeDestino(),
                null,
                null,
                null,
                null,
                null,
                operacao.getUsuario(),
                operacao.getCorrelationId(),
                List.of(
                        new ItemTransloadRequest(
                                loteOrigemId,
                                loteDestinoUmId,
                                new BigDecimal("6.000"),
                                BigDecimal.ZERO,
                                BigDecimal.ZERO),
                        new ItemTransloadRequest(
                                loteOrigemId,
                                loteDestinoDoisId,
                                new BigDecimal("6.000"),
                                BigDecimal.ZERO,
                                BigDecimal.ZERO)));

        when(transloadRepositorio.findComBloqueioById(operacaoId)).thenReturn(Optional.of(operacao));
        when(loteRepositorio.findComBloqueioById(loteOrigemId)).thenReturn(Optional.of(loteOrigem));
        when(loteRepositorio.findComBloqueioById(loteDestinoUmId)).thenReturn(Optional.of(loteDestinoUm));
        when(loteRepositorio.findComBloqueioById(loteDestinoDoisId)).thenReturn(Optional.of(loteDestinoDois));
        configurarLote(loteOrigem, itemId, "LOTE-ORIGEM");
        configurarLote(loteDestinoUm, itemId, "LOTE-DESTINO-1");
        configurarLote(loteDestinoDois, itemId, "LOTE-DESTINO-2");
        when(loteOrigem.getQuantidadeDisponivel()).thenReturn(new BigDecimal("10.000"));
        when(loteOrigem.getVolumeDisponivelM3()).thenReturn(BigDecimal.ZERO);
        when(loteOrigem.getPesoDisponivelKg()).thenReturn(BigDecimal.ZERO);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> servico.aplicar(operacaoId, request));

        assertEquals(HttpStatus.CONFLICT, exception.getStatus());
        verify(loteOrigem, never()).retirarSaldo(
                new BigDecimal("6.000"),
                BigDecimal.ZERO,
                BigDecimal.ZERO);
        verify(loteRepositorio, never()).saveAll(anyCollection());
    }

    private OperacaoTransload novaOperacao(UUID operacaoId) {
        OperacaoTransload operacao = new OperacaoTransload();
        ReflectionTestUtils.setField(operacao, "id", operacaoId);
        operacao.setCommandId(UUID.randomUUID());
        operacao.setUnidadeOrigem("CONT-ORIGEM-001");
        operacao.setUnidadeDestino("CONT-DESTINO-001");
        operacao.setReservaOrigemId(UUID.randomUUID());
        operacao.setReservaDestinoId(UUID.randomUUID());
        operacao.setUsuario("operador.teste");
        operacao.setCorrelationId(operacao.getCommandId().toString());
        operacao.iniciar();
        return operacao;
    }

    private void configurarLote(LoteCarga lote, UUID itemId, String codigo) {
        when(lote.getUnidadeMedida()).thenReturn("UN");
        when(lote.getItem()).thenReturn(itemConhecimento);
        when(itemConhecimento.getId()).thenReturn(itemId);
        when(lote.getCodigo()).thenReturn(codigo);
        when(lote.getQuantidadeSaldo()).thenReturn(BigDecimal.ZERO);
        when(lote.getVolumeSaldoM3()).thenReturn(BigDecimal.ZERO);
        when(lote.getPesoSaldoKg()).thenReturn(BigDecimal.ZERO);
        when(lote.getQuantidadePrevista()).thenReturn(new BigDecimal("100.000"));
        when(lote.getVolumePrevistoM3()).thenReturn(BigDecimal.ZERO);
        when(lote.getPesoPrevistoKg()).thenReturn(BigDecimal.ZERO);
    }
}
