package br.com.cloudport.servicogate.integration.dmt;

import br.com.cloudport.servicogate.config.BarcodeProperties;
import br.com.cloudport.servicogate.model.GatePass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class DmtBarcodeService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DmtBarcodeService.class);
    private static final String MENSAGEM_INTEGRACAO_AUSENTE =
            "A validação de barcode não pode ser habilitada sem um cliente DMT real configurado.";

    private final BarcodeProperties barcodeProperties;

    public DmtBarcodeService(BarcodeProperties barcodeProperties) {
        this.barcodeProperties = barcodeProperties;
        if (barcodeProperties.isHabilitado()) {
            throw new IllegalStateException(MENSAGEM_INTEGRACAO_AUSENTE);
        }
    }

    public void solicitarConfirmacaoBarcode(GatePass gatePass, String containerNumber) {
        if (!barcodeProperties.isHabilitado()) {
            LOGGER.debug("Confirmação de barcode desabilitada para gatePass {}", gatePass.getId());
            return;
        }
        throw new IllegalStateException(MENSAGEM_INTEGRACAO_AUSENTE);
    }
}
