package br.com.cloudport.servicoyard.patio.controller;

import br.com.cloudport.servicoyard.patio.modelo.InstrucaoTrabalho;
import br.com.cloudport.servicoyard.patio.modelo.PrioridadeInstrucao;
import br.com.cloudport.servicoyard.patio.modelo.StatusInstrucao;
import br.com.cloudport.servicoyard.patio.modelo.TipoOperacaoInstrucao;
import br.com.cloudport.servicoyard.patio.servico.InstrucaoTrabalhoServico;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/yard/work-instructions")
@Tag(name = "Instruções de trabalho", description = "Planejamento e execução de atividades do pátio")
@PreAuthorize("hasAnyRole('ADMIN_PORTO','OPERADOR_PATIO','PLANEJADOR')")
public class InstrucaoTrabalhoController {

    private final InstrucaoTrabalhoServico servico;

    public InstrucaoTrabalhoController(InstrucaoTrabalhoServico servico) {
        this.servico = servico;
    }

    @PostMapping
    @Operation(summary = "Cria uma instrução de trabalho")
    public ResponseEntity<Map<String, Object>> criar(@Valid @RequestBody InstrucaoRequest request) {
        InstrucaoTrabalho instrucao = servico.criar(
                request.getCodigoConteiner(), request.getTipoOperacao(), request.getOrigem(), request.getDestino(),
                request.getPrioridade(), request.getAgendadaEm(), request.getEquipamento(), request.getEquipe(),
                request.getObservacoes(), request.getCriadoPor());
        return ResponseEntity.ok(mapear(instrucao));
    }

    @GetMapping
    @Operation(summary = "Pesquisa instruções por status e contêiner")
    public List<Map<String, Object>> pesquisar(@RequestParam(required = false) StatusInstrucao status,
                                                @RequestParam(required = false) String codigoConteiner) {
        return servico.pesquisar(status, codigoConteiner).stream()
                .map(this::mapear)
                .collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consulta os detalhes de uma instrução")
    public ResponseEntity<Map<String, Object>> obter(@PathVariable Long id) {
        return ResponseEntity.ok(mapear(servico.obter(id)));
    }

    @PostMapping("/{id}/start")
    @Operation(summary = "Inicia a execução de uma instrução")
    public ResponseEntity<Map<String, Object>> iniciar(@PathVariable Long id) {
        return ResponseEntity.ok(mapear(servico.iniciar(id)));
    }

    @PostMapping("/{id}/complete")
    @Operation(summary = "Conclui a instrução e atualiza o inventário")
    public ResponseEntity<Map<String, Object>> concluir(@PathVariable Long id) {
        return ResponseEntity.ok(mapear(servico.concluir(id)));
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "Cancela uma instrução não concluída")
    public ResponseEntity<Map<String, Object>> cancelar(@PathVariable Long id,
                                                         @Valid @RequestBody CancelamentoRequest request) {
        return ResponseEntity.ok(mapear(servico.cancelar(id, request.getJustificativa())));
    }

    private Map<String, Object> mapear(InstrucaoTrabalho instrucao) {
        Map<String, Object> dto = new LinkedHashMap<>();
        dto.put("id", instrucao.getId());
        dto.put("codigoConteiner", instrucao.getCodigoConteiner());
        dto.put("tipoOperacao", instrucao.getTipoOperacao());
        dto.put("origem", instrucao.getOrigem());
        dto.put("destino", instrucao.getDestino());
        dto.put("prioridade", instrucao.getPrioridade());
        dto.put("status", instrucao.getStatus());
        dto.put("agendadaEm", instrucao.getAgendadaEm());
        dto.put("iniciadaEm", instrucao.getIniciadaEm());
        dto.put("concluidaEm", instrucao.getConcluidaEm());
        dto.put("canceladaEm", instrucao.getCanceladaEm());
        dto.put("equipamento", instrucao.getEquipamento());
        dto.put("equipe", instrucao.getEquipe());
        dto.put("observacoes", instrucao.getObservacoes());
        dto.put("criadoPor", instrucao.getCriadoPor());
        dto.put("justificativaCancelamento", instrucao.getJustificativaCancelamento());
        dto.put("criadaEm", instrucao.getCreatedAt());
        dto.put("atualizadaEm", instrucao.getUpdatedAt());
        return dto;
    }

    public static class InstrucaoRequest {
        @NotBlank @Size(max = 30) private String codigoConteiner;
        @NotNull private TipoOperacaoInstrucao tipoOperacao;
        @Size(max = 80) private String origem;
        @Size(max = 80) private String destino;
        private PrioridadeInstrucao prioridade = PrioridadeInstrucao.NORMAL;
        private LocalDateTime agendadaEm;
        @Size(max = 30) private String equipamento;
        @Size(max = 80) private String equipe;
        @Size(max = 1000) private String observacoes;
        @NotBlank @Size(max = 80) @Schema(example = "operador.patio") private String criadoPor;
        public String getCodigoConteiner() { return codigoConteiner; }
        public void setCodigoConteiner(String codigoConteiner) { this.codigoConteiner = codigoConteiner; }
        public TipoOperacaoInstrucao getTipoOperacao() { return tipoOperacao; }
        public void setTipoOperacao(TipoOperacaoInstrucao tipoOperacao) { this.tipoOperacao = tipoOperacao; }
        public String getOrigem() { return origem; }
        public void setOrigem(String origem) { this.origem = origem; }
        public String getDestino() { return destino; }
        public void setDestino(String destino) { this.destino = destino; }
        public PrioridadeInstrucao getPrioridade() { return prioridade; }
        public void setPrioridade(PrioridadeInstrucao prioridade) { this.prioridade = prioridade; }
        public LocalDateTime getAgendadaEm() { return agendadaEm; }
        public void setAgendadaEm(LocalDateTime agendadaEm) { this.agendadaEm = agendadaEm; }
        public String getEquipamento() { return equipamento; }
        public void setEquipamento(String equipamento) { this.equipamento = equipamento; }
        public String getEquipe() { return equipe; }
        public void setEquipe(String equipe) { this.equipe = equipe; }
        public String getObservacoes() { return observacoes; }
        public void setObservacoes(String observacoes) { this.observacoes = observacoes; }
        public String getCriadoPor() { return criadoPor; }
        public void setCriadoPor(String criadoPor) { this.criadoPor = criadoPor; }
    }

    public static class CancelamentoRequest {
        @NotBlank @Size(max = 500) private String justificativa;
        public String getJustificativa() { return justificativa; }
        public void setJustificativa(String justificativa) { this.justificativa = justificativa; }
    }
}
