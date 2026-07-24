package br.com.cloudport.servicoyard.patio.seguranca;

import java.time.LocalDateTime;
import java.util.List;
import javax.validation.constraints.Future;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

public final class ZonaSegurancaPatioDtos {

    private ZonaSegurancaPatioDtos() {
    }

    public record Criar(
            @NotBlank String chaveIdempotencia,
            @NotBlank String nome,
            String geometria,
            @NotEmpty List<@NotBlank String> posicoes,
            @NotNull LocalDateTime inicio,
            @NotNull @Future LocalDateTime fim,
            @NotBlank String responsavel,
            @NotEmpty List<@NotBlank String> equipe,
            @NotBlank String motivo,
            Boolean bloqueiaOrigem,
            Boolean bloqueiaDestino,
            Boolean bloqueiaRota,
            String operador,
            String correlationId) {
    }

    public record Prorrogar(
            @NotNull @Future LocalDateTime novoFim,
            @NotBlank String motivo,
            @NotBlank String operador,
            String correlationId) {
    }

    public record Liberar(
            @NotBlank String motivo,
            @NotBlank String operador,
            String correlationId) {
    }

    public record Resposta(
            Long id,
            String chaveIdempotencia,
            String nome,
            String geometria,
            List<String> posicoes,
            LocalDateTime inicio,
            LocalDateTime fim,
            String responsavel,
            List<String> equipe,
            String motivo,
            String estado,
            boolean bloqueiaOrigem,
            boolean bloqueiaDestino,
            boolean bloqueiaRota,
            long versao,
            int conflitosAtivos,
            LocalDateTime ativadaEm,
            LocalDateTime liberadaEm,
            String liberadaPor,
            String motivoLiberacao) {
    }
}
