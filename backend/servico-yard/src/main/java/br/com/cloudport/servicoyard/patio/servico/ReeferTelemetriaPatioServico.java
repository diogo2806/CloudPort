package br.com.cloudport.servicoyard.patio.servico;

import br.com.cloudport.servicoyard.container.entidade.TipoCargaConteiner;
import br.com.cloudport.servicoyard.patio.dto.ReeferTelemetriaPatioDto;
import br.com.cloudport.servicoyard.patio.dto.ReeferTelemetriaPatioRequisicaoDto;
import br.com.cloudport.servicoyard.patio.modelo.ConteinerPatio;
import br.com.cloudport.servicoyard.patio.modelo.PosicaoPatio;
import br.com.cloudport.servicoyard.patio.modelo.ReeferTelemetriaPatio;
import br.com.cloudport.servicoyard.patio.repositorio.ConteinerPatioRepositorio;
import br.com.cloudport.servicoyard.patio.repositorio.ReeferTelemetriaPatioRepositorio;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReeferTelemetriaPatioServico {

    private static final int MINUTOS_TELEMETRIA_DESATUALIZADA = 15;

    private final ReeferTelemetriaPatioRepositorio telemetriaRepositorio;
    private final ConteinerPatioRepositorio conteinerRepositorio;

    public ReeferTelemetriaPatioServico(ReeferTelemetriaPatioRepositorio telemetriaRepositorio,
                                         ConteinerPatioRepositorio conteinerRepositorio) {
        this.telemetriaRepositorio = telemetriaRepositorio;
        this.conteinerRepositorio = conteinerRepositorio;
    }

    @Transactional(readOnly = true)
    public List<ReeferTelemetriaPatioDto> listar() {
        LocalDateTime agora = LocalDateTime.now();
        return telemetriaRepositorio.findAllByOrderByRegistradoEmDesc().stream()
                .map(item -> converter(item, agora))
                .toList();
    }

    @Transactional
    public ReeferTelemetriaPatioDto registrar(Long conteinerId, ReeferTelemetriaPatioRequisicaoDto dto) {
        if (dto.getTemperaturaMinimaCelsius().compareTo(dto.getTemperaturaMaximaCelsius()) > 0) {
            throw new IllegalArgumentException("A temperatura mínima não pode ser maior que a máxima.");
        }
        ConteinerPatio conteiner = conteinerRepositorio.findById(conteinerId)
                .orElseThrow(() -> new IllegalArgumentException("Contêiner não encontrado."));
        if (!ehReefer(conteiner)) {
            throw new IllegalArgumentException("A telemetria de temperatura só pode ser registrada para contêiner reefer.");
        }

        ReeferTelemetriaPatio telemetria = telemetriaRepositorio.findByConteinerId(conteinerId)
                .orElseGet(ReeferTelemetriaPatio::new);
        telemetria.setConteiner(conteiner);
        telemetria.setTemperaturaAtualCelsius(dto.getTemperaturaAtualCelsius());
        telemetria.setTemperaturaMinimaCelsius(dto.getTemperaturaMinimaCelsius());
        telemetria.setTemperaturaMaximaCelsius(dto.getTemperaturaMaximaCelsius());
        telemetria.setLigado(dto.getLigado());
        telemetria.setRegistradoEm(LocalDateTime.now());
        return converter(telemetriaRepositorio.save(telemetria), LocalDateTime.now());
    }

    private ReeferTelemetriaPatioDto converter(ReeferTelemetriaPatio item, LocalDateTime agora) {
        ConteinerPatio conteiner = item.getConteiner();
        PosicaoPatio posicao = conteiner.getPosicao();
        String status = calcularStatus(item, agora);
        return new ReeferTelemetriaPatioDto(
                conteiner.getId(),
                conteiner.getCodigo(),
                posicao == null ? null : posicao.getBloco(),
                posicao == null ? null : posicao.getLinha(),
                posicao == null ? null : posicao.getColuna(),
                posicao == null ? null : posicao.getCamadaOperacional(),
                item.getTemperaturaAtualCelsius(),
                item.getTemperaturaMinimaCelsius(),
                item.getTemperaturaMaximaCelsius(),
                item.isLigado(),
                item.getRegistradoEm(),
                status,
                calcularMensagem(item, status));
    }

    private String calcularStatus(ReeferTelemetriaPatio item, LocalDateTime agora) {
        if (!item.isLigado()) {
            return "CRITICO";
        }
        if (item.getTemperaturaAtualCelsius().compareTo(item.getTemperaturaMinimaCelsius()) < 0
                || item.getTemperaturaAtualCelsius().compareTo(item.getTemperaturaMaximaCelsius()) > 0) {
            return "CRITICO";
        }
        if (item.getRegistradoEm().isBefore(agora.minusMinutes(MINUTOS_TELEMETRIA_DESATUALIZADA))) {
            return "ATENCAO";
        }
        return "NORMAL";
    }

    private String calcularMensagem(ReeferTelemetriaPatio item, String status) {
        if (!item.isLigado()) {
            return "Reefer desligado ou sem alimentação elétrica.";
        }
        if ("CRITICO".equals(status)) {
            return String.format(Locale.ROOT,
                    "Temperatura fora da faixa: %s °C, esperado entre %s °C e %s °C.",
                    item.getTemperaturaAtualCelsius(),
                    item.getTemperaturaMinimaCelsius(),
                    item.getTemperaturaMaximaCelsius());
        }
        if ("ATENCAO".equals(status)) {
            return "Telemetria sem atualização há mais de 15 minutos.";
        }
        return "Temperatura dentro da faixa operacional.";
    }

    private boolean ehReefer(ConteinerPatio conteiner) {
        if (conteiner.getTipoCarga() == TipoCargaConteiner.REFRIGERADO) {
            return true;
        }
        if (conteiner.getCarga() == null) {
            return false;
        }
        String descricao = String.valueOf(conteiner.getCarga().getDescricao()).toUpperCase(Locale.ROOT);
        return descricao.contains("REEFER") || descricao.contains("REFRIGERADO");
    }
}