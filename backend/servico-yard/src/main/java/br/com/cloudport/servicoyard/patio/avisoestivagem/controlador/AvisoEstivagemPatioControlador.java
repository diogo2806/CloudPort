package br.com.cloudport.servicoyard.patio.avisoestivagem.controlador;

import br.com.cloudport.servicoyard.patio.avisoestivagem.dto.AvisoEstivagemPatioDtos.AtribuicaoRequisicao;
import br.com.cloudport.servicoyard.patio.avisoestivagem.dto.AvisoEstivagemPatioDtos.AvisoResposta;
import br.com.cloudport.servicoyard.patio.avisoestivagem.dto.AvisoEstivagemPatioDtos.CorrecaoRequisicao;
import br.com.cloudport.servicoyard.patio.avisoestivagem.dto.AvisoEstivagemPatioDtos.HistoricoResposta;
import br.com.cloudport.servicoyard.patio.avisoestivagem.dto.AvisoEstivagemPatioDtos.RevalidacaoRequisicao;
import br.com.cloudport.servicoyard.patio.avisoestivagem.modelo.AvisoEstivagemPatioEnums.SeveridadeAvisoEstivagemPatio;
import br.com.cloudport.servicoyard.patio.avisoestivagem.modelo.AvisoEstivagemPatioEnums.StatusAvisoEstivagemPatio;
import br.com.cloudport.servicoyard.patio.avisoestivagem.servico.AvisoEstivagemPatioServico;
import java.util.List;
import javax.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/yard/patio/avisos-estivagem")
public class AvisoEstivagemPatioControlador {

    private static final String LEITURA =
            "hasAnyRole('ADMIN_PORTO','PLANEJADOR','OPERADOR_PATIO','SERVICE_NAVIO')";
    private static final String OPERACAO =
            "hasAnyRole('ADMIN_PORTO','PLANEJADOR','OPERADOR_PATIO')";

    private final AvisoEstivagemPatioServico servico;

    public AvisoEstivagemPatioControlador(AvisoEstivagemPatioServico servico) {
        this.servico = servico;
    }

    @GetMapping
    @PreAuthorize(LEITURA)
    public List<AvisoResposta> listar(
            @RequestParam(name = "status", required = false) StatusAvisoEstivagemPatio status,
            @RequestParam(name = "severidade", required = false) SeveridadeAvisoEstivagemPatio severidade,
            @RequestParam(name = "responsavel", required = false) String responsavel,
            @RequestParam(name = "bloco", required = false) String bloco,
            @RequestParam(name = "codigoUnidade", required = false) String codigoUnidade) {
        return servico.listar(status, severidade, responsavel, bloco, codigoUnidade);
    }

    @GetMapping("/{id}")
    @PreAuthorize(LEITURA)
    public AvisoResposta buscar(@PathVariable Long id) {
        return servico.buscar(id);
    }

    @GetMapping("/{id}/historico")
    @PreAuthorize(LEITURA)
    public List<HistoricoResposta> listarHistorico(@PathVariable Long id) {
        return servico.listarHistorico(id);
    }

    @PostMapping("/revalidar")
    @PreAuthorize(OPERACAO)
    public List<AvisoResposta> revalidarInventario(Authentication authentication) {
        return servico.revalidarInventario(usuario(authentication));
    }

    @PostMapping("/{id}/atribuir")
    @PreAuthorize(OPERACAO)
    public AvisoResposta atribuir(
            @PathVariable Long id,
            @Valid @RequestBody AtribuicaoRequisicao requisicao,
            Authentication authentication) {
        return servico.atribuir(id, requisicao, usuario(authentication));
    }

    @PostMapping("/{id}/iniciar-correcao")
    @PreAuthorize(OPERACAO)
    public AvisoResposta iniciarCorrecao(
            @PathVariable Long id,
            @Valid @RequestBody CorrecaoRequisicao requisicao,
            Authentication authentication) {
        return servico.iniciarCorrecao(id, requisicao, usuario(authentication));
    }

    @PostMapping("/{id}/aguardar-revalidacao")
    @PreAuthorize(OPERACAO)
    public AvisoResposta aguardarRevalidacao(
            @PathVariable Long id,
            @RequestBody(required = false) RevalidacaoRequisicao requisicao,
            Authentication authentication) {
        return servico.aguardarRevalidacao(id, requisicao, usuario(authentication));
    }

    @PostMapping("/{id}/revalidar")
    @PreAuthorize(OPERACAO)
    public AvisoResposta revalidar(
            @PathVariable Long id,
            @RequestBody(required = false) RevalidacaoRequisicao requisicao,
            Authentication authentication) {
        return servico.revalidar(id, requisicao, usuario(authentication));
    }

    private String usuario(Authentication authentication) {
        return authentication != null && StringUtils.hasText(authentication.getName())
                ? authentication.getName()
                : "SISTEMA";
    }
}
