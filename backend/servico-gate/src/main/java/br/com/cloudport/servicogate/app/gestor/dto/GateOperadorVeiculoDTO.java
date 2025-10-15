package br.com.cloudport.servicogate.app.gestor.dto;

import java.util.List;

public record GateOperadorVeiculoDTO(Long id,
                                     String placa,
                                     String documento,
                                     String motorista,
                                     String status,
                                     String statusDescricao,
                                     Long tempoFilaMinutos,
                                     String canalEntrada,
                                     String transportadora,
                                     List<GateOperadorContatoDTO> contatos,
                                     List<GateOperadorExcecaoDTO> excecoes,
                                     boolean podeImprimirComprovante) {
}
