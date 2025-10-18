package br.com.cloudport.servicoyard.patio.modelo;

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

@Entity
@Table(name = "movimento_patio")
public class MovimentoPatio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conteiner_id", nullable = false)
    private ConteinerPatio conteiner;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_movimento", nullable = false, length = 30)
    private TipoMovimentoPatio tipoMovimento;

    @Column(name = "descricao", nullable = false, length = 160)
    private String descricao;

    @Column(name = "registrado_em", nullable = false)
    private LocalDateTime registradoEm;

    public MovimentoPatio() {
    }

    public MovimentoPatio(Long id, ConteinerPatio conteiner, TipoMovimentoPatio tipoMovimento,
                           String descricao, LocalDateTime registradoEm) {
        this.id = id;
        this.conteiner = conteiner;
        this.tipoMovimento = tipoMovimento;
        this.descricao = descricao;
        this.registradoEm = registradoEm;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public ConteinerPatio getConteiner() {
        return conteiner;
    }

    public void setConteiner(ConteinerPatio conteiner) {
        this.conteiner = conteiner;
    }

    public TipoMovimentoPatio getTipoMovimento() {
        return tipoMovimento;
    }

    public void setTipoMovimento(TipoMovimentoPatio tipoMovimento) {
        this.tipoMovimento = tipoMovimento;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public LocalDateTime getRegistradoEm() {
        return registradoEm;
    }

    public void setRegistradoEm(LocalDateTime registradoEm) {
        this.registradoEm = registradoEm;
    }
}
