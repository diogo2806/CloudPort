package br.com.cloudport.servicogate.app.gestor;

import br.com.cloudport.servicogate.app.gestor.dto.GateFlowRequest;
import br.com.cloudport.servicogate.exception.BusinessException;
import br.com.cloudport.servicogate.exception.NotFoundException;
import br.com.cloudport.servicogate.model.GateCall;
import br.com.cloudport.servicogate.model.GatePass;
import br.com.cloudport.servicogate.model.GateQueueEntry;
import br.com.cloudport.servicogate.model.TruckHoppingSession;
import br.com.cloudport.servicogate.model.enums.GateCallPriority;
import br.com.cloudport.servicogate.model.enums.GateCallStatus;
import br.com.cloudport.servicogate.model.enums.GateQueueDirection;
import br.com.cloudport.servicogate.model.enums.GateQueuePriority;
import br.com.cloudport.servicogate.model.enums.GateQueueStatus;
import br.com.cloudport.servicogate.model.enums.TruckHoppingStatus;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Transactional
public class GateOperationsService {

    private static final EnumSet<GateCallStatus> CHAMADOS_ATIVOS =
            EnumSet.of(GateCallStatus.CHAMADO, GateCallStatus.EM_ATENDIMENTO);
    private static final EnumSet<GateQueueStatus> FILA_ATIVA =
            EnumSet.of(GateQueueStatus.AGUARDANDO, GateQueueStatus.CHAMADO, GateQueueStatus.EM_ATENDIMENTO);

    private final GatePassRepository gatePassRepository;
    private final TruckHoppingSessionRepository truckHoppingSessionRepository;
    private final GateCallRepository gateCallRepository;
    private final GateQueueEntryRepository gateQueueEntryRepository;

    public GateOperationsService(GatePassRepository gatePassRepository,
                                 TruckHoppingSessionRepository truckHoppingSessionRepository,
                                 GateCallRepository gateCallRepository,
                                 GateQueueEntryRepository gateQueueEntryRepository) {
        this.gatePassRepository = gatePassRepository;
        this.truckHoppingSessionRepository = truckHoppingSessionRepository;
        this.gateCallRepository = gateCallRepository;
        this.gateQueueEntryRepository = gateQueueEntryRepository;
    }

    public void registrarEntrada(GateFlowRequest request, Long gatePassId) {
        if (gatePassId == null) {
            return;
        }
        removerDaFila(gatePassId, GateQueueDirection.ENTRADA);
        garantirNaFila(gatePassId, GateQueueDirection.SAIDA);
        if (Boolean.TRUE.equals(request.getTrocaCavalo())) {
            abrirTroca(request.getCpfMotorista(), request.getNumeroCnh(), request.getPlaca(), gatePassId);
        }
    }

    public void registrarSaida(GateFlowRequest request, Long gatePassId) {
        if (gatePassId == null) {
            return;
        }
        removerDaFila(gatePassId, GateQueueDirection.SAIDA);
        if (Boolean.TRUE.equals(request.getTrocaCavalo())) {
            encerrarTroca(request.getCpfMotorista(), request.getPlaca(), gatePassId);
        }
    }

    public TruckHoppingSession abrirTroca(String cpf, String cnh, String cavalo, Long gatePassId) {
        String cpfNormalizado = obrigatorio(cpf, "CPF do motorista");
        String cnhNormalizada = obrigatorio(cnh, "Número da CNH");
        String cavaloNormalizado = obrigatorio(cavalo, "Placa do cavalo").toUpperCase(Locale.ROOT);
        if (truckHoppingSessionRepository.existsByCpfMotoristaAndStatus(cpfNormalizado, TruckHoppingStatus.ABERTA)) {
            throw new BusinessException("Já existe uma troca de cavalo aberta para o CPF informado");
        }
        TruckHoppingSession sessao = new TruckHoppingSession();
        sessao.setCpfMotorista(cpfNormalizado);
        sessao.setNumeroCnh(cnhNormalizada);
        sessao.setCavaloAtual(cavaloNormalizado);
        sessao.setStatus(TruckHoppingStatus.ABERTA);
        sessao.setGateIn(obterGatePass(gatePassId));
        return truckHoppingSessionRepository.save(sessao);
    }

