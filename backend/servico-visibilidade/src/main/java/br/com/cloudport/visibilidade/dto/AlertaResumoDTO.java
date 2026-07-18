package br.com.cloudport.visibilidade.dto;

public class AlertaResumoDTO {

    private final long totalAtivos;
    private final long criticos;
    private final long altos;
    private final long medios;
    private final long baixos;
    private final long naoReconhecidos;

    public AlertaResumoDTO(long totalAtivos,
                           long criticos,
                           long altos,
                           long medios,
                           long baixos,
                           long naoReconhecidos) {
        this.totalAtivos = totalAtivos;
        this.criticos = criticos;
        this.altos = altos;
        this.medios = medios;
        this.baixos = baixos;
        this.naoReconhecidos = naoReconhecidos;
    }

    public long getTotalAtivos() { return totalAtivos; }
    public long getCriticos() { return criticos; }
    public long getAltos() { return altos; }
    public long getMedios() { return medios; }
    public long getBaixos() { return baixos; }
    public long getNaoReconhecidos() { return naoReconhecidos; }
}
