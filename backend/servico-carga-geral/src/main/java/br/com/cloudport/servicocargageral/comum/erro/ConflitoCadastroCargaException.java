package br.com.cloudport.servicocargageral.comum.erro;

public class ConflitoCadastroCargaException extends RuntimeException {

    private final String codigo;

    public ConflitoCadastroCargaException(String codigo, String mensagem, Throwable causa) {
        super(mensagem, causa);
        this.codigo = codigo;
    }

    public String getCodigo() {
        return codigo;
    }
}
