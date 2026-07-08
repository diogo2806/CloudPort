package br.com.cloudport.serviconaviosiderurgico.dto;

public record AlertaIntegracaoNavioPatioDTO(
        String tipo,
        String severidade,
        Long visitaNavioId,
        Long itemOperacaoNavioId,
        Long ordemTrabalhoPatioId,
        String mensagem
) {}
