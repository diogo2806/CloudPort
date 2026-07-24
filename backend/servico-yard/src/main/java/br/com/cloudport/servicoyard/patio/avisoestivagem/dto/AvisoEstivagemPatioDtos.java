package br.com.cloudport.servicoyard.patio.avisoestivagem.dto;

import br.com.cloudport.servicoyard.patio.avisoestivagem.modelo.AvisoEstivagemPatio;
import br.com.cloudport.servicoyard.patio.avisoestivagem.modelo.AvisoEstivagemPatioEnums.SeveridadeAvisoEstivagemPatio;
import br.com.cloudport.servicoyard.patio.avisoestivagem.modelo.AvisoEstivagemPatioEnums.StatusAvisoEstivagemPatio;
import br.com.cloudport.servicoyard.patio.avisoestivagem.modelo.AvisoEstivagemPatioEnums.TipoEventoHistoricoAvisoEstivagemPatio;
import br.com.cloudport.servicoyard.patio.avisoestivagem.modelo.AvisoEstivagemPatioEnums.TipoRegraEstivagemPatio;
import br.com.cloudport.servicoyard.patio.avisoestivagem.modelo.HistoricoAvisoEstivagemPatio;
import java.time.LocalDateTime;
import javax.validation.constraints.NotBlank;

public final class AvisoEstivagemPatioDtos {

    private AvisoEstivagemPatioDtos() {
    }

    public static class AvisoResposta {
        private Long id;
        private String chaveEstavel;
        private String codigoUnidade;
        private Long posicaoId;
        private String bloco;
        private Integer linha;
        private Integer coluna;
        private String camada;
        private TipoRegraEstivagemPatio regra;
        private SeveridadeAvisoEstivagemPatio severidade;
        private StatusAvisoEstivagemPatio status;
        private String descricao;
        private String valorObservado;
        private String valorEsperado;
        private String acaoSugerida;
        private String responsavel;
        private LocalDateTime prazo;
        private String acaoCorretiva;
        private String evidencia;
        private String resultado;
        private LocalDateTime abertoEm;
        private LocalDateTime ultimaRevalidacaoEm;
        private LocalDateTime resolvidoEm;
        private Integer recorrencias;
        private LocalDateTime atualizadoEm;

        public static AvisoResposta de(AvisoEstivagemPatio aviso) {
            AvisoResposta resposta = new AvisoResposta();
            resposta.id = aviso.getId();
            resposta.chaveEstavel = aviso.getChaveEstavel();
            resposta.codigoUnidade = aviso.getCodigoUnidade();
            resposta.posicaoId = aviso.getPosicaoId();
            resposta.bloco = aviso.getBloco();
            resposta.linha = aviso.getLinha();
            resposta.coluna = aviso.getColuna();
            resposta.camada = aviso.getCamada();
            resposta.regra = aviso.getRegra();
            resposta.severidade = aviso.getSeveridade();
            resposta.status = aviso.getStatus();
            resposta.descricao = aviso.getDescricao();
            resposta.valorObservado = aviso.getValorObservado();
            resposta.valorEsperado = aviso.getValorEsperado();
            resposta.acaoSugerida = aviso.getAcaoSugerida();
            resposta.responsavel = aviso.getResponsavel();
            resposta.prazo = aviso.getPrazo();
            resposta.acaoCorretiva = aviso.getAcaoCorretiva();
            resposta.evidencia = aviso.getEvidencia();
            resposta.resultado = aviso.getResultado();
            resposta.abertoEm = aviso.getAbertoEm();
            resposta.ultimaRevalidacaoEm = aviso.getUltimaRevalidacaoEm();
            resposta.resolvidoEm = aviso.getResolvidoEm();
            resposta.recorrencias = aviso.getRecorrencias();
            resposta.atualizadoEm = aviso.getAtualizadoEm();
            return resposta;
        }

