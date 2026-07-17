package br.com.cloudport.servicogate.integration.alerta;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import br.com.cloudport.servicogate.app.gestor.ReconciliacaoBarcodeRepository;
import br.com.cloudport.servicogate.model.GatePass;
import br.com.cloudport.servicogate.model.ReconciliacaoBarcode;
import br.com.cloudport.servicogate.model.enums.StatusEntregaAlerta;
import br.com.cloudport.servicogate.model.enums.TipoDesincroniaBarcode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;

@ExtendWith(MockitoExtension.class)
class EntregaAlertaReconciliacaoServiceTest {

    @Mock
    private ReconciliacaoBarcodeRepository reconciliacaoRepository;

    @Mock
    private AlertaOperacionalGateway alertaOperacionalGateway;

    @Mock
    private PlatformTransactionManager transactionManager;

    private EntregaAlertaReconciliacaoService service;

    @BeforeEach
    void setUp() {
        TransactionStatus transactionStatus = mock(TransactionStatus.class);
        when(transactionManager.getTransaction(any(TransactionDefinition.class))).thenReturn(transactionStatus);
        service = new EntregaAlertaReconciliacaoService(
                reconciliacaoRepository,
                alertaOperacionalGateway,
                transactionManager,
                Duration.ofMinutes(2));
    }

    @Test
    void deveImpedirSegundaReivindicacaoEnquantoLeaseEstiverAtivo() {
        ReconciliacaoBarcode reconciliacao = novaReconciliacao();
        when(reconciliacaoRepository.findOneById(10L)).thenReturn(Optional.of(reconciliacao));
        when(reconciliacaoRepository.saveAndFlush(reconciliacao)).thenReturn(reconciliacao);

        Optional<EntregaAlertaReconciliacaoService.ReivindicacaoAlerta> primeira =
                service.reivindicarAlertaPendente(10L);
        Optional<EntregaAlertaReconciliacaoService.ReivindicacaoAlerta> segunda =
                service.reivindicarAlertaPendente(10L);

        assertThat(primeira).isPresent();
        assertThat(segunda).isEmpty();
        assertThat(reconciliacao.getStatusEntregaAlerta()).isEqualTo(StatusEntregaAlerta.PROCESSANDO);
        assertThat(reconciliacao.getAlertaReivindicacaoToken()).isNotBlank();
        assertThat(reconciliacao.getAlertaLeaseAte()).isAfter(reconciliacao.getAlertaReivindicadoEm());
        assertThat(primeira.get().getAlerta().getChaveIdempotencia())
                .isEqualTo("reconciliacao-barcode-10-webhook");
    }

    @Test
    void deveEnviarEConfirmarSomenteAlertaReivindicado() {
        ReconciliacaoBarcode reconciliacao = novaReconciliacao();
        LocalDateTime confirmadoEm = LocalDateTime.of(2026, 7, 17, 14, 30);
        when(reconciliacaoRepository
                .findByAlertaEnviadoFalseAndResolvidoEmIsNullOrderByDetectadoEmAsc())
                .thenReturn(Collections.singletonList(reconciliacao));
        when(reconciliacaoRepository.findOneById(10L)).thenReturn(Optional.of(reconciliacao));
        when(reconciliacaoRepository.saveAndFlush(reconciliacao)).thenReturn(reconciliacao);
        when(alertaOperacionalGateway.enviar(any(AlertaReconciliacaoBarcode.class)))
                .thenReturn(new ConfirmacaoEntregaAlerta("WEBHOOK", "delivery-10", confirmadoEm));

        int enviados = service.enviarAlertasPendentes();

        assertThat(enviados).isEqualTo(1);
        assertThat(reconciliacao.isAlertaEnviado()).isTrue();
        assertThat(reconciliacao.getStatusEntregaAlerta()).isEqualTo(StatusEntregaAlerta.ENVIADO);
        assertThat(reconciliacao.getAlertaTentativas()).isEqualTo(1);
        assertThat(reconciliacao.getAlertaEnviadoEm()).isEqualTo(confirmadoEm);
        assertThat(reconciliacao.getAlertaIdentificadorExterno()).isEqualTo("delivery-10");
        assertThat(reconciliacao.getAlertaReivindicacaoToken()).isNull();
        assertThat(reconciliacao.getAlertaLeaseAte()).isNull();
    }

