package br.com.cloudport.servicoautenticacao.app.notificacoes.dto;

public class CanalNotificacaoDTO {

    private final Long identificador;
    private final String nomeCanal;
    private final boolean habilitado;

    public CanalNotificacaoDTO(Long identificador, String nomeCanal, boolean habilitado) {
        this.identificador = identificador;
        this.nomeCanal = nomeCanal;
        this.habilitado = habilitado;
    }

    public Long getIdentificador() {
        return identificador;
    }

    public String getNomeCanal() {
        return nomeCanal;
    }

    public boolean isHabilitado() {
        return habilitado;
    }
}
