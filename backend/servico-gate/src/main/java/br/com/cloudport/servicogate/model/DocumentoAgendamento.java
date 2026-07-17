package br.com.cloudport.servicogate.model;

import br.com.cloudport.servicogate.model.enums.StatusValidacaoDocumento;
import java.time.LocalDateTime;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@Entity
@Table(name = "documento_agendamento")
public class DocumentoAgendamento extends AbstractAuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tipo_documento", nullable = false, length = 80)
    private String tipoDocumento;

    @Column(name = "numero", length = 80)
    private String numero;

    @Column(name = "url_documento", length = 255)
    private String urlDocumento;

    @Column(name = "nome_arquivo", length = 255)
    private String nomeArquivo;

    @Column(name = "content_type", length = 120)
    private String contentType;

    @Column(name = "tamanho_bytes")
    private Long tamanhoBytes;

    @Column(name = "ultima_revalidacao")
    private LocalDateTime ultimaRevalidacao;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_validacao", nullable = false, length = 30)
    private StatusValidacaoDocumento statusValidacao = StatusValidacaoDocumento.PENDENTE;

    @Column(name = "mensagem_validacao", length = 500)
    private String mensagemValidacao;

    @Column(name = "tentativas_ocr", nullable = false)
    private Integer tentativasOcr = 0;

    @Column(name = "ultimo_erro_ocr", length = 500)
    private String ultimoErroOcr;

    @Column(name = "proxima_tentativa_ocr_em")
    private LocalDateTime proximaTentativaOcrEm;

    @Column(name = "processamento_ocr_iniciado_em")
    private LocalDateTime processamentoOcrIniciadoEm;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agendamento_id", nullable = false)
    private Agendamento agendamento;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTipoDocumento() { return tipoDocumento; }
    public void setTipoDocumento(String tipoDocumento) { this.tipoDocumento = tipoDocumento; }
    public String getNumero() { return numero; }
    public void setNumero(String numero) { this.numero = numero; }
    public String getUrlDocumento() { return urlDocumento; }
    public void setUrlDocumento(String urlDocumento) { this.urlDocumento = urlDocumento; }
    public String getNomeArquivo() { return nomeArquivo; }
    public void setNomeArquivo(String nomeArquivo) { this.nomeArquivo = nomeArquivo; }
    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }
    public Long getTamanhoBytes() { return tamanhoBytes; }
    public void setTamanhoBytes(Long tamanhoBytes) { this.tamanhoBytes = tamanhoBytes; }
    public LocalDateTime getUltimaRevalidacao() { return ultimaRevalidacao; }
    public void setUltimaRevalidacao(LocalDateTime ultimaRevalidacao) { this.ultimaRevalidacao = ultimaRevalidacao; }
    public StatusValidacaoDocumento getStatusValidacao() { return statusValidacao; }
    public void setStatusValidacao(StatusValidacaoDocumento statusValidacao) { this.statusValidacao = statusValidacao; }
    public String getMensagemValidacao() { return mensagemValidacao; }
    public void setMensagemValidacao(String mensagemValidacao) { this.mensagemValidacao = mensagemValidacao; }
    public Integer getTentativasOcr() { return tentativasOcr; }
    public void setTentativasOcr(Integer tentativasOcr) { this.tentativasOcr = tentativasOcr; }
    public String getUltimoErroOcr() { return ultimoErroOcr; }
    public void setUltimoErroOcr(String ultimoErroOcr) { this.ultimoErroOcr = ultimoErroOcr; }
    public LocalDateTime getProximaTentativaOcrEm() { return proximaTentativaOcrEm; }
    public void setProximaTentativaOcrEm(LocalDateTime proximaTentativaOcrEm) { this.proximaTentativaOcrEm = proximaTentativaOcrEm; }
    public LocalDateTime getProcessamentoOcrIniciadoEm() { return processamentoOcrIniciadoEm; }
    public void setProcessamentoOcrIniciadoEm(LocalDateTime processamentoOcrIniciadoEm) { this.processamentoOcrIniciadoEm = processamentoOcrIniciadoEm; }
    public Agendamento getAgendamento() { return agendamento; }
    public void setAgendamento(Agendamento agendamento) { this.agendamento = agendamento; }
}
