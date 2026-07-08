package br.com.cloudport.serviconaviosiderurgico.dto;

public record ComandoReplanejamentoPatioNavioDTO(
        Boolean aplicar,
        String usuario,
        Integer limiteRehandleAceitavel
) {
    public boolean aplicarEfetivo() {
        return Boolean.TRUE.equals(aplicar);
    }
}
