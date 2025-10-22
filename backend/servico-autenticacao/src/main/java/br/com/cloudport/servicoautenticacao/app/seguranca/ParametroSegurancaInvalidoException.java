package br.com.cloudport.servicoautenticacao.app.seguranca;

public class ParametroSegurancaInvalidoException extends RuntimeException {

    public ParametroSegurancaInvalidoException(String mensagem) {
        super(mensagem);
    }
}
