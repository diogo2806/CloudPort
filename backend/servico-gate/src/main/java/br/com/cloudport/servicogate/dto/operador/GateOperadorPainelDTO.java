package br.com.cloudport.servicogate.dto.operador;

import java.time.LocalDateTime;
import java.util.List;

public record GateOperadorPainelDTO(List<GateOperadorFilaDTO> filasEntrada,
                                    List<GateOperadorFilaDTO> filasSaida,
                                    List<GateOperadorVeiculoDTO> veiculosAtendimento,
                                    List<GateOperadorEventoDTO> historico,
                                    LocalDateTime ultimaAtualizacao) {
}
