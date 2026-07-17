package br.com.cloudport.servicogate.app.gestor.dto;

import br.com.cloudport.servicogate.model.enums.TipoPessoaAcesso;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.Map;

@Schema(description = "Resumo das pessoas atualmente presentes no terminal")
public record ResumoAcessoPessoasDTO(
        long totalPresentes,
        Map<TipoPessoaAcesso, Long> presentesPorTipo,
        LocalDateTime atualizadoEm) {
}