    public TruckHoppingSession encerrarTroca(String cpf, String cavalo, Long gatePassId) {
        String cpfNormalizado = obrigatorio(cpf, "CPF do motorista");
        TruckHoppingSession sessao = truckHoppingSessionRepository
                .findFirstByCpfMotoristaAndStatusOrderByCreatedAtDesc(cpfNormalizado, TruckHoppingStatus.ABERTA)
                .orElseThrow(() -> new NotFoundException("Não existe troca de cavalo aberta para o CPF informado"));
        if (StringUtils.hasText(cavalo)) {
            sessao.setCavaloAtual(cavalo.trim().toUpperCase(Locale.ROOT));
        }
        sessao.setGateOut(obterGatePass(gatePassId));
        sessao.setStatus(TruckHoppingStatus.ENCERRADA);
        sessao.setEncerradaEm(LocalDateTime.now());
        return truckHoppingSessionRepository.save(sessao);
    }

    @Transactional(readOnly = true)
    public List<TruckHoppingSession> listarTrocas() {
        return truckHoppingSessionRepository.findAllByOrderByCreatedAtDesc();
    }

    public GateCall chamarVeiculo(Long gatePassId, GateCallPriority prioridade, String operador) {
        if (gateCallRepository.findFirstByGatePassIdAndStatusIn(gatePassId, CHAMADOS_ATIVOS).isPresent()) {
            throw new BusinessException("Já existe um chamado ativo para o GatePass informado");
        }
        GateCall chamado = new GateCall();
        chamado.setGatePass(obterGatePass(gatePassId));
        chamado.setStatus(GateCallStatus.CHAMADO);
        chamado.setPrioridade(prioridade != null ? prioridade : GateCallPriority.NORMAL);
        chamado.setChamadoEm(LocalDateTime.now());
        chamado.setOperador(normalizarOpcional(operador));
        atualizarFilaPeloChamado(gatePassId, GateQueueStatus.CHAMADO);
        return gateCallRepository.save(chamado);
    }

    public GateCall iniciarAtendimento(Long chamadoId) {
        GateCall chamado = obterChamado(chamadoId);
        exigirStatus(chamado, GateCallStatus.CHAMADO);
        chamado.setStatus(GateCallStatus.EM_ATENDIMENTO);
        chamado.setAtendimentoIniciadoEm(LocalDateTime.now());
        atualizarFilaPeloChamado(chamado.getGatePass().getId(), GateQueueStatus.EM_ATENDIMENTO);
        return gateCallRepository.save(chamado);
    }

    public GateCall finalizarAtendimento(Long chamadoId) {
        GateCall chamado = obterChamado(chamadoId);
        exigirStatus(chamado, GateCallStatus.EM_ATENDIMENTO);
        chamado.setStatus(GateCallStatus.FINALIZADO);
        chamado.setFinalizadoEm(LocalDateTime.now());
        removerFilasAtivas(chamado.getGatePass().getId());
        return gateCallRepository.save(chamado);
    }

    public GateCall cancelar(Long chamadoId, String justificativa) {
        GateCall chamado = obterChamado(chamadoId);
        if (!CHAMADOS_ATIVOS.contains(chamado.getStatus())) {
            throw new BusinessException("Somente chamados ativos podem ser cancelados");
        }
        chamado.setJustificativaCancelamento(obrigatorio(justificativa, "Justificativa do cancelamento"));
        chamado.setStatus(GateCallStatus.CANCELADO);
        chamado.setCanceladoEm(LocalDateTime.now());
        return gateCallRepository.save(chamado);
    }

    @Transactional(readOnly = true)
    public List<GateCall> listarChamados() {
        return gateCallRepository.findAllByOrderByChamadoEmDesc();
    }

    public GateQueueEntry adicionarNaFila(Long gatePassId, GateQueueDirection sentido) {
        return garantirNaFila(gatePassId, sentido != null ? sentido : GateQueueDirection.ENTRADA);
    }

    public GateQueueEntry reordenar(Long entradaId, Integer novaPosicao, String justificativa, String operador) {
        if (novaPosicao == null || novaPosicao < 1) {
            throw new BusinessException("A nova posição deve ser maior que zero");
        }
        GateQueueEntry entrada = obterEntradaFila(entradaId);
        entrada.setPosicaoAtual(novaPosicao);
        entrada.setJustificativaPrioridade(obrigatorio(justificativa, "Justificativa da reordenação"));
        entrada.setOperadorPrioridade(normalizarOpcional(operador));
        return gateQueueEntryRepository.save(entrada);
    }

