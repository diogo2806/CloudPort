package br.com.cloudport.servicoyard.scheduler.dto;

import br.com.cloudport.servicoyard.scheduler.modelo.EstadoPlanoPosicaoOperacional;
import br.com.cloudport.servicoyard.scheduler.modelo.PlanoPosicaoOperacional;
import java.time.LocalDateTime;

public class PlanoPosicaoOperacionalRespostaDto {

    private Long id;
    private String codigoContainer;
    private Long ordemTrabalhoPatioId;
    private String bloco;
    private Integer linha;
    private Integer coluna;
    private String camada;
    private String equipamentoId;
    private EstadoPlanoPosicaoOperacional estado;
    private LocalDateTime horizonteInicio;
    private LocalDateTime horizonteFim;
    private LocalDateTime validoAte;
    private String origem;
    private String motivo;
    private String assinaturaEntrada;
    private String alteradoPor;
    private Long versao;
    private LocalDateTime convertidoEm;
    private LocalDateTime criadoEm;
    private LocalDateTime atualizadoEm;
    private long segundosAteExpiracao;

    public static PlanoPosicaoOperacionalRespostaDto deEntidade(PlanoPosicaoOperacional entidade) {
        PlanoPosicaoOperacionalRespostaDto dto = new PlanoPosicaoOperacionalRespostaDto();
        dto.id = entidade.getId();
        dto.codigoContainer = entidade.getCodigoContainer();
        dto.ordemTrabalhoPatioId = entidade.getOrdemTrabalhoPatioId();
        dto.bloco = entidade.getBloco();
        dto.linha = entidade.getLinha();
        dto.coluna = entidade.getColuna();
        dto.camada = entidade.getCamada();
        dto.equipamentoId = entidade.getEquipamentoId();
        dto.estado = entidade.getEstado();
        dto.horizonteInicio = entidade.getHorizonteInicio();
        dto.horizonteFim = entidade.getHorizonteFim();
        dto.validoAte = entidade.getValidoAte();
        dto.origem = entidade.getOrigem();
        dto.motivo = entidade.getMotivo();
        dto.assinaturaEntrada = entidade.getAssinaturaEntrada();
        dto.alteradoPor = entidade.getAlteradoPor();
        dto.versao = entidade.getVersao();
        dto.convertidoEm = entidade.getConvertidoEm();
        dto.criadoEm = entidade.getCriadoEm();
        dto.atualizadoEm = entidade.getAtualizadoEm();
        dto.segundosAteExpiracao = entidade.getValidoAte() == null
                ? 0
                : java.time.Duration.between(LocalDateTime.now(), entidade.getValidoAte()).getSeconds();
        return dto;
    }

    public Long getId() { return id; }
    public String getCodigoContainer() { return codigoContainer; }
    public Long getOrdemTrabalhoPatioId() { return ordemTrabalhoPatioId; }
    public String getBloco() { return bloco; }
    public Integer getLinha() { return linha; }
    public Integer getColuna() { return coluna; }
    public String getCamada() { return camada; }
    public String getEquipamentoId() { return equipamentoId; }
    public EstadoPlanoPosicaoOperacional getEstado() { return estado; }
    public LocalDateTime getHorizonteInicio() { return horizonteInicio; }
    public LocalDateTime getHorizonteFim() { return horizonteFim; }
    public LocalDateTime getValidoAte() { return validoAte; }
    public String getOrigem() { return origem; }
    public String getMotivo() { return motivo; }
    public String getAssinaturaEntrada() { return assinaturaEntrada; }
    public String getAlteradoPor() { return alteradoPor; }
    public Long getVersao() { return versao; }
    public LocalDateTime getConvertidoEm() { return convertidoEm; }
    public LocalDateTime getCriadoEm() { return criadoEm; }
    public LocalDateTime getAtualizadoEm() { return atualizadoEm; }
    public long getSegundosAteExpiracao() { return segundosAteExpiracao; }
}
