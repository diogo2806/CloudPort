package br.com.cloudport.serviconaviosiderurgico.dto;

import br.com.cloudport.serviconaviosiderurgico.dominio.OperacaoSiderurgica;
import br.com.cloudport.serviconaviosiderurgico.dominio.StatusOperacaoSiderurgica;
import br.com.cloudport.serviconaviosiderurgico.dominio.TipoOperacaoSiderurgica;
import java.time.LocalDateTime;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

public record OperacaoSiderurgicaDTO(
        Long id,
        @NotNull Long navioId,
        String navioNome,
        @NotNull TipoOperacaoSiderurgica tipoOperacao,
        StatusOperacaoSiderurgica status,
        @Size(max = 30) String berco,
        @Size(max = 40) String viagem,
        LocalDateTime eta,
        LocalDateTime inicioOperacao,
        LocalDateTime fimOperacao,
        @Size(max = 80) String origem,
        @Size(max = 80) String destino,
        @Size(max = 500) String observacoes
) {
    public static OperacaoSiderurgicaDTO de(OperacaoSiderurgica operacao) {
        return new OperacaoSiderurgicaDTO(
                operacao.getId(),
                operacao.getNavio().getId(),
                operacao.getNavio().getNome(),
                operacao.getTipoOperacao(),
                operacao.getStatus(),
                operacao.getBerco(),
                operacao.getViagem(),
                operacao.getEta(),
                operacao.getInicioOperacao(),
                operacao.getFimOperacao(),
                operacao.getOrigem(),
                operacao.getDestino(),
                operacao.getObservacoes()
        );
    }
}
