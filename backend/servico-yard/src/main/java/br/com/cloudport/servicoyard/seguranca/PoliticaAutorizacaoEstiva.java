package br.com.cloudport.servicoyard.seguranca;

public final class PoliticaAutorizacaoEstiva {

    public static final String LEITURA = "hasAnyRole('ADMIN_PORTO','PLANEJADOR')";
    public static final String COMANDO = "hasAnyRole('ADMIN_PORTO','PLANEJADOR')";

    private PoliticaAutorizacaoEstiva() {
    }
}
