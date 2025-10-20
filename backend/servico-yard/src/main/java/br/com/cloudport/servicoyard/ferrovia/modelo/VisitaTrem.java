package br.com.cloudport.servicoyard.ferrovia.modelo;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Table;

@Entity
@Table(name = "visita_trem")
public class VisitaTrem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "identificador_trem", nullable = false, length = 40)
    private String identificadorTrem;

    @Column(name = "operadora_ferroviaria", nullable = false, length = 80)
    private String operadoraFerroviaria;

    @Column(name = "hora_chegada_prevista", nullable = false)
    private LocalDateTime horaChegadaPrevista;

    @Column(name = "hora_partida_prevista", nullable = false)
    private LocalDateTime horaPartidaPrevista;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_visita", nullable = false, length = 20)
    private StatusVisitaTrem statusVisita;

    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm;

    @Column(name = "atualizado_em", nullable = false)
    private LocalDateTime atualizadoEm;

    @ElementCollection
    @CollectionTable(name = "visita_trem_descarga", joinColumns = @JoinColumn(name = "visita_trem_id"))
    private List<OperacaoConteinerVisita> listaDescarga = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "visita_trem_carga", joinColumns = @JoinColumn(name = "visita_trem_id"))
    private List<OperacaoConteinerVisita> listaCarga = new ArrayList<>();

    public VisitaTrem() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getIdentificadorTrem() {
        return identificadorTrem;
    }

    public void setIdentificadorTrem(String identificadorTrem) {
        this.identificadorTrem = identificadorTrem;
    }

    public String getOperadoraFerroviaria() {
        return operadoraFerroviaria;
    }

    public void setOperadoraFerroviaria(String operadoraFerroviaria) {
        this.operadoraFerroviaria = operadoraFerroviaria;
    }

    public LocalDateTime getHoraChegadaPrevista() {
        return horaChegadaPrevista;
    }

    public void setHoraChegadaPrevista(LocalDateTime horaChegadaPrevista) {
        this.horaChegadaPrevista = horaChegadaPrevista;
    }

    public LocalDateTime getHoraPartidaPrevista() {
        return horaPartidaPrevista;
    }

    public void setHoraPartidaPrevista(LocalDateTime horaPartidaPrevista) {
        this.horaPartidaPrevista = horaPartidaPrevista;
    }

    public StatusVisitaTrem getStatusVisita() {
        return statusVisita;
    }

    public void setStatusVisita(StatusVisitaTrem statusVisita) {
        this.statusVisita = statusVisita;
    }

    public LocalDateTime getCriadoEm() {
        return criadoEm;
    }

    public void setCriadoEm(LocalDateTime criadoEm) {
        this.criadoEm = criadoEm;
    }

    public LocalDateTime getAtualizadoEm() {
        return atualizadoEm;
    }

    public void setAtualizadoEm(LocalDateTime atualizadoEm) {
        this.atualizadoEm = atualizadoEm;
    }

    public List<OperacaoConteinerVisita> getListaDescarga() {
        return listaDescarga;
    }

    public void definirListaDescarga(List<OperacaoConteinerVisita> listaDescarga) {
        this.listaDescarga.clear();
        if (listaDescarga != null) {
            this.listaDescarga.addAll(listaDescarga);
        }
    }

    public List<OperacaoConteinerVisita> getListaCarga() {
        return listaCarga;
    }

    public void definirListaCarga(List<OperacaoConteinerVisita> listaCarga) {
        this.listaCarga.clear();
        if (listaCarga != null) {
            this.listaCarga.addAll(listaCarga);
        }
    }

    @PrePersist
    public void aoCriar() {
        LocalDateTime agora = LocalDateTime.now();
        this.criadoEm = agora;
        this.atualizadoEm = agora;
    }

    @PreUpdate
    public void aoAtualizar() {
        this.atualizadoEm = LocalDateTime.now();
    }
}
