package br.com.cloudport.visibilidade.exception;

public class EventoEnvelopeInvalidoException extends RuntimeException {

    public EventoEnvelopeInvalidoException(String mensagem) {
        super(mensagem);
    }
}
