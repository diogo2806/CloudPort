package br.com.cloudport.servicoyard.patio.dto;

import br.com.cloudport.servicoyard.patio.modelo.TelemetriaEquipamentoPatio;
import java.time.LocalDateTime;

public record TelemetriaEquipamentoPatioDto(
        String equipamento,
        String tipoEquipamento,
        String statusOperacional,
        Double latitude,
        Double longitude,
        Double coordenadaX,
        Double coordenadaY,
        Double heading,
        String posicaoMaisProxima,
        Integer distanciaPosicaoCentimetros,
        Boolean dentroDaPosicao,
        String origem,
        String operadorVmt,
        String statusVmt,
        Long workInstructionAtualId,
        Long sequencia,
        LocalDateTime capturadoEm,
        LocalDateTime recebidoEm
) {
    public static TelemetriaEquipamentoPatioDto de(TelemetriaEquipamentoPatio telemetria) {
        return new TelemetriaEquipamentoPatioDto(
                telemetria.getEquipamento().getIdentificador(),
                telemetria.getEquipamento().getTipoEquipamento().name(),
                telemetria.getEquipamento().getStatusOperacional().name(),
                telemetria.getLatitude(),
                telemetria.getLongitude(),
                telemetria.getCoordenadaX(),
                telemetria.getCoordenadaY(),
                telemetria.getHeading(),
                telemetria.getPosicaoMaisProxima(),
                telemetria.getDistanciaPosicaoCentimetros(),
                telemetria.getDentroDaPosicao(),
                telemetria.getOrigem(),
                telemetria.getOperadorVmt(),
                telemetria.getStatusVmt(),
                telemetria.getWorkInstructionAtualId(),
                telemetria.getSequencia(),
                telemetria.getCapturadoEm(),
                telemetria.getRecebidoEm()
        );
    }
}
