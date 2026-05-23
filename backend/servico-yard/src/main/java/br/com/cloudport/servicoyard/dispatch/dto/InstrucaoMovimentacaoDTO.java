package br.com.cloudport.servicoyard.dispatch.dto;

import br.com.cloudport.servicoyard.dispatch.modelo.StatusInstrucaoMovimentacao;
import br.com.cloudport.servicoyard.dispatch.modelo.TipoMoveVmt;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class InstrucaoMovimentacaoDTO {
    private final Long id;
    private final String codigoConteiner;
    private final String isoTipo;
    private final Integer comprimentoPes;
    private final String lineOperator;
    private final String portoOrigem;
    private final String portoDestino;
    private final Integer pesoKg;
    private final TipoMoveVmt tipoMove;
    private final String posicaoOrigem;
    private final String posicaoDestino;
    private final Long equipamentoId;
    private final String equipamentoIdentificador;
    private final String filaTrabalho;
    private final Integer sequencia;
    private final boolean prioridadeFetch;
    private final boolean moveTwin;
    private final boolean requerEnergia;
    private final boolean perigoso;
    private final boolean foraDeBitola;
    private final StatusInstrucaoMovimentacao status;
    private final LocalDateTime criadoEm;
    private final LocalDateTime atualizadoEm;
    private final LocalDateTime concluidoEm;
}
