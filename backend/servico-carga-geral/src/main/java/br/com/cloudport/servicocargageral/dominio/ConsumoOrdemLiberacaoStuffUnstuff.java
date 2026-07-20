package br.com.cloudport.servicocargageral.dominio;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.PrePersist;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

@Entity
@Table(name = "consumo_ordem_liberacao_stuff_unstuff", uniqueConstraints =
        @UniqueConstraint(name = "uk_consumo_ordem_command", columnNames = {"operacao_id", "command_id"}))
public class ConsumoOrdemLiberacaoStuffUnstuff {
    @Id @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;
    @Column(name = "operacao_id", nullable = false)
    private UUID operacaoId;
    @Column(name = "command_id", nullable = false)
    private UUID commandId;
    @Column(nullable = false, precision = 19, scale = 3)
    private BigDecimal quantidade;
    @Column(name = "criado_em", nullable = false)
    private OffsetDateTime criadoEm;
    @PrePersist void prePersist() { criadoEm = OffsetDateTime.now(); }
    public void setOperacaoId(UUID operacaoId) { this.operacaoId = operacaoId; }
    public void setCommandId(UUID commandId) { this.commandId = commandId; }
    public void setQuantidade(BigDecimal quantidade) { this.quantidade = quantidade; }
}
