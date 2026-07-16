package br.com.cloudport.visibilidade.exception;

public class ConflitoIdentidadeEventoException extends IllegalStateException {

    public ConflitoIdentidadeEventoException(String mensagem) {
        super(mensagem);
    }
}
