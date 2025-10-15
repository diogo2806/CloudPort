package br.com.cloudport.servicogate.app.gestor.dto;

import java.util.List;

public record GateOperadorFilaDTO(String id,
                                  String nome,
                                  Integer quantidade,
                                  Long tempoMedioEsperaMinutos,
                                  List<GateOperadorVeiculoDTO> veiculos) {
}
