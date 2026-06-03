package br.com.cloudport.servicoyard.edi.servico;

import br.com.cloudport.servicoyard.edi.dto.BayPlanRespostaDto;
import br.com.cloudport.servicoyard.edi.modelo.BayPlan;
import br.com.cloudport.servicoyard.edi.modelo.BayPlanContainer;
import br.com.cloudport.servicoyard.edi.modelo.StatusBayPlan;
import br.com.cloudport.servicoyard.edi.repositorio.BayPlanContainerRepositorio;
import br.com.cloudport.servicoyard.edi.repositorio.BayPlanRepositorio;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Gerencia o ciclo de vida do Bay Plan:
 *   BAPLIE → cria BayPlan em RASCUNHO e o promove a ATIVO
 *   COPRAR → atualiza posições e containers pendentes → status ATUALIZADO
 *   COARRI → confirma conclusão das operações → status EM_OPERACAO ou CONCLUIDO
 */
@Service
public class BayPlanServico {

    private final BayPlanRepositorio bayPlanRepositorio;
    private final BayPlanContainerRepositorio containerRepositorio;
    private final BayPlanPublicadorServico publicador;

    public BayPlanServico(BayPlanRepositorio bayPlanRepositorio,
                          BayPlanContainerRepositorio containerRepositorio,
                          BayPlanPublicadorServico publicador) {
        this.bayPlanRepositorio = bayPlanRepositorio;
        this.containerRepositorio = containerRepositorio;
        this.publicador = publicador;
    }

    // ── BAPLIE ────────────────────────────────────────────────────────────────

    @Transactional
    public BayPlanRespostaDto processarBaplie(BayPlan bayPlanParsed) {
        Optional<BayPlan> existente = bayPlanRepositorio
                .findTopByCodigoNavioAndCodigoViagemOrderByAtualizadoEmDesc(
                        bayPlanParsed.getCodigoNavio(), bayPlanParsed.getCodigoViagem());

        BayPlan bayPlan;
        if (existente.isPresent()) {
            // Re-emissão do BAPLIE: substitui os containers e ativa
            bayPlan = existente.get();
            bayPlan.getContainers().clear();
            bayPlanParsed.getContainers().forEach(c -> {
                c.setBayPlan(bayPlan);
                bayPlan.getContainers().add(c);
            });
            bayPlan.setNomeNavio(bayPlanParsed.getNomeNavio());
            bayPlan.setPortoCarga(bayPlanParsed.getPortoCarga());
            bayPlan.setPortoDescarga(bayPlanParsed.getPortoDescarga());
        } else {
            bayPlan = bayPlanParsed;
        }
        bayPlan.setStatus(StatusBayPlan.ATIVO);

        BayPlan salvo = bayPlanRepositorio.save(bayPlan);
        publicador.publicarCriacaoBaplie(salvo);
        return BayPlanRespostaDto.deEntidade(salvo);
    }

    // ── COPRAR ────────────────────────────────────────────────────────────────

    @Transactional
    public BayPlanRespostaDto processarCoprar(String codigoNavio,
                                              String codigoViagem,
                                              List<BayPlanContainer> containersParsed) {
        BayPlan bayPlan = localizarOuCriarBayPlan(codigoNavio, codigoViagem, "COPRAR");

        List<String> adicionados = new ArrayList<>();
        List<String> atualizados = new ArrayList<>();

        for (BayPlanContainer novo : containersParsed) {
            Optional<BayPlanContainer> existente = bayPlan.getContainers().stream()
                    .filter(c -> c.getCodigoContainer().equals(novo.getCodigoContainer()))
                    .findFirst();

            if (existente.isPresent()) {
                // Atualiza posição e tipo de operação
                BayPlanContainer c = existente.get();
                c.setPosicaoBay(novo.getPosicaoBay());
                c.setTipoOperacao(novo.getTipoOperacao());
                c.setPortoDescarga(novo.getPortoDescarga());
                if (novo.getPesoKg() != null) c.setPesoKg(novo.getPesoKg());
                atualizados.add(novo.getCodigoContainer());
            } else {
                bayPlan.adicionarContainer(novo);
                adicionados.add(novo.getCodigoContainer());
            }
        }

        bayPlan.setStatus(StatusBayPlan.ATUALIZADO);
        BayPlan salvo = bayPlanRepositorio.save(bayPlan);
        publicador.publicarAtualizacaoCoprar(salvo, adicionados, atualizados);
        return BayPlanRespostaDto.deEntidade(salvo);
    }

    // ── COARRI ────────────────────────────────────────────────────────────────

    @Transactional
    public BayPlanRespostaDto processarCoarri(String codigoNavio,
                                              String codigoViagem,
                                              List<BayPlanContainer> containersParsed) {
        BayPlan bayPlan = localizarOuCriarBayPlan(codigoNavio, codigoViagem, "COARRI");

        List<String> confirmados = new ArrayList<>();

        for (BayPlanContainer confirmacao : containersParsed) {
            bayPlan.getContainers().stream()
                    .filter(c -> c.getCodigoContainer().equals(confirmacao.getCodigoContainer()))
                    .findFirst()
                    .ifPresent(c -> {
                        c.setStatusOperacao("CONCLUIDO");
                        if (confirmacao.getHorarioOperacao() != null)
                            c.setHorarioOperacao(confirmacao.getHorarioOperacao());
                        confirmados.add(c.getCodigoContainer());
                    });
        }

        // Promove para EM_OPERACAO na primeira COARRI, CONCLUIDO quando todos confirmados
        long pendentes = bayPlan.getContainers().stream()
                .filter(c -> !"CONCLUIDO".equals(c.getStatusOperacao())).count();
        bayPlan.setStatus(pendentes == 0 ? StatusBayPlan.CONCLUIDO : StatusBayPlan.EM_OPERACAO);

        BayPlan salvo = bayPlanRepositorio.save(bayPlan);
        publicador.publicarConfirmacaoCoarri(salvo, confirmados);
        return BayPlanRespostaDto.deEntidade(salvo);
    }

    // ── Consultas ─────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public BayPlanRespostaDto buscarPorId(Long id) {
        return bayPlanRepositorio.findById(id)
                .map(BayPlanRespostaDto::deEntidade)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Bay Plan não encontrado: " + id));
    }

    @Transactional(readOnly = true)
    public List<BayPlanRespostaDto> buscarPorNavio(String codigoNavio) {
        return bayPlanRepositorio.findByCodigoNavioOrderByAtualizadoEmDesc(codigoNavio)
                .stream()
                .map(BayPlanRespostaDto::deEntidade)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<BayPlanRespostaDto> listarAtivos() {
        return bayPlanRepositorio.findByStatusIn(
                List.of(StatusBayPlan.ATIVO, StatusBayPlan.EM_OPERACAO, StatusBayPlan.ATUALIZADO))
                .stream()
                .map(BayPlanRespostaDto::deEntidade)
                .collect(Collectors.toList());
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private BayPlan localizarOuCriarBayPlan(String codigoNavio, String codigoViagem,
                                             String origemMensagem) {
        return bayPlanRepositorio
                .findTopByCodigoNavioAndCodigoViagemOrderByAtualizadoEmDesc(codigoNavio, codigoViagem)
                .orElseGet(() -> {
                    BayPlan novo = new BayPlan();
                    novo.setCodigoNavio(codigoNavio);
                    novo.setCodigoViagem(codigoViagem);
                    novo.setStatus(StatusBayPlan.RASCUNHO);
                    novo.setOrigemMensagem(origemMensagem);
                    return novo;
                });
    }
}
