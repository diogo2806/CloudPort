package br.com.cloudport.servicogate.app.gestor.dto;

import br.com.cloudport.servicogate.model.enums.TipoPessoaAcesso;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "Pessoa que possui entrada aberta no terminal")
public record PessoaPresenteDTO(
        Long id,
        String nome,
        String documento,
        TipoPessoaAcesso tipoPessoa,
        String empresa,
        String cracha,
        LocalDateTime entradaEm,
        String pontoEntrada,
        long permanenciaMinutos) {
}
