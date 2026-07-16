package br.com.cloudport.contracts.api;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * Metadados obrigatórios para comandos destrutivos ou administrativos.
 */
public record ComandoMotivado(
        @NotBlank(message = "O motivo e obrigatorio.")
        @Size(max = 500, message = "O motivo deve ter no maximo 500 caracteres.")
        String motivo,
        @Size(max = 150, message = "O usuario deve ter no maximo 150 caracteres.")
        String usuario,
        @Size(max = 100, message = "A origem da acao deve ter no maximo 100 caracteres.")
        String origemAcao,
        @Size(max = 100, message = "O correlationId deve ter no maximo 100 caracteres.")
        String correlationId
) {

    public String motivoNormalizado() {
        return motivo == null ? null : motivo.trim();
    }

    public String usuarioEfetivo(String usuarioAutenticado) {
        if (usuario != null && !usuario.isBlank()) {
            return usuario.trim();
        }
        return usuarioAutenticado == null || usuarioAutenticado.isBlank() ? "sistema" : usuarioAutenticado.trim();
    }
}
