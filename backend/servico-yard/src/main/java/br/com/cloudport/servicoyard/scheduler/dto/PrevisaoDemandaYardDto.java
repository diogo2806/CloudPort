package br.com.cloudport.servicoyard.scheduler.dto;

import java.time.LocalDateTime;
import java.util.List;

public record PrevisaoDemandaYardDto(
        String modelo,
        String versaoModelo,
        boolean modeloAtivo,
        boolean fallbackDeterministico,
        int horizonteHoras,
        int demandaPrevista,
        int duracaoPrevistaMinutos,
        double confianca,
        int baselineDeterministico,
        int diferencaBaseline,
        String explicacao,
        List<String> validacoesObrigatorias,
        LocalDateTime geradoEm) {
}