    public GateQueueEntry alterarPrioridade(Long entradaId, GateQueuePriority prioridade,
                                            String justificativa, String operador) {
        GateQueueEntry entrada = obterEntradaFila(entradaId);
        GateQueuePriority novaPrioridade = prioridade != null ? prioridade : GateQueuePriority.NORMAL;
        if (novaPrioridade != GateQueuePriority.NORMAL) {
            entrada.setJustificativaPrioridade(obrigatorio(justificativa, "Justificativa da prioridade"));
            entrada.setOperadorPrioridade(normalizarOpcional(operador));
        }
        entrada.setPrioridade(novaPrioridade);
        return gateQueueEntryRepository.save(entrada);
    }

    @Transactional(readOnly = true)
    public List<GateQueueEntry> listarFila(GateQueueDirection sentido) {
        GateQueueDirection filtro = sentido != null ? sentido : GateQueueDirection.ENTRADA;
        return gateQueueEntryRepository.findBySentidoAndStatusIn(filtro, FILA_ATIVA).stream()
                .sorted(Comparator.comparingInt((GateQueueEntry item) -> peso(item.getPrioridade())).reversed()
                        .thenComparing(GateQueueEntry::getPosicaoAtual)
                        .thenComparing(GateQueueEntry::getEntrouEm))
                .collect(Collectors.toList());
    }

    private GateQueueEntry garantirNaFila(Long gatePassId, GateQueueDirection sentido) {
        return gateQueueEntryRepository.findFirstByGatePassIdAndSentidoAndStatusIn(gatePassId, sentido, FILA_ATIVA)
                .orElseGet(() -> {
                    long quantidade = gateQueueEntryRepository.countBySentidoAndStatusIn(sentido, FILA_ATIVA);
                    GateQueueEntry entrada = new GateQueueEntry();
                    entrada.setGatePass(obterGatePass(gatePassId));
                    entrada.setSentido(sentido);
                    entrada.setStatus(GateQueueStatus.AGUARDANDO);
                    entrada.setPosicaoOriginal(Math.toIntExact(quantidade + 1));
                    entrada.setPosicaoAtual(Math.toIntExact(quantidade + 1));
                    entrada.setPrioridade(GateQueuePriority.NORMAL);
                    entrada.setEntrouEm(LocalDateTime.now());
                    return gateQueueEntryRepository.save(entrada);
                });
    }

    private void atualizarFilaPeloChamado(Long gatePassId, GateQueueStatus status) {
        Arrays.stream(GateQueueDirection.values())
                .map(sentido -> gateQueueEntryRepository
                        .findFirstByGatePassIdAndSentidoAndStatusIn(gatePassId, sentido, FILA_ATIVA).orElse(null))
                .filter(item -> item != null)
                .findFirst()
                .ifPresent(item -> {
                    item.setStatus(status);
                    if (status == GateQueueStatus.CHAMADO) {
                        item.setChamadoEm(LocalDateTime.now());
                    } else if (status == GateQueueStatus.EM_ATENDIMENTO) {
                        item.setAtendimentoIniciadoEm(LocalDateTime.now());
                    }
                    gateQueueEntryRepository.save(item);
                });
    }

    private void removerDaFila(Long gatePassId, GateQueueDirection sentido) {
        gateQueueEntryRepository.findFirstByGatePassIdAndSentidoAndStatusIn(gatePassId, sentido, FILA_ATIVA)
                .ifPresent(gateQueueEntryRepository::delete);
    }

    private void removerFilasAtivas(Long gatePassId) {
        Arrays.stream(GateQueueDirection.values()).forEach(sentido -> removerDaFila(gatePassId, sentido));
    }

    private GatePass obterGatePass(Long id) {
        if (id == null) {
            throw new BusinessException("GatePass deve ser informado");
        }
        return gatePassRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("GatePass não encontrado"));
    }

    private GateCall obterChamado(Long id) {
        return gateCallRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Chamado de gate não encontrado"));
    }

    private GateQueueEntry obterEntradaFila(Long id) {
        return gateQueueEntryRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Item de fila não encontrado"));
    }

    private void exigirStatus(GateCall chamado, GateCallStatus esperado) {
        if (chamado.getStatus() != esperado) {
            throw new BusinessException("Transição inválida para chamado no status " + chamado.getStatus());
        }
    }

    private int peso(GateQueuePriority prioridade) {
        if (prioridade == GateQueuePriority.EMERGENCIAL) {
            return 3;
        }
        if (prioridade == GateQueuePriority.ALTA) {
            return 2;
        }
        return 1;
    }

    private String obrigatorio(String valor, String campo) {
        if (!StringUtils.hasText(valor)) {
            throw new BusinessException(campo + " deve ser informado");
        }
        return valor.trim();
    }

    private String normalizarOpcional(String valor) {
        return StringUtils.hasText(valor) ? valor.trim() : null;
    }
}
