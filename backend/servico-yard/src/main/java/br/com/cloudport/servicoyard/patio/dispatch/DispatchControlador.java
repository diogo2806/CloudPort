package br.com.cloudport.servicoyard.patio.dispatch;

import br.com.cloudport.servicoyard.patio.dispatch.DispatchDtos.AutoDispatchRequest;
import br.com.cloudport.servicoyard.patio.dispatch.DispatchDtos.Configuracao;
import br.com.cloudport.servicoyard.patio.dispatch.DispatchDtos.ConfiguracaoRequest;
import br.com.cloudport.servicoyard.patio.dispatch.DispatchDtos.Decisao;
import br.com.cloudport.servicoyard.patio.dispatch.DispatchDtos.Etapa;
import br.com.cloudport.servicoyard.patio.dispatch.DispatchDtos.EtapaRequest;
import br.com.cloudport.servicoyard.patio.dispatch.DispatchDtos.Ranking;
import br.com.cloudport.servicoyard.patio.dispatch.DispatchDtos.Resumo;
import br.com.cloudport.servicoyard.patio.dispatch.DispatchEnums.TipoEtapa;
import java.util.List;
import javax.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.util.StringUtils;

@RestController
public class DispatchControlador {

    private static final String LEITURA =
            "hasAnyRole('ADMIN_PORTO','PLANEJADOR','OPERADOR_PATIO','SERVICE_NAVIO')";
    private static final String OPERACAO =
            "hasAnyRole('ADMIN_PORTO','PLANEJADOR','OPERADOR_PATIO')";
    private static final String ADMINISTRACAO =
            "hasAnyRole('ADMIN_PORTO','PLANEJADOR')";

    private final DispatchDinamicoServico dispatchServico;
    private final ConfiguracaoDispatchServico configuracaoServico;
    private final EtapaWorkInstructionServico etapaServico;

    public DispatchControlador(
            DispatchDinamicoServico dispatchServico,
            ConfiguracaoDispatchServico configuracaoServico,
            EtapaWorkInstructionServico etapaServico) {
        this.dispatchServico = dispatchServico;
        this.configuracaoServico = configuracaoServico;
        this.etapaServico = etapaServico;
    }

    @GetMapping("/api/dispatch/resumo")
    @PreAuthorize(LEITURA)
    public Resumo resumo() {
        return dispatchServico.resumo();
    }

    @GetMapping("/api/dispatch/decisoes")
    @PreAuthorize(LEITURA)
    public List<Decisao> listarDecisoes(
            @RequestParam(name = "limite", defaultValue = "50") int limite) {
        return dispatchServico.listarDecisoes(limite);
    }

    @GetMapping("/api/dispatch/work-queues/{id}/ranking")
    @PreAuthorize(LEITURA)
    public List<Ranking> ranking(
            @PathVariable Long id,
            @RequestParam(name = "equipamentoId", required = false) Long equipamentoId) {
        return dispatchServico.ranking(id, equipamentoId);
    }

    @PostMapping({
            "/api/dispatch/auto-dispatch",
            "/yard/patio/work-instructions/auto-dispatch"
    })
    @PreAuthorize(OPERACAO)
    public Decisao autoDispatch(@Valid @RequestBody AutoDispatchRequest request) {
        return dispatchServico.autoDispatch(request);
    }

    @GetMapping("/api/dispatch/work-instructions/{ordemId}/etapas")
    @PreAuthorize(LEITURA)
    public List<Etapa> listarEtapas(@PathVariable Long ordemId) {
        return etapaServico.listar(ordemId);
    }

    @PostMapping("/api/dispatch/work-instructions/{ordemId}/etapas/{tipo}")
    @PreAuthorize(OPERACAO)
    public Etapa avancarEtapa(
            @PathVariable Long ordemId,
            @PathVariable TipoEtapa tipo,
            @Valid @RequestBody EtapaRequest request) {
        return etapaServico.avancar(ordemId, tipo, request);
    }

    @GetMapping("/api/dispatch/configuracoes")
    @PreAuthorize(LEITURA)
    public List<Configuracao> listarConfiguracoes() {
        return configuracaoServico.listar();
    }

    @PostMapping("/api/dispatch/configuracoes")
    @PreAuthorize(ADMINISTRACAO)
    public Configuracao criarConfiguracao(
            @Valid @RequestBody ConfiguracaoRequest request,
            Authentication authentication) {
        return configuracaoServico.criar(request, usuario(authentication));
    }

    @PostMapping("/api/dispatch/configuracoes/{id}/ativar")
    @PreAuthorize(ADMINISTRACAO)
    public Configuracao ativarConfiguracao(
            @PathVariable Long id,
            Authentication authentication) {
        return configuracaoServico.ativar(id, usuario(authentication));
    }

    @PostMapping("/api/dispatch/configuracoes/{id}/rollback")
    @PreAuthorize(ADMINISTRACAO)
    public Configuracao rollbackConfiguracao(
            @PathVariable Long id,
            @RequestParam(name = "motivo") String motivo,
            Authentication authentication) {
        return configuracaoServico.rollback(id, usuario(authentication), motivo);
    }

    private String usuario(Authentication authentication) {
        return authentication != null && StringUtils.hasText(authentication.getName())
                ? authentication.getName()
                : "sistema";
    }
}
