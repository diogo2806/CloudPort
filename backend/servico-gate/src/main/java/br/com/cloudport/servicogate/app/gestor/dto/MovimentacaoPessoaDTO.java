package br.com.cloudport.servicogate.app.gestor.dto;

import br.com.cloudport.servicogate.model.enums.DirecaoMovimentacaoPessoa;
import br.com.cloudport.servicogate.model.enums.TipoPessoaAcesso;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "Movimentação auditável de entrada ou saída de uma pessoa")
public record MovimentacaoPessoaDTO(
        Long id,
        Long pessoaId,
        String nome,
        String documento,
        TipoPessoaAcesso tipoPessoa,
        String empresa,
        String cracha,
        DirecaoMovimentacaoPessoa direcao,
        String pontoAcesso,
        String motivo,
        LocalDateTime registradoEm,
        String usuarioResponsavel,
        String origemAcao,
        String correlationId,
        Long permanenciaMinutos) {
}
