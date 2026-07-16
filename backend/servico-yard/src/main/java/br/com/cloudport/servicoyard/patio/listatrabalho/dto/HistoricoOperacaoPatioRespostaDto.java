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
        dto.id = historico.getId();
        dto.workQueueId = historico.getWorkQueueId();
        dto.ordemTrabalhoPatioId = historico.getOrdemTrabalhoPatioId();
        dto.acao = historico.getAcao();
        dto.usuario = historico.getUsuario();
        dto.motivo = historico.getMotivo();
        dto.detalhes = historico.getDetalhes();
        dto.criadoEm = historico.getCriadoEm();
        return dto;
    }

    public Long getId() { return id; }
    public Long getWorkQueueId() { return workQueueId; }
    public Long getOrdemTrabalhoPatioId() { return ordemTrabalhoPatioId; }
    public String getAcao() { return acao; }
    public String getUsuario() { return usuario; }
    public String getMotivo() { return motivo; }
    public String getDetalhes() { return detalhes; }
    public LocalDateTime getCriadoEm() { return criadoEm; }
}
