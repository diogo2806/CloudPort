package br.com.cloudport.servicogate.integration.alerta;

public interface AlertaOperacionalGateway {

    ConfirmacaoEntregaAlerta enviar(AlertaReconciliacaoBarcode alerta);
}
