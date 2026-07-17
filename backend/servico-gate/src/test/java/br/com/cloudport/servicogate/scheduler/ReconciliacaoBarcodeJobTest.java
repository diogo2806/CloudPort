package br.com.cloudport.servicogate.scheduler;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.cloudport.servicogate.app.gestor.ReconciliacaoBarcodeService;
import br.com.cloudport.servicogate.integration.alerta.EntregaAlertaReconciliacaoService;
import java.util.Collections;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ReconciliacaoBarcodeJobTest {

    @Test
    void naoDeveExecutarQuandoOutraInstanciaReivindicouOCiclo() {
        ReconciliacaoBarcodeService reconciliacaoService = mock(ReconciliacaoBarcodeService.class);
        EntregaAlertaReconciliacaoService entregaAlertaService =
                mock(EntregaAlertaReconciliacaoService.class);
        ReconciliacaoBarcodeCicloCoordenador coordenador =
                mock(ReconciliacaoBarcodeCicloCoordenador.class);
        when(coordenador.reivindicar()).thenReturn(Optional.empty());
        ReconciliacaoBarcodeJob job = new ReconciliacaoBarcodeJob(
                reconciliacaoService,
                entregaAlertaService,
                coordenador);

        job.executar();

        verify(reconciliacaoService, never()).executarReconciliacao();
        verify(entregaAlertaService, never()).enviarAlertasPendentes();
    }

    @Test
    void deveExecutarReconciliacaoEAlertasSobAMesmaReivindicacao() {
        ReconciliacaoBarcodeService reconciliacaoService = mock(ReconciliacaoBarcodeService.class);
        EntregaAlertaReconciliacaoService entregaAlertaService =
                mock(EntregaAlertaReconciliacaoService.class);
        ReconciliacaoBarcodeCicloCoordenador coordenador =
                mock(ReconciliacaoBarcodeCicloCoordenador.class);
        ReconciliacaoBarcodeCicloCoordenador.ReivindicacaoCiclo reivindicacao =
                mock(ReconciliacaoBarcodeCicloCoordenador.ReivindicacaoCiclo.class);
        when(coordenador.reivindicar()).thenReturn(Optional.of(reivindicacao));
        when(reconciliacaoService.executarReconciliacao()).thenReturn(Collections.emptyList());
        when(entregaAlertaService.enviarAlertasPendentes()).thenReturn(2);
        ReconciliacaoBarcodeJob job = new ReconciliacaoBarcodeJob(
                reconciliacaoService,
                entregaAlertaService,
                coordenador);

        job.executar();

        verify(reconciliacaoService).executarReconciliacao();
        verify(entregaAlertaService).enviarAlertasPendentes();
        verify(reivindicacao).close();
    }

    @Test
    void deveLiberarReivindicacaoEPropagarFalhaDoCiclo() {
        ReconciliacaoBarcodeService reconciliacaoService = mock(ReconciliacaoBarcodeService.class);
        EntregaAlertaReconciliacaoService entregaAlertaService =
                mock(EntregaAlertaReconciliacaoService.class);
        ReconciliacaoBarcodeCicloCoordenador coordenador =
                mock(ReconciliacaoBarcodeCicloCoordenador.class);
        ReconciliacaoBarcodeCicloCoordenador.ReivindicacaoCiclo reivindicacao =
                mock(ReconciliacaoBarcodeCicloCoordenador.ReivindicacaoCiclo.class);
        when(coordenador.reivindicar()).thenReturn(Optional.of(reivindicacao));
        when(reconciliacaoService.executarReconciliacao()).thenReturn(Collections.emptyList());
        when(entregaAlertaService.enviarAlertasPendentes())
                .thenThrow(new IllegalStateException("webhook indisponível"));
        ReconciliacaoBarcodeJob job = new ReconciliacaoBarcodeJob(
                reconciliacaoService,
                entregaAlertaService,
                coordenador);

        assertThatThrownBy(job::executar)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("webhook indisponível");
        verify(reivindicacao).close();
    }
}
