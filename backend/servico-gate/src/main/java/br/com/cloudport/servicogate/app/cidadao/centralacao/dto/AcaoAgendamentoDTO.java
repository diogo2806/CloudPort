package br.com.cloudport.servicogate.app.cidadao.centralacao.dto;

public class AcaoAgendamentoDTO {

    private String codigo;
    private String titulo;
    private String descricao;
    private String metodoHttp;
    private String rotaApiRelativa;
    private boolean habilitada;

    public AcaoAgendamentoDTO() {
    }

    public AcaoAgendamentoDTO(String codigo,
                               String titulo,
                               String descricao,
                               String metodoHttp,
                               String rotaApiRelativa,
                               boolean habilitada) {
        this.codigo = codigo;
        this.titulo = titulo;
        this.descricao = descricao;
        this.metodoHttp = metodoHttp;
        this.rotaApiRelativa = rotaApiRelativa;
        this.habilitada = habilitada;
    }

    public String getCodigo() {
        return codigo;
    }

    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public String getMetodoHttp() {
        return metodoHttp;
    }

    public void setMetodoHttp(String metodoHttp) {
        this.metodoHttp = metodoHttp;
    }

    public String getRotaApiRelativa() {
        return rotaApiRelativa;
    }

    public void setRotaApiRelativa(String rotaApiRelativa) {
        this.rotaApiRelativa = rotaApiRelativa;
    }

    public boolean isHabilitada() {
        return habilitada;
    }

    public void setHabilitada(boolean habilitada) {
        this.habilitada = habilitada;
    }
}
