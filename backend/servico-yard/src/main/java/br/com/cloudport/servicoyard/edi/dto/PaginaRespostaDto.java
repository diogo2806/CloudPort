package br.com.cloudport.servicoyard.edi.dto;

import java.util.List;
import org.springframework.data.domain.Page;

public record PaginaRespostaDto<T>(
        List<T> conteudo,
        int pagina,
        int tamanho,
        long totalElementos,
        int totalPaginas,
        boolean primeira,
        boolean ultima
) {

    public static <T> PaginaRespostaDto<T> de(Page<T> pagina) {
        return new PaginaRespostaDto<>(
                pagina.getContent(),
                pagina.getNumber(),
                pagina.getSize(),
                pagina.getTotalElements(),
                pagina.getTotalPages(),
                pagina.isFirst(),
                pagina.isLast()
        );
    }
}
