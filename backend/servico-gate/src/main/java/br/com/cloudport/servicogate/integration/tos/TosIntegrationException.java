package br.com.cloudport.servicogate.integration.tos;

import br.com.cloudport.servicogate.exception.BusinessException;

public class TosIntegrationException extends BusinessException {

    public TosIntegrationException(String message) {
        super(message);
    }

    public TosIntegrationException(String message, Throwable cause) {
        super(message, cause);
    }
}