    @Test
    void deveIgnorarConfirmacaoDeLeaseSubstituido() {
        ReconciliacaoBarcode reconciliacao = novaReconciliacao();
        when(reconciliacaoRepository.findOneById(10L)).thenReturn(Optional.of(reconciliacao));
        when(reconciliacaoRepository.saveAndFlush(reconciliacao)).thenReturn(reconciliacao);
        EntregaAlertaReconciliacaoService.ReivindicacaoAlerta primeira =
                service.reivindicarAlertaPendente(10L).orElseThrow(AssertionError::new);
        String chaveIdempotencia = primeira.getAlerta().getChaveIdempotencia();
        reconciliacao.setAlertaLeaseAte(LocalDateTime.now().minusMinutes(1));
        EntregaAlertaReconciliacaoService.ReivindicacaoAlerta segunda =
                service.reivindicarAlertaPendente(10L).orElseThrow(AssertionError::new);
        ConfirmacaoEntregaAlerta confirmacao = new ConfirmacaoEntregaAlerta(
                "WEBHOOK",
                "delivery-10",
                LocalDateTime.now());

        boolean primeiraConfirmada = service.confirmarEntrega(primeira, confirmacao);
        boolean segundaConfirmada = service.confirmarEntrega(segunda, confirmacao);

        assertThat(primeiraConfirmada).isFalse();
        assertThat(segundaConfirmada).isTrue();
        assertThat(segunda.getAlerta().getChaveIdempotencia()).isEqualTo(chaveIdempotencia);
        assertThat(reconciliacao.getStatusEntregaAlerta()).isEqualTo(StatusEntregaAlerta.ENVIADO);
    }

    @Test
    void deveRegistrarFalhaELiberarLeaseParaRetry() {
        ReconciliacaoBarcode reconciliacao = novaReconciliacao();
        when(reconciliacaoRepository
                .findByAlertaEnviadoFalseAndResolvidoEmIsNullOrderByDetectadoEmAsc())
                .thenReturn(Collections.singletonList(reconciliacao));
        when(reconciliacaoRepository.findOneById(10L)).thenReturn(Optional.of(reconciliacao));
        when(reconciliacaoRepository.saveAndFlush(reconciliacao)).thenReturn(reconciliacao);
        when(alertaOperacionalGateway.enviar(any(AlertaReconciliacaoBarcode.class)))
                .thenThrow(new IllegalStateException("webhook indisponível"));

        assertThatThrownBy(service::enviarAlertasPendentes)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("1 alerta(s)");
        assertThat(reconciliacao.isAlertaEnviado()).isFalse();
        assertThat(reconciliacao.getStatusEntregaAlerta()).isEqualTo(StatusEntregaAlerta.FALHA);
        assertThat(reconciliacao.getAlertaUltimoErro()).isEqualTo("webhook indisponível");
        assertThat(reconciliacao.getAlertaReivindicacaoToken()).isNull();
        assertThat(reconciliacao.getAlertaLeaseAte()).isNull();
    }

    private ReconciliacaoBarcode novaReconciliacao() {
        GatePass gatePass = new GatePass();
        gatePass.setId(20L);
        gatePass.setCodigo("GP-20");

        ReconciliacaoBarcode reconciliacao = new ReconciliacaoBarcode();
        reconciliacao.setId(10L);
        reconciliacao.setGatePass(gatePass);
        reconciliacao.setTipoDesinconia(TipoDesincroniaBarcode.BARCODE_MISMATCH);
        reconciliacao.setDescricao("Barcode divergente");
        reconciliacao.setDetectadoEm(LocalDateTime.of(2026, 7, 17, 2, 0));
        reconciliacao.setAlertaEnviado(false);
        reconciliacao.setStatusEntregaAlerta(StatusEntregaAlerta.PENDENTE);
        reconciliacao.setAlertaTentativas(0);
        return reconciliacao;
    }
}
