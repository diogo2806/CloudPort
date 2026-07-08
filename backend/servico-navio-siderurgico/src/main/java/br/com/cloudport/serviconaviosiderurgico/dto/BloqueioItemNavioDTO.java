package br.com.cloudport.serviconaviosiderurgico.dto;

public record BloqueioItemNavioDTO(
        boolean bloqueado,
        String motivo,
        String usuario
) {}
