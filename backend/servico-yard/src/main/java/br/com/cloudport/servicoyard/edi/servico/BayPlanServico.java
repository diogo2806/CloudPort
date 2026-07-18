package br.com.cloudport.servicoyard.edi.servico;

import br.com.cloudport.servicoyard.edi.dto.BayPlanRespostaDto;
import br.com.cloudport.servicoyard.edi.modelo.BayPlan;
import br.com.cloudport.servicoyard.edi.modelo.BayPlanContainer;
import br.com.cloudport.servicoyard.edi.modelo.StatusBayPlan;
import br.com.cloudport.servicoyard.edi.parser.VermasParser.PesoVgm;
import br.com.cloudport.servicoyard.edi.repositorio.BayPlanRepositorio;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class BayPlanServico {

    private final BayPlanRepositorio bayPlanRepositorio;
    private final BayPlanPublicadorServico publicador;

    public BayPlanServico(BayPlanRepositorio bayPlanRepositorio,
                           br.com.cloudport.servicoyard.edi.repositorio.BayPlanContainerRepositorio containerRepositorio,
                           BayPlanPublicadorServico publicador) {
        this.bayPlanRepositorio = bayPlanRepositorio;
        this.publicador = publicador;
    }

    @Transactional
    public BayPlanRespostaDto processarBaplie(BayPlan bayPlanParsed) {
        Optional<BayPlan> existente = bayPlanRepositorio
                .findTopByCodigoNavioAndCodigoViagemOrderByAtualizadoEmDesc(
                        bayPlanParsed.getCodigoNavio(), bayPlanParsed.getCodigoViagem());

        BayPlan bayPlan;
        if (existente.isPresent()) {
            bayPlan = existente.get();
            bayPlan.getContainers().clear();
            bayPlanParsed.getContainers().forEach(container -> {
                container.setBayPlan(bayPlan);
                bayPlan.getContainers().add(container);
            });
            bayPlan.setNomeNavio(bayPlanParsed.getNomeNavio());
            bayPlan.setPortoCarga(bayPlanParsed.getPortoCarga());
            bayPlan.setPortoDescarga(bayPlanParsed.getPortoDescarga());
            bayPlan.setOrigemMensagem("BAPLIE");
        } else {
            bayPlan = bayPlanParsed;
        }
        bayPlan.setStatus(StatusBayPlan.ATIVO);

        BayPlan salvo = bayPlanRepositorio.save(bayPlan);
        publicador.publicarCriacaoBaplie(salvo);
        return BayPlanRespostaDto.deEntidade(salvo);
    }

    @Transactional
    public BayPlanRespostaDto processarCoprar(String codigoNavio,
                                               String codigoViagem,
                                               List<BayPlanContainer> containersParsed) {
        BayPlan bayPlan = localizarOuCriarBayPlan(codigoNavio, codigoViagem, "COPRAR");
        List<String> adicionados = new ArrayList<>();
        List<String> atualizados = new ArrayList<>();

        for (BayPlanContainer novo : containersParsed) {
            Optional<BayPlanContainer> existente = bayPlan.getContainers().stream()
                    .filter(container -> container.getCodigoContainer().equalsIgnoreCase(novo.getCodigoContainer()))
                    .findFirst();
            if (existente.isPresent()) {
                BayPlanContainer container = existente.get();
                container.setPosicaoBay(novo.getPosicaoBay());
                container.setTipoOperacao(novo.getTipoOperacao());
                container.setPortoDescarga(novo.getPortoDescarga());
                if (novo.getPesoKg() != null) {
                    container.setPesoKg(novo.getPesoKg());
                }
                atualizados.add(novo.getCodigoContainer());
            } else {
                bayPlan.adicionarContainer(novo);
                adicionados.add(novo.getCodigoContainer());
            }
        }

        if (adicionados.isEmpty() && atualizados.isEmpty()) {
            throw new IllegalArgumentException("COPRAR: nenhum container valido foi processado.");
        }
        bayPlan.setStatus(StatusBayPlan.ATUALIZADO);
        bayPlan.setOrigemMensagem("COPRAR");
        BayPlan salvo = bayPlanRepositorio.save(bayPlan);
        publicador.publicarAtualizacaoCoprar(salvo, adicionados, atualizados);
        return BayPlanRespostaDto.deEntidade(salvo);
    }

    @Transactional
    public BayPlanRespostaDto processarCoarri(String codigoNavio,
                                               String codigoViagem,
                                               List<BayPlanContainer> containersParsed) {
        BayPlan bayPlan = localizarExistente(codigoNavio, codigoViagem, "COARRI");
        List<String> confirmados = new ArrayList<>();

        for (BayPlanContainer confirmacao : containersParsed) {
            bayPlan.getContainers().stream()
                    .filter(container -> container.getCodigoContainer()
                            .equalsIgnoreCase(confirmacao.getCodigoContainer()))
                    .findFirst()
                    .ifPresent(container -> {
                        container.setStatusOperacao("CONCLUIDO");
                        if (confirmacao.getHorarioOperacao() != null) {
                            container.setHorarioOperacao(confirmacao.getHorarioOperacao());
                        }
                        if (confirmacao.getPosicaoBay() != null) {
                            container.setPosicaoExecucao(confirmacao.getPosicaoBay());
                        }
                        if (confirmacao.getPesoKg() != null) {
                            container.setPesoExecucaoKg(confirmacao.getPesoKg());
                        }
                        confirmados.add(container.getCodigoContainer());
                    });
        }
        if (confirmados.isEmpty()) {
            throw new IllegalArgumentException("COARRI: nenhum container corresponde ao Bay Plan informado.");
        }

        long pendentes = bayPlan.getContainers().stream()
                .filter(container -> !"CONCLUIDO".equals(container.getStatusOperacao()))
                .count();
        bayPlan.setStatus(pendentes == 0 ? StatusBayPlan.CONCLUIDO : StatusBayPlan.EM_OPERACAO);
        bayPlan.setOrigemMensagem("COARRI");

        BayPlan salvo = bayPlanRepositorio.save(bayPlan);
        publicador.publicarConfirmacaoCoarri(salvo, confirmados);
        return BayPlanRespostaDto.deEntidade(salvo);
    }

    @Transactional
    public BayPlanRespostaDto processarVermas(String codigoNavio,
                                               String codigoViagem,
                                               List<PesoVgm> pesos) {
        BayPlan bayPlan = localizarExistente(codigoNavio, codigoViagem, "VERMAS");
        List<String> atualizados = new ArrayList<>();
        List<String> naoEncontrados = new ArrayList<>();

        for (PesoVgm peso : pesos) {
            Optional<BayPlanContainer> container = bayPlan.getContainers().stream()
                    .filter(item -> item.getCodigoContainer().equalsIgnoreCase(peso.codigoContainer()))
                    .findFirst();
            if (container.isPresent()) {
                container.get().setPesoVgmKg(peso.pesoKg());
                container.get().setUnidadeVgmOriginal("KGM");
                container.get().setOrigemVgm("VERMAS");
                container.get().setStatusVgm("VERIFICADO");
                atualizados.add(container.get().getCodigoContainer());
            } else {
                naoEncontrados.add(peso.codigoContainer());
            }
        }
        if (atualizados.isEmpty()) {
            throw new IllegalArgumentException(
                    "VERMAS: nenhum container corresponde ao Bay Plan. Containers recebidos: "
                            + String.join(", ", naoEncontrados) + ".");
        }
        bayPlan.setOrigemMensagem("VERMAS");
        if (bayPlan.getStatus() == StatusBayPlan.RASCUNHO || bayPlan.getStatus() == StatusBayPlan.ATIVO) {
            bayPlan.setStatus(StatusBayPlan.ATUALIZADO);
        }
        BayPlan salvo = bayPlanRepositorio.save(bayPlan);
        publicador.publicarAtualizacaoVermas(salvo, atualizados);
        return BayPlanRespostaDto.deEntidade(salvo);
    }

    @Transactional(readOnly = true)
    public BayPlanRespostaDto buscarPorId(Long id) {
        return bayPlanRepositorio.findById(id)
                .map(BayPlanRespostaDto::deEntidade)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Bay Plan nao encontrado: " + id));
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

    private BayPlan localizarOuCriarBayPlan(String codigoNavio,
                                             String codigoViagem,
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

    private BayPlan localizarExistente(String codigoNavio,
                                        String codigoViagem,
                                        String tipoMensagem) {
        return bayPlanRepositorio
                .findTopByCodigoNavioAndCodigoViagemOrderByAtualizadoEmDesc(codigoNavio, codigoViagem)
                .orElseThrow(() -> new IllegalArgumentException(
                        tipoMensagem + ": Bay Plan nao encontrado para navio " + codigoNavio
                                + " e viagem " + codigoViagem + "."));
    }
}
