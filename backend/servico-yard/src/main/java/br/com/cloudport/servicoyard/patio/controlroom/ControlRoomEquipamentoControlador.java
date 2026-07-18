package br.com.cloudport.servicoyard.patio.controlroom;

import java.util.List;
import javax.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/yard/control-room")
public class ControlRoomEquipamentoControlador {

    private static final String CONSULTA = "hasAnyRole('ADMIN_PORTO','PLANEJADOR','OPERADOR_PATIO','OPERADOR_GATE')";
    private static final String OPERACAO = "hasAnyRole('ADMIN_PORTO','PLANEJADOR','OPERADOR_PATIO')";
    private static final String DISPOSITIVO = "hasAnyRole('SERVICE_NAVIO','ADMIN_PORTO')";

    private final ControlRoomEquipamentoServico servico;
    private final ControlRoomEquipamentoStreamingServico streamingServico;

    public ControlRoomEquipamentoControlador(
            ControlRoomEquipamentoServico servico,
            ControlRoomEquipamentoStreamingServico streamingServico
    ) {
        this.servico = servico;
        this.streamingServico = streamingServico;
    }

    @GetMapping("/resumo")
    @PreAuthorize(CONSULTA)
    public ControlRoomEquipamentoDtos.Resumo resumo() {
        return servico.resumo();
    }

    @GetMapping("/equipamentos")
    @PreAuthorize(CONSULTA)
    public List<ControlRoomEquipamentoDtos.Equipamento> equipamentos(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String tipo,
            @RequestParam(required = false) String conectividade
    ) {
        return servico.listarEquipamentos(status, tipo, conectividade);
    }

    @GetMapping("/equipamentos/{identificador}/historico")
    @PreAuthorize(CONSULTA)
    public List<ControlRoomEquipamentoDtos.HistoricoTelemetria> historico(
            @PathVariable String identificador,
            @RequestParam(defaultValue = "100") int limite
    ) {
        return servico.historico(identificador, limite);
    }

    @GetMapping("/alarmes")
    @PreAuthorize(CONSULTA)
    public List<ControlRoomEquipamentoDtos.Alarme> alarmes(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String severidade
    ) {
        return servico.listarAlarmes(status, severidade);
    }

    @PatchMapping("/alarmes/{id}/reconhecer")
    @PreAuthorize(OPERACAO)
    public ControlRoomEquipamentoDtos.Alarme reconhecerAlarme(
            @PathVariable Long id,
            Authentication authentication
    ) {
        return servico.reconhecerAlarme(id, usuario(authentication));
    }

    @PatchMapping("/alarmes/{id}/resolver")
    @PreAuthorize(OPERACAO)
    public ControlRoomEquipamentoDtos.Alarme resolverAlarme(
            @PathVariable Long id,
            Authentication authentication
    ) {
        return servico.resolverAlarme(id, usuario(authentication));
    }

    @GetMapping("/comandos")
    @PreAuthorize(CONSULTA)
    public List<ControlRoomEquipamentoDtos.Comando> comandos(
            @RequestParam(required = false) String equipamento
    ) {
        return servico.listarComandos(equipamento);
    }

    @PostMapping("/equipamentos/{identificador}/comandos")
    @PreAuthorize(OPERACAO)
    public ControlRoomEquipamentoDtos.Comando criarComando(
            @PathVariable String identificador,
            @Valid @RequestBody ControlRoomEquipamentoDtos.ComandoRequisicao requisicao,
            Authentication authentication
    ) {
        return servico.criarComando(identificador, requisicao, usuario(authentication));
    }

    @GetMapping("/indisponibilidades")
    @PreAuthorize(CONSULTA)
    public List<ControlRoomEquipamentoDtos.Indisponibilidade> indisponibilidades(
            @RequestParam(required = false) String equipamento
    ) {
        return servico.listarIndisponibilidades(equipamento);
    }

    @PostMapping("/equipamentos/{identificador}/indisponibilidades")
    @PreAuthorize(OPERACAO)
    public ControlRoomEquipamentoDtos.Indisponibilidade iniciarIndisponibilidade(
            @PathVariable String identificador,
            @Valid @RequestBody ControlRoomEquipamentoDtos.IndisponibilidadeRequisicao requisicao,
            Authentication authentication
    ) {
        return servico.iniciarIndisponibilidade(identificador, requisicao, usuario(authentication));
    }

    @PatchMapping("/indisponibilidades/{id}/encerrar")
    @PreAuthorize(OPERACAO)
    public ControlRoomEquipamentoDtos.Indisponibilidade encerrarIndisponibilidade(
            @PathVariable Long id,
            @RequestBody(required = false) ControlRoomEquipamentoDtos.EncerramentoIndisponibilidadeRequisicao requisicao,
            Authentication authentication
    ) {
        return servico.encerrarIndisponibilidade(id, requisicao, usuario(authentication));
    }

    @GetMapping("/dispositivos")
    @PreAuthorize(CONSULTA)
    public List<ControlRoomEquipamentoDtos.Dispositivo> dispositivos() {
        return servico.listarDispositivos();
    }

    @PostMapping("/dispositivos/{dispositivo}/heartbeat")
    @PreAuthorize(DISPOSITIVO)
    public ControlRoomEquipamentoDtos.Dispositivo heartbeat(
            @PathVariable String dispositivo,
            @Valid @RequestBody ControlRoomEquipamentoDtos.HeartbeatRequisicao requisicao
    ) {
        return servico.heartbeat(dispositivo, requisicao);
    }

    @GetMapping("/dispositivos/{dispositivo}/comandos-pendentes")
    @PreAuthorize(DISPOSITIVO)
    public List<ControlRoomEquipamentoDtos.Comando> comandosPendentes(@PathVariable String dispositivo) {
        return servico.buscarComandosPendentes(dispositivo);
    }

    @PostMapping("/dispositivos/{dispositivo}/comandos/{comandoId}/confirmacao")
    @PreAuthorize(DISPOSITIVO)
    public ControlRoomEquipamentoDtos.Comando confirmarComando(
            @PathVariable String dispositivo,
            @PathVariable Long comandoId,
            @Valid @RequestBody ControlRoomEquipamentoDtos.ConfirmacaoComandoRequisicao requisicao
    ) {
        return servico.confirmarComando(dispositivo, comandoId, requisicao);
    }

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @PreAuthorize(CONSULTA)
    public SseEmitter stream() {
        return streamingServico.assinar(servico.resumo());
    }

    private String usuario(Authentication authentication) {
        return authentication == null || authentication.getName() == null
                ? "sistema"
                : authentication.getName();
    }
}
