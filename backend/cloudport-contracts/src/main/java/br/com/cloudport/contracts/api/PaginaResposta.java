package br.com.cloudport.contracts.api;

import java.util.List;

/**
 * Contrato estável de paginação para APIs internas e externas.
 */
public record PaginaResposta<T>(
        List<T> conteudo,
        int pagina,
        int tamanho,
        long totalElementos,
        int totalPaginas,
        boolean primeira,
        boolean ultima
) {

    public PaginaResposta {
        conteudo = conteudo == null ? List.of() : List.copyOf(conteudo);
        if (pagina < 0) {
            throw new IllegalArgumentException("A pagina nao pode ser negativa.");
        }
        if (tamanho < 1) {
            throw new IllegalArgumentException("O tamanho da pagina deve ser maior que zero.");
        }
    }
}
