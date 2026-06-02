package br.com.cloudport.visibilidade.entity;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "conteiner_localizacao")
public class ConteinerLocalizacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "container_id", unique = true, nullable = false)
    private String containerId;

    @Column(name = "status_atual")
    private String statusAtual; // no_yard, em_navio, saiu_do_porto, etc.

    @Column(name = "zona")
    private String zona;

    @Column(name = "posicao")
    private String posicao;

    @Column(name = "latitude")
    private Double latitude;

    @Column(name = "longitude")
    private Double longitude;

    @Column(name = "navio_destino_id")
    private String navioDestinoId;

    @Column(name = "data_atualizacao")
    private LocalDateTime dataAtualizacao;

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getContainerId() { return containerId; }
    public void setContainerId(String containerId) { this.containerId = containerId; }

    public String getStatusAtual() { return statusAtual; }
    public void setStatusAtual(String statusAtual) { this.statusAtual = statusAtual; }

    public String getZona() { return zona; }
    public void setZona(String zona) { this.zona = zona; }

    public String getPosicao() { return posicao; }
    public void setPosicao(String posicao) { this.posicao = posicao; }

    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }

    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }

    public String getNavioDestinoId() { return navioDestinoId; }
    public void setNavioDestinoId(String navioDestinoId) { this.navioDestinoId = navioDestinoId; }

    public LocalDateTime getDataAtualizacao() { return dataAtualizacao; }
    public void setDataAtualizacao(LocalDateTime dataAtualizacao) { this.dataAtualizacao = dataAtualizacao; }
}
