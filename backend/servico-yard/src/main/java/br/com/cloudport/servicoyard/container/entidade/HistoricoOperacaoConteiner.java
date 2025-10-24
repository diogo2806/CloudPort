package br.com.cloudport.servicoyard.container.entidade;

import java.time.OffsetDateTime;
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
@Table(name = "historico_operacao_conteiner")
public class HistoricoOperacaoConteiner {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    @Setter(AccessLevel.NONE)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "conteiner_id", nullable = false)
    private Conteiner conteiner;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_operacao", nullable = false, length = 40)
    private TipoOperacaoConteiner tipoOperacao;

    @Column(name = "descricao", nullable = false, length = 255)
    private String descricao;

    @Column(name = "posicao_anterior", length = 60)
    private String posicaoAnterior;

    @Column(name = "posicao_atual", length = 60)
    private String posicaoAtual;

    @Column(name = "responsavel", length = 80)
    private String responsavel;

    @Column(name = "data_registro", nullable = false)
    private OffsetDateTime dataRegistro;
}