        public Long getId() { return id; }
        public String getChaveEstavel() { return chaveEstavel; }
        public String getCodigoUnidade() { return codigoUnidade; }
        public Long getPosicaoId() { return posicaoId; }
        public String getBloco() { return bloco; }
        public Integer getLinha() { return linha; }
        public Integer getColuna() { return coluna; }
        public String getCamada() { return camada; }
        public TipoRegraEstivagemPatio getRegra() { return regra; }
        public SeveridadeAvisoEstivagemPatio getSeveridade() { return severidade; }
        public StatusAvisoEstivagemPatio getStatus() { return status; }
        public String getDescricao() { return descricao; }
        public String getValorObservado() { return valorObservado; }
        public String getValorEsperado() { return valorEsperado; }
        public String getAcaoSugerida() { return acaoSugerida; }
        public String getResponsavel() { return responsavel; }
        public LocalDateTime getPrazo() { return prazo; }
        public String getAcaoCorretiva() { return acaoCorretiva; }
        public String getEvidencia() { return evidencia; }
        public String getResultado() { return resultado; }
        public LocalDateTime getAbertoEm() { return abertoEm; }
        public LocalDateTime getUltimaRevalidacaoEm() { return ultimaRevalidacaoEm; }
        public LocalDateTime getResolvidoEm() { return resolvidoEm; }
        public Integer getRecorrencias() { return recorrencias; }
        public LocalDateTime getAtualizadoEm() { return atualizadoEm; }
    }

    public static class HistoricoResposta {
        private Long id;
        private TipoEventoHistoricoAvisoEstivagemPatio tipoEvento;
        private StatusAvisoEstivagemPatio statusAnterior;
        private StatusAvisoEstivagemPatio statusNovo;
        private String ator;
        private String detalhes;
        private String evidencia;
        private String resultado;
        private LocalDateTime criadoEm;

        public static HistoricoResposta de(HistoricoAvisoEstivagemPatio historico) {
            HistoricoResposta resposta = new HistoricoResposta();
            resposta.id = historico.getId();
            resposta.tipoEvento = historico.getTipoEvento();
            resposta.statusAnterior = historico.getStatusAnterior();
            resposta.statusNovo = historico.getStatusNovo();
            resposta.ator = historico.getAtor();
            resposta.detalhes = historico.getDetalhes();
            resposta.evidencia = historico.getEvidencia();
            resposta.resultado = historico.getResultado();
            resposta.criadoEm = historico.getCriadoEm();
            return resposta;
        }

        public Long getId() { return id; }
        public TipoEventoHistoricoAvisoEstivagemPatio getTipoEvento() { return tipoEvento; }
        public StatusAvisoEstivagemPatio getStatusAnterior() { return statusAnterior; }
        public StatusAvisoEstivagemPatio getStatusNovo() { return statusNovo; }
        public String getAtor() { return ator; }
        public String getDetalhes() { return detalhes; }
        public String getEvidencia() { return evidencia; }
        public String getResultado() { return resultado; }
        public LocalDateTime getCriadoEm() { return criadoEm; }
    }

    public static class AtribuicaoRequisicao {
        @NotBlank
        private String responsavel;
        private LocalDateTime prazo;

        public String getResponsavel() { return responsavel; }
        public void setResponsavel(String responsavel) { this.responsavel = responsavel; }
        public LocalDateTime getPrazo() { return prazo; }
        public void setPrazo(LocalDateTime prazo) { this.prazo = prazo; }
    }

    public static class CorrecaoRequisicao {
        @NotBlank
        private String acaoCorretiva;
        private String evidencia;

        public String getAcaoCorretiva() { return acaoCorretiva; }
        public void setAcaoCorretiva(String acaoCorretiva) { this.acaoCorretiva = acaoCorretiva; }
        public String getEvidencia() { return evidencia; }
        public void setEvidencia(String evidencia) { this.evidencia = evidencia; }
    }

    public static class RevalidacaoRequisicao {
        private String evidencia;

        public String getEvidencia() { return evidencia; }
        public void setEvidencia(String evidencia) { this.evidencia = evidencia; }
    }
}
