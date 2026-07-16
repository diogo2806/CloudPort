package br.com.cloudport.servicoyard.patio.listatrabalho.dto;

import br.com.cloudport.servicoyard.patio.listatrabalho.modelo.HistoricoOperacaoPatio;
import java.time.LocalDateTime;

public class HistoricoOperacaoPatioRespostaDto {

    private Long id;
    private Long workQueueId;
    private Long ordemTrabalhoPatioId;
    private String acao;
    private String usuario;
    private String motivo;
    private String detalhes;
    private LocalDateTime criadoEm;

    public static HistoricoOperacaoPatioRespostaDto deEntidade(HistoricoOperacaoPatio historico) {
        HistoricoOperacaoPatioRespostaDto dto = new HistoricoOperacaoPatioRespostaDto();
        dto.setId(historico.getId());
        dto.setWorkQueueId(historico.getWorkQueueId());
        dto.setOrdemTrabalhoPatioId(historico.getOrdemTrabalhoPatioId());
        dto.setAcao(historico.getAcao());
        dto.setUsuario(historico.getUsuario());
        dto.setMotivo(historico.getMotivo());
        dto.setDetalhes(historico.getDetalhes());
        dto.setCriadoEm(historico.getCriadoEm());
        return dto;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getWorkQueueId() { return workQueueId; }
    public void setWorkQueueId(Long workQueueId) { this.workQueueId = workQueueId; }
    public Long getOrdemTrabalhoPatioId() { return ordemTrabalhoPatioId; }
    public void setOrdemTrabalhoPatioId(Long ordemTrabalhoPatioId) { this.ordemTrabalhoPatioId = ordemTrabalhoPatioId; }
    public String getAcao() { return acao; }
    public void setAcao(String acao) { this.acao = acao; }
    public String getUsuario() { return usuario; }
    public void setUsuario(String usuario) { this.usuario = usuario; }
    public String getMotivo() { return motivo; }
    public void setMotivo(String motivo) { this.motivo = motivo; }
    public String getDetalhes() { return detalhes; }
    public void setDetalhes(String detalhes) { this.detalhes = detalhes; }
    public LocalDateTime getCriadoEm() { return criadoEm; }
    public void setCriadoEm(LocalDateTime criadoEm) { this.criadoEm = criadoEm; }
}
