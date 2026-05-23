package br.com.cloudport.servicoyard.dispatch.dto;

import br.com.cloudport.servicoyard.dispatch.modelo.TipoMoveVmt;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CadastroInstrucaoMovimentacaoDTO {

    @NotBlank(message = "Informe o código do contêiner.")
    @Size(max = 30, message = "O código do contêiner deve ter no máximo 30 caracteres.")
    private String codigoConteiner;

    @NotNull(message = "Informe o tipo da movimentação.")
    private TipoMoveVmt tipoMove;

    @Size(max = 60, message = "A posição de origem deve ter no máximo 60 caracteres.")
    private String posicaoOrigem;

    @Size(max = 60, message = "A posição de destino deve ter no máximo 60 caracteres.")
    private String posicaoDestino;

    @Size(max = 10, message = "O tipo ISO deve ter no máximo 10 caracteres.")
    private String isoTipo;

    private Integer comprimentoPes;

    @Size(max = 60, message = "O operador (line operator) deve ter no máximo 60 caracteres.")
    private String lineOperator;

    @Size(max = 10, message = "O porto de origem deve ter no máximo 10 caracteres.")
    private String portoOrigem;

    @Size(max = 10, message = "O porto de destino deve ter no máximo 10 caracteres.")
    private String portoDestino;

    private Integer pesoKg;

    @Size(max = 40, message = "A fila de trabalho deve ter no máximo 40 caracteres.")
    private String filaTrabalho;

    private Integer sequencia;

    private boolean prioridadeFetch;

    private boolean moveTwin;

    private boolean requerEnergia;

    private boolean perigoso;

    private boolean foraDeBitola;
}
