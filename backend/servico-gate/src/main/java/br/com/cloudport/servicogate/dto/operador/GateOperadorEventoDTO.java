package br.com.cloudport.servicogate.dto.operador;

import java.time.LocalDateTime;

public record GateOperadorEventoDTO(Long id,
                                    String tipo,
                                    String descricao,
                                    String nivel,
                                    LocalDateTime registradoEm,
                                    Long veiculoId,
                                    String placaVeiculo,
                                    String transportadora,
                                    String usuario) {
}
