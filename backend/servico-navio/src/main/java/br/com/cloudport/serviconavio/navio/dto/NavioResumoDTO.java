package br.com.cloudport.serviconavio.navio.dto;

public class NavioResumoDTO {

    private final Long identificador;
    private final String nome;
    private final String codigoImo;
    private final String empresaArmadora;
    private final Integer capacidadeTeu;

    public NavioResumoDTO(Long identificador,
                          String nome,
                          String codigoImo,
                          String empresaArmadora,
                          Integer capacidadeTeu) {
        this.identificador = identificador;
        this.nome = nome;
        this.codigoImo = codigoImo;
        this.empresaArmadora = empresaArmadora;
        this.capacidadeTeu = capacidadeTeu;
    }

    public Long getIdentificador() {
        return identificador;
    }

    public String getNome() {
        return nome;
    }

    public String getCodigoImo() {
        return codigoImo;
    }

    public String getEmpresaArmadora() {
        return empresaArmadora;
    }

    public Integer getCapacidadeTeu() {
        return capacidadeTeu;
    }
}
