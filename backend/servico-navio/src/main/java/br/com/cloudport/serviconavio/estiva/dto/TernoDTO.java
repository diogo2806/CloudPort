package br.com.cloudport.serviconavio.estiva.dto;

import br.com.cloudport.serviconavio.estiva.entidade.Terno;

public class TernoDTO {

    private final Long id;
    private final String identificador;
    private final int sequencia;
    private final int baiaInicial;
    private final int baiaFinal;

    public TernoDTO(Long id, String identificador, int sequencia, int baiaInicial, int baiaFinal) {
        this.id = id;
        this.identificador = identificador;
        this.sequencia = sequencia;
        this.baiaInicial = baiaInicial;
        this.baiaFinal = baiaFinal;
    }

    public static TernoDTO deEntidade(Terno terno) {
        return new TernoDTO(
                terno.getId(),
                terno.getIdentificador(),
                terno.getSequencia(),
                terno.getBaiaInicial(),
                terno.getBaiaFinal()
        );
    }

    public Long getId() {
        return id;
    }

    public String getIdentificador() {
        return identificador;
    }

    public int getSequencia() {
        return sequencia;
    }

    public int getBaiaInicial() {
        return baiaInicial;
    }

    public int getBaiaFinal() {
        return baiaFinal;
    }
}
