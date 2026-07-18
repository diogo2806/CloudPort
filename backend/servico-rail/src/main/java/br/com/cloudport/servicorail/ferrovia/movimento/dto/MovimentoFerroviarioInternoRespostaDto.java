package br.com.cloudport.servicorail.ferrovia.movimento.dto;

import br.com.cloudport.servicorail.ferrovia.movimento.modelo.EstadoMovimentoFerroviarioInterno;
import br.com.cloudport.servicorail.ferrovia.movimento.modelo.MovimentoFerroviarioInterno;
import br.com.cloudport.servicorail.ferrovia.movimento.modelo.ReservaRecursoFerroviario;
import br.com.cloudport.servicorail.ferrovia.movimento.modelo.TipoRecursoFerroviario;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.web.util.HtmlUtils;

public class MovimentoFerroviarioInternoRespostaDto {

    private final Long id;
    private final String codigoMovimento;
    private final Long visitaTremId;
    private final String identificadorTrem;
    private final String origem;
    private final String destino;
    private final LocalDateTime inicioPlanejado;
    private final LocalDateTime fimPlanejado;
    private final EstadoMovimentoFerroviarioInterno estado;
    private final boolean reservaAtiva;
    private final List<RecursoResposta> recursos;
    private final String motivoCancelamento;
    private final String planejadoPor;
    private final String autorizadoPor;
    private final String iniciadoPor;
    private final String concluidoPor;
    private final String canceladoPor;
    private final LocalDateTime criadoEm;
    private final LocalDateTime atualizadoEm;
    private final LocalDateTime autorizadoEm;
    private final LocalDateTime iniciadoEm;
    private final LocalDateTime concluidoEm;
    private final LocalDateTime canceladoEm;
    private final Long versao;

    private MovimentoFerroviarioInternoRespostaDto(MovimentoFerroviarioInterno movimento) {
        this.id = movimento.getId();
        this.codigoMovimento = movimento.getCodigoMovimento();
        this.visitaTremId = movimento.getVisitaTrem().getId();
        this.identificadorTrem = escapar(movimento.getVisitaTrem().getIdentificadorTrem());
        this.origem = escapar(movimento.getOrigem());
        this.destino = escapar(movimento.getDestino());
        this.inicioPlanejado = movimento.getInicioPlanejado();
        this.fimPlanejado = movimento.getFimPlanejado();
        this.estado = movimento.getEstado();
        this.reservaAtiva = movimento.isReservaAtiva();
        this.recursos = movimento.getRecursos().stream()
                .map(RecursoResposta::new)
                .collect(Collectors.toList());
        this.motivoCancelamento = escapar(movimento.getMotivoCancelamento());
        this.planejadoPor = escapar(movimento.getPlanejadoPor());
        this.autorizadoPor = escapar(movimento.getAutorizadoPor());
        this.iniciadoPor = escapar(movimento.getIniciadoPor());
        this.concluidoPor = escapar(movimento.getConcluidoPor());
        this.canceladoPor = escapar(movimento.getCanceladoPor());
        this.criadoEm = movimento.getCriadoEm();
        this.atualizadoEm = movimento.getAtualizadoEm();
        this.autorizadoEm = movimento.getAutorizadoEm();
        this.iniciadoEm = movimento.getIniciadoEm();
        this.concluidoEm = movimento.getConcluidoEm();
        this.canceladoEm = movimento.getCanceladoEm();
        this.versao = movimento.getVersao();
    }

    public static MovimentoFerroviarioInternoRespostaDto deEntidade(MovimentoFerroviarioInterno movimento) {
        return new MovimentoFerroviarioInternoRespostaDto(movimento);
    }

    private static String escapar(String valor) {
        return valor == null ? null : HtmlUtils.htmlEscape(valor);
    }

    public Long getId() {
        return id;
    }

    public String getCodigoMovimento() {
        return codigoMovimento;
    }

    public Long getVisitaTremId() {
        return visitaTremId;
    }

    public String getIdentificadorTrem() {
        return identificadorTrem;
    }

    public String getOrigem() {
        return origem;
    }

    public String getDestino() {
        return destino;
    }

    public LocalDateTime getInicioPlanejado() {
        return inicioPlanejado;
    }

    public LocalDateTime getFimPlanejado() {
        return fimPlanejado;
    }

    public EstadoMovimentoFerroviarioInterno getEstado() {
        return estado;
    }

    public boolean isReservaAtiva() {
        return reservaAtiva;
    }

    public List<RecursoResposta> getRecursos() {
        return recursos;
    }

    public String getMotivoCancelamento() {
        return motivoCancelamento;
    }

    public String getPlanejadoPor() {
        return planejadoPor;
    }

    public String getAutorizadoPor() {
        return autorizadoPor;
    }

    public String getIniciadoPor() {
        return iniciadoPor;
    }

    public String getConcluidoPor() {
        return concluidoPor;
    }

    public String getCanceladoPor() {
        return canceladoPor;
    }

    public LocalDateTime getCriadoEm() {
        return criadoEm;
    }

    public LocalDateTime getAtualizadoEm() {
        return atualizadoEm;
    }

    public LocalDateTime getAutorizadoEm() {
        return autorizadoEm;
    }

    public LocalDateTime getIniciadoEm() {
        return iniciadoEm;
    }

    public LocalDateTime getConcluidoEm() {
        return concluidoEm;
    }

    public LocalDateTime getCanceladoEm() {
        return canceladoEm;
    }

    public Long getVersao() {
        return versao;
    }

    public static class RecursoResposta {

        private final TipoRecursoFerroviario tipo;
        private final String codigo;
        private final LocalDateTime inicioReserva;
        private final LocalDateTime fimReserva;
        private final boolean ativo;

        private RecursoResposta(ReservaRecursoFerroviario recurso) {
            this.tipo = recurso.getTipoRecurso();
            this.codigo = escapar(recurso.getCodigoRecurso());
            this.inicioReserva = recurso.getInicioReserva();
            this.fimReserva = recurso.getFimReserva();
            this.ativo = recurso.isAtivo();
        }

        public TipoRecursoFerroviario getTipo() {
            return tipo;
        }

        public String getCodigo() {
            return codigo;
        }

        public LocalDateTime getInicioReserva() {
            return inicioReserva;
        }

        public LocalDateTime getFimReserva() {
            return fimReserva;
        }

        public boolean isAtivo() {
            return ativo;
        }
    }
}
