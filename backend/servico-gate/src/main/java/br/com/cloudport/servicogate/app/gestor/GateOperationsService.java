package br.com.cloudport.servicogate.app.gestor;

import br.com.cloudport.servicogate.app.gestor.dto.GateFlowRequest;
import br.com.cloudport.servicogate.exception.BusinessException;
import br.com.cloudport.servicogate.exception.NotFoundException;
import br.com.cloudport.servicogate.model.GateCall;
import br.com.cloudport.servicogate.model.GatePass;
import br.com.cloudport.servicogate.model.GateQueueEntry;
import br.com.cloudport.servicogate.model.enums.GateCallPriority;
import br.com.cloudport.servicogate.model.enums.GateCallStatus;
import br.com.cloudport.servicogate.model.enums.GateQueueDirection;
import br.com.cloudport.servicogate.model.enums.GateQueuePriority;
import br.com.cloudport.servicogate.model.enums.GateQueueStatus;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Transactional
public class GateOperationsService {

    private static final int VALIDADE_PADRAO_MINUTOS = 5;
    private static final EnumSet<GateCallStatus> CHAMADOS_ATIVOS =
            EnumSet.of(GateCallStatus.CHAMADO, GateCallStatus.ACEITO, GateCallStatus.EM_ATENDIMENTO);
    private static final EnumSet<GateQueueStatus> FILA_ATIVA =
            EnumSet.of(GateQueueStatus.AGUARDANDO, GateQueueStatus.CHAMADO, GateQueueStatus.EM_ATENDIMENTO);

    private final GatePassRepository gatePassRepository;
    private final GateCallRepository gateCallRepository;
    private final GateQueueEntryRepository gateQueueEntryRepository;

    public GateOperationsService(GatePassRepository gatePassRepository,
                                 GateCallRepository gateCallRepository,
                                 GateQueueEntryRepository gateQueueEntryRepository) {
        this.gatePassRepository = gatePassRepository;
        this.gateCallRepository = gateCallRepository;
        this.gateQueueEntryRepository = gateQueueEntryRepository;
    }

    public void registrarEntrada(GateFlowRequest request, Long gatePassId) {
        if (gatePassId == null) {
            return;
        }
        removerDaFila(gatePassId, GateQueueDirection.ENTRADA);
        garantirNaFila(gatePassId, GateQueueDirection.SAIDA);
    }

    public void registrarSaida(GateFlowRequest request, Long gatePassId) {
        if (gatePassId != null) {
            removerDaFila(gatePassId, GateQueueDirection.SAIDA);
        }
    }

    public GateCall chamarVeiculo(Long gatePassId,
                                  GateCallPriority prioridade,
                                  String gatePista,
                                  Integer validadeMinutos,
                                  String operador) {
        expirarChamadasVencidas();
        if (gateCallRepository.findFirstByGatePassIdAndStatusIn(gatePassId, CHAMADOS_ATIVOS).isPresent()) {
            throw new BusinessException("Já existe um chamado ativo para o GatePass informado");
        }
        GateQueueEntry entradaFila = obterFilaAtiva(gatePassId);
        LocalDateTime agora = LocalDateTime.now();
        GateCall chamado = new GateCall();
        chamado.setGatePass(obterGatePass(gatePassId));
        chamado.setStatus(GateCallStatus.CHAMADO);
        chamado.setPrioridade(prioridade != null ? prioridade : GateCallPriority.NORMAL);
        chamado.setPosicaoFila(entradaFila.getPosicaoAtual());
        chamado.setGatePista(obrigatorio(gatePista, "Gate/pista"));
        chamado.setChamadoEm(agora);
        chamado.setExpiraEm(agora.plusMinutes(validade(validadeMinutos)));
        chamado.setQuantidadeRechamadas(0);
        chamado.setOperador(normalizarOpcional(operador));
        atualizarFilaPeloChamado(gatePassId, GateQueueStatus.CHAMADO);
        return gateCallRepository.save(chamado);
    }

    public GateCall aceitarChamado(Long chamadoId) {
        GateCall chamado = obterChamado(chamadoId);
        expirarSeVencido(chamado);
        exigirStatus(chamado, GateCallStatus.CHAMADO);
        chamado.setStatus(GateCallStatus.ACEITO);
        chamado.setAceitoEm(LocalDateTime.now());
        return gateCallRepository.save(chamado);
    }

