package br.com.cloudport.servicoyard.dispatch.modelo;

import br.com.cloudport.servicoyard.patio.modelo.EquipamentoPatio;
import java.time.LocalDateTime;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "instrucao_movimentacao")
public class InstrucaoMovimentacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Setter(AccessLevel.NONE)
    private Long id;

    @Column(name = "codigo_conteiner", nullable = false, length = 30)
    private String codigoConteiner;

    @Column(name = "iso_tipo", length = 10)
    private String isoTipo;

    @Column(name = "comprimento_pes")
    private Integer comprimentoPes;

    @Column(name = "line_operator", length = 60)
    private String lineOperator;

    @Column(name = "porto_origem", length = 10)
    private String portoOrigem;

    @Column(name = "porto_destino", length = 10)
    private String portoDestino;

    @Column(name = "peso_kg")
    private Integer pesoKg;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_move", nullable = false, length = 30)
    private TipoMoveVmt tipoMove;

    @Column(name = "posicao_origem", length = 60)
    private String posicaoOrigem;

    @Column(name = "posicao_destino", length = 60)
    private String posicaoDestino;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "equipamento_id")
    private EquipamentoPatio equipamento;

    @Column(name = "fila_trabalho", length = 40)
    private String filaTrabalho;

    @Column(name = "sequencia", nullable = false)
    private Integer sequencia = 0;

    @Column(name = "prioridade_fetch", nullable = false)
    private boolean prioridadeFetch;

    @Column(name = "move_twin", nullable = false)
    private boolean moveTwin;

    @Column(name = "requer_energia", nullable = false)
    private boolean requerEnergia;

    @Column(name = "perigoso", nullable = false)
    private boolean perigoso;

    @Column(name = "fora_de_bitola", nullable = false)
    private boolean foraDeBitola;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private StatusInstrucaoMovimentacao status;

    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm;

    @Column(name = "atualizado_em", nullable = false)
    private LocalDateTime atualizadoEm;

    @Column(name = "concluido_em")
    private LocalDateTime concluidoEm;
}
