package br.com.cloudport.serviconaviosiderurgico.dto;

import java.util.List;

public record RelatorioOperacionalIntegradoDTO(
        VisitaNavioDTO visita,
        ResumoOperacionalNavioDTO resumoOperacional,
        ResumoIntegracaoNavioPatioDTO resumoIntegracao,
        PlanoEstivaNavioDTO planoEstiva,
        List<ItemOperacaoNavioDTO> itens,
        List<ReservaPatioNavioDTO> reservasPatio,
        List<OrdemPatioDaVisitaDTO> ordensPatio,
        List<AlertaIntegracaoNavioPatioDTO> divergenciasAlertas,
        List<EventoVisitaNavioDTO> eventosRelevantes
) {}
