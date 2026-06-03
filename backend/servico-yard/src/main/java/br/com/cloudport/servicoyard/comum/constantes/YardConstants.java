package br.com.cloudport.servicoyard.comum.constantes;

import java.math.BigDecimal;

public final class YardConstants {

    public static final int EMPILHAMENTO_MAXIMO = 4;
    public static final BigDecimal PESO_LIMITE_PILHA_INTERMEDIARIA = new BigDecimal("25");
    public static final BigDecimal PESO_LIMITE_PILHA_DUPLA = new BigDecimal("20");

    private YardConstants() {}
}
