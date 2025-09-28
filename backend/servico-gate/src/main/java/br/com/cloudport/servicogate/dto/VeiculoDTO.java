package br.com.cloudport.servicogate.dto;

public class VeiculoDTO {

    private Long id;
    private String placa;
    private String modelo;
    private String tipo;
    private Long transportadoraId;
    private String transportadoraNome;

    public VeiculoDTO() {
    }

    public VeiculoDTO(Long id, String placa, String modelo, String tipo,
                       Long transportadoraId, String transportadoraNome) {
        this.id = id;
        this.placa = placa;
        this.modelo = modelo;
        this.tipo = tipo;
        this.transportadoraId = transportadoraId;
        this.transportadoraNome = transportadoraNome;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getPlaca() {
        return placa;
    }

    public void setPlaca(String placa) {
        this.placa = placa;
    }

    public String getModelo() {
        return modelo;
    }

    public void setModelo(String modelo) {
        this.modelo = modelo;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public Long getTransportadoraId() {
        return transportadoraId;
    }

    public void setTransportadoraId(Long transportadoraId) {
        this.transportadoraId = transportadoraId;
    }

    public String getTransportadoraNome() {
        return transportadoraNome;
    }

    public void setTransportadoraNome(String transportadoraNome) {
        this.transportadoraNome = transportadoraNome;
    }
}