    public GateCall iniciarAtendimento(Long chamadoId) {
        GateCall chamado = obterChamado(chamadoId);
        exigirStatus(chamado, GateCallStatus.ACEITO);
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

    public GateCall expirarChamado(Long chamadoId) {
        GateCall chamado = obterChamado(chamadoId);
        exigirStatus(chamado, GateCallStatus.CHAMADO);
        aplicarExpiracao(chamado, LocalDateTime.now());
        return gateCallRepository.save(chamado);
    }

    public GateCall rechamar(Long chamadoId, String gatePista, Integer validadeMinutos) {
        GateCall chamado = obterChamado(chamadoId);
        if (chamado.getStatus() != GateCallStatus.EXPIRADO) {
            throw new BusinessException("Somente chamadas expiradas podem ser rechamadas");
        }
        LocalDateTime agora = LocalDateTime.now();
        chamado.setStatus(GateCallStatus.CHAMADO);
        chamado.setChamadoEm(agora);
        chamado.setAceitoEm(null);
        chamado.setExpiradoEm(null);
        chamado.setExpiraEm(agora.plusMinutes(validade(validadeMinutos)));
        chamado.setGatePista(StringUtils.hasText(gatePista) ? gatePista.trim() : chamado.getGatePista());
        chamado.setQuantidadeRechamadas(chamado.getQuantidadeRechamadas() + 1);
        chamado.setUltimaRechamadaEm(agora);
        atualizarFilaPeloChamado(chamado.getGatePass().getId(), GateQueueStatus.CHAMADO);
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
        restaurarFilaAguardando(chamado.getGatePass().getId());
        return gateCallRepository.save(chamado);
    }

    public List<GateCall> listarChamados() {
        expirarChamadasVencidas();
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
        List<GateQueueEntry> itens = gateQueueEntryRepository
                .findBySentidoAndStatusIn(entrada.getSentido(), FILA_ATIVA);
        if (novaPosicao > itens.size()) {
            throw new BusinessException("A nova posição não pode ser maior que o tamanho da fila");
        }
        int posicaoAnterior = entrada.getPosicaoAtual();
        for (GateQueueEntry item : itens) {
            if (item.getId().equals(entrada.getId())) {
                continue;
            }
            int posicaoItem = item.getPosicaoAtual();
            if (novaPosicao < posicaoAnterior && posicaoItem >= novaPosicao && posicaoItem < posicaoAnterior) {
                item.setPosicaoAtual(posicaoItem + 1);
            } else if (novaPosicao > posicaoAnterior && posicaoItem > posicaoAnterior && posicaoItem <= novaPosicao) {
                item.setPosicaoAtual(posicaoItem - 1);
            }
        }
        gateQueueEntryRepository.saveAll(itens);
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
        } else {
            entrada.setJustificativaPrioridade(null);
            entrada.setOperadorPrioridade(null);
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

    private void expirarChamadasVencidas() {
        LocalDateTime agora = LocalDateTime.now();
        gateCallRepository.findAllByOrderByChamadoEmDesc().stream()
                .filter(chamado -> chamado.getStatus() == GateCallStatus.CHAMADO)
                .filter(chamado -> chamado.getExpiraEm() != null && !chamado.getExpiraEm().isAfter(agora))
                .forEach(chamado -> aplicarExpiracao(chamado, agora));
    }

    private void expirarSeVencido(GateCall chamado) {
        if (chamado.getStatus() == GateCallStatus.CHAMADO
                && chamado.getExpiraEm() != null
                && !chamado.getExpiraEm().isAfter(LocalDateTime.now())) {
            aplicarExpiracao(chamado, LocalDateTime.now());
            gateCallRepository.save(chamado);
            throw new BusinessException("A chamada expirou e precisa ser refeita");
        }
    }

    private void aplicarExpiracao(GateCall chamado, LocalDateTime timestamp) {
        chamado.setStatus(GateCallStatus.EXPIRADO);
        chamado.setExpiradoEm(timestamp);
        restaurarFilaAguardando(chamado.getGatePass().getId());
        gateCallRepository.save(chamado);
    }

    private GateQueueEntry garantirNaFila(Long gatePassId, GateQueueDirection sentido) {
        return gateQueueEntryRepository.findFirstByGatePassIdAndSentidoAndStatusIn(gatePassId, sentido, FILA_ATIVA)
                .orElseGet(() -> {
                    int proximaPosicao = gateQueueEntryRepository.findBySentidoAndStatusIn(sentido, FILA_ATIVA).stream()
                            .map(GateQueueEntry::getPosicaoAtual)
                            .max(Integer::compareTo)
                            .orElse(0) + 1;
                    GateQueueEntry entrada = new GateQueueEntry();
                    entrada.setGatePass(obterGatePass(gatePassId));
                    entrada.setSentido(sentido);
                    entrada.setStatus(GateQueueStatus.AGUARDANDO);
                    entrada.setPosicaoOriginal(proximaPosicao);
                    entrada.setPosicaoAtual(proximaPosicao);
                    entrada.setPrioridade(GateQueuePriority.NORMAL);
                    entrada.setEntrouEm(LocalDateTime.now());
                    return gateQueueEntryRepository.save(entrada);
                });
    }

    private GateQueueEntry obterFilaAtiva(Long gatePassId) {
        return Arrays.stream(GateQueueDirection.values())
                .map(sentido -> gateQueueEntryRepository
                        .findFirstByGatePassIdAndSentidoAndStatusIn(gatePassId, sentido, FILA_ATIVA).orElse(null))
                .filter(item -> item != null)
                .findFirst()
                .orElseThrow(() -> new BusinessException("GatePass não está em uma fila ativa"));
    }

    private void atualizarFilaPeloChamado(Long gatePassId, GateQueueStatus status) {
        GateQueueEntry item = obterFilaAtiva(gatePassId);
        item.setStatus(status);
        if (status == GateQueueStatus.CHAMADO) {
            item.setChamadoEm(LocalDateTime.now());
        } else if (status == GateQueueStatus.EM_ATENDIMENTO) {
            item.setAtendimentoIniciadoEm(LocalDateTime.now());
        }
        gateQueueEntryRepository.save(item);
    }

    private void restaurarFilaAguardando(Long gatePassId) {
        Arrays.stream(GateQueueDirection.values())
                .map(sentido -> gateQueueEntryRepository
                        .findFirstByGatePassIdAndSentidoAndStatusIn(gatePassId, sentido, FILA_ATIVA).orElse(null))
                .filter(item -> item != null)
                .forEach(item -> {
                    item.setStatus(GateQueueStatus.AGUARDANDO);
                    item.setChamadoEm(null);
                    item.setAtendimentoIniciadoEm(null);
                    gateQueueEntryRepository.save(item);
                });
    }

    private void removerDaFila(Long gatePassId, GateQueueDirection sentido) {
        gateQueueEntryRepository.findFirstByGatePassIdAndSentidoAndStatusIn(gatePassId, sentido, FILA_ATIVA)
                .ifPresent(entrada -> {
                    int posicaoRemovida = entrada.getPosicaoAtual();
                    List<GateQueueEntry> itens = gateQueueEntryRepository.findBySentidoAndStatusIn(sentido, FILA_ATIVA);
                    List<GateQueueEntry> restantes = itens.stream()
                            .filter(item -> !item.getId().equals(entrada.getId()))
                            .collect(Collectors.toList());
                    restantes.stream()
                            .filter(item -> item.getPosicaoAtual() > posicaoRemovida)
                            .forEach(item -> item.setPosicaoAtual(item.getPosicaoAtual() - 1));
                    gateQueueEntryRepository.saveAll(restantes);
                    gateQueueEntryRepository.delete(entrada);
                });
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

    private int validade(Integer validadeMinutos) {
        if (validadeMinutos == null) {
            return VALIDADE_PADRAO_MINUTOS;
        }
        if (validadeMinutos < 1 || validadeMinutos > 60) {
            throw new BusinessException("A validade da chamada deve estar entre 1 e 60 minutos");
        }
        return validadeMinutos;
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
