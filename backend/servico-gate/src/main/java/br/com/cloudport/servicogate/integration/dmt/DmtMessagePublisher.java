package br.com.cloudport.servicogate.integration.dmt;

import br.com.cloudport.servicogate.integration.dmt.DmtBarcodeService.BarcodeConfirmacaoRequest;

public interface DmtMessagePublisher {

    void enviarSolicitacaoBarcode(BarcodeConfirmacaoRequest request);
}
