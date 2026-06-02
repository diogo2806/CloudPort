package br.com.cloudport.servicogate.integration.dmt;

import br.com.cloudport.servicogate.config.BarcodeProperties;
import br.com.cloudport.servicogate.model.GatePass;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class DmtBarcodeService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DmtBarcodeService.class);

    private final BarcodeProperties barcodeProperties;
    private final ScheduledExecutorService executorService;
    private final DmtMessagePublisher messagePublisher;

    public DmtBarcodeService(BarcodeProperties barcodeProperties,
                            ScheduledExecutorService gateExecutorService,
                            DmtMessagePublisher messagePublisher) {
        this.barcodeProperties = barcodeProperties;
        this.executorService = gateExecutorService;
        this.messagePublisher = messagePublisher;
    }

    public void solicitarConfirmacaoBarcode(GatePass gatePass, String containerNumber) {
        if (!barcodeProperties.isHabilitado()) {
            LOGGER.debug("Confirmação de barcode desabilitada para gatePass {}", gatePass.getId());
            return;
        }

        BarcodeConfirmacaoRequest request = new BarcodeConfirmacaoRequest(
                gatePass.getToken(),
                containerNumber,
                gatePass.getAgendamento().getVeiculo().getPlaca()
        );

        LOGGER.info("event=dmt.barcode.solicitado gatePassId={} token={} container={} timeout={}",
                gatePass.getId(), gatePass.getToken(), containerNumber,
                barcodeProperties.getTimeoutConfirmacao().getSeconds());

        messagePublisher.enviarSolicitacaoBarcode(request);
        agendarTimeoutConfirmacao(gatePass);
    }

    private void agendarTimeoutConfirmacao(GatePass gatePass) {
        executorService.schedule(
                () -> verificarTimeoutConfirmacao(gatePass),
                barcodeProperties.getTimeoutConfirmacao().getSeconds(),
                TimeUnit.SECONDS
        );
    }

    private void verificarTimeoutConfirmacao(GatePass gatePass) {
        LOGGER.warn("event=dmt.barcode.timeout gatePassId={} token={}",
                gatePass.getId(), gatePass.getToken());
    }

    public static class BarcodeConfirmacaoRequest {
        private final String tokenGatePass;
        private final String containerNumber;
        private final String placa;

        public BarcodeConfirmacaoRequest(String tokenGatePass, String containerNumber, String placa) {
            this.tokenGatePass = tokenGatePass;
            this.containerNumber = containerNumber;
            this.placa = placa;
        }

        public String getTokenGatePass() {
            return tokenGatePass;
        }

        public String getContainerNumber() {
            return containerNumber;
        }

        public String getPlaca() {
            return placa;
        }
    }
}
