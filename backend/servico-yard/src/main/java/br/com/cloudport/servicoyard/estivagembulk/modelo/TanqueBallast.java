package br.com.cloudport.servicoyard.estivagembulk.modelo;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@Entity
@Table(name = "tanque_ballast")
public class TanqueBallast {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "navio_granel_id", nullable = false)
    private NavioGranel navio;

    @Column(length = 30, nullable = false)
    private String nome;

    @Column(name = "capacidade_m3", nullable = false)
    private Double capacidadeM3;

    @Column(name = "volume_atual_m3")
    private Double volumeAtualM3 = 0.0;

    @Column(name = "pos_long_centro_m")
    private Double posLongCentroM;

    @Column(name = "pos_trans_centro_m")
    private Double posTransCentroM;

    @Column(name = "vcg_cheio_m")
    private Double vcgCheioM;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public NavioGranel getNavio() { return navio; }
    public void setNavio(NavioGranel navio) { this.navio = navio; }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public Double getCapacidadeM3() { return capacidadeM3; }
    public void setCapacidadeM3(Double capacidadeM3) { this.capacidadeM3 = capacidadeM3; }

    public Double getVolumeAtualM3() { return volumeAtualM3; }
    public void setVolumeAtualM3(Double volumeAtualM3) { this.volumeAtualM3 = volumeAtualM3; }

    public Double getPosLongCentroM() { return posLongCentroM; }
    public void setPosLongCentroM(Double posLongCentroM) { this.posLongCentroM = posLongCentroM; }

    public Double getPosTransCentroM() { return posTransCentroM; }
    public void setPosTransCentroM(Double posTransCentroM) { this.posTransCentroM = posTransCentroM; }

    public Double getVcgCheioM() { return vcgCheioM; }
    public void setVcgCheioM(Double vcgCheioM) { this.vcgCheioM = vcgCheioM; }

    public double getPesoToneladas() {
        if (volumeAtualM3 == null || volumeAtualM3 <= 0) return 0.0;
        return volumeAtualM3 * 1.025;
    }
}
