package br.com.cloudport.serviconaviosiderurgico.comum;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class IntegracaoYardIndisponivelException extends ResponseStatusException {

    private final String operacao;

    public IntegracaoYardIndisponivelException(String operacao, Throwable causa) {
        super(
                HttpStatus.SERVICE_UNAVAILABLE,
                "Yard obrigatorio indisponivel durante " + operacao + ".",
                causa
        );
        this.operacao = operacao;
    }

    public String getOperacao() {
        return operacao;
    }
}
