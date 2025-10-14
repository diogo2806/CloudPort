package br.com.cloudport.servicogate.dto.operador;

import java.util.List;

public record GateOperadorFilaDTO(String id,
                                  String nome,
                                  Integer quantidade,
                                  Long tempoMedioEsperaMinutos,
                                  List<GateOperadorVeiculoDTO> veiculos) {
}
