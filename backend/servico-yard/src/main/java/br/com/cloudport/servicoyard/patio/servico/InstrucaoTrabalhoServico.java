package br.com.cloudport.servicoyard.patio.servico;

import br.com.cloudport.servicoyard.patio.dto.ConteinerPatioRequisicaoDto;
import br.com.cloudport.servicoyard.patio.modelo.ConteinerPatio;
import br.com.cloudport.servicoyard.patio.modelo.EquipamentoPatio;
import br.com.cloudport.servicoyard.patio.modelo.InstrucaoTrabalho;
import br.com.cloudport.servicoyard.patio.modelo.PrioridadeInstrucao;
import br.com.cloudport.servicoyard.patio.modelo.StatusEquipamento;
import br.com.cloudport.servicoyard.patio.modelo.StatusInstrucao;
import br.com.cloudport.servicoyard.patio.modelo.TipoOperacaoInstrucao;
import br.com.cloudport.servicoyard.patio.repositorio.ConteinerPatioRepositorio;
import br.com.cloudport.servicoyard.patio.repositorio.EquipamentoPatioRepositorio;
import br.com.cloudport.servicoyard.patio.repositorio.InstrucaoTrabalhoRepositorio;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Transactional
public class InstrucaoTrabalhoServico {

    private static final EnumSet<StatusInstrucao> STATUS_ATIVOS =
            EnumSet.of(StatusInstrucao.PENDENTE, StatusInstrucao.EM_EXECUCAO);
    private static final Pattern POSICAO_PATTERN = Pattern.compile("(\\d+)\\D+(\\d+)");

    private final InstrucaoTrabalhoRepositorio instrucaoRepositorio;
    private final ConteinerPatioRepositorio conteinerRepositorio;
    private final EquipamentoPatioRepositorio equipamentoRepositorio;
    private final ValidadorYardPlacementService validadorPlacement;

    public InstrucaoTrabalhoServico(InstrucaoTrabalhoRepositorio instrucaoRepositorio,
                                    ConteinerPatioRepositorio conteinerRepositorio,
                                    EquipamentoPatioRepositorio equipamentoRepositorio,
                                    ValidadorYardPlacementService validadorPlacement) {
        this.instrucaoRepositorio = instrucaoRepositorio;
        this.conteinerRepositorio = conteinerRepositorio;
        this.equipamentoRepositorio = equipamentoRepositorio;
        this.validadorPlacement = validadorPlacement;
    }

    public InstrucaoTrabalho criar(String codigoConteiner,
                                    TipoOperacaoInstrucao tipoOperacao,
                                    String origem,
                                    String destino,
                                    PrioridadeInstrucao prioridade,
                                    LocalDateTime agendadaEm,
                                    String equipamento,
                                    String equipe,
                                    String observacoes,
                                    String criadoPor) {
        String codigo = obrigatorio(codigoConteiner, "Código do contêiner").toUpperCase(Locale.ROOT);
        if (tipoOperacao == null) {
            throw new IllegalArgumentException("Tipo da operação deve ser informado");
        }
        if (tipoOperacao == TipoOperacaoInstrucao.MOVIMENTACAO) {
            validarMovimentacao(codigo, destino);
        }

        InstrucaoTrabalho instrucao = new InstrucaoTrabalho();
        instrucao.setCodigoConteiner(codigo);
        instrucao.setTipoOperacao(tipoOperacao);
        instrucao.setOrigem(normalizar(origem));
        instrucao.setDestino(normalizar(destino));
        instrucao.setPrioridade(prioridade != null ? prioridade : PrioridadeInstrucao.NORMAL);
        instrucao.setStatus(StatusInstrucao.PENDENTE);
        instrucao.setAgendadaEm(agendadaEm);
        instrucao.setEquipamento(normalizar(equipamento));
        instrucao.setEquipe(normalizar(equipe));
        instrucao.setObservacoes(normalizar(observacoes));
        instrucao.setCriadoPor(obrigatorio(criadoPor, "Usuário criador"));
        return instrucaoRepositorio.save(instrucao);
    }

    public InstrucaoTrabalho iniciar(Long id) {
        InstrucaoTrabalho instrucao = obter(id);
        exigirStatus(instrucao, StatusInstrucao.PENDENTE);
        instrucao.setStatus(StatusInstrucao.EM_EXECUCAO);
        instrucao.setIniciadaEm(LocalDateTime.now());
        return instrucaoRepositorio.save(instrucao);
    }

    public InstrucaoTrabalho concluir(Long id) {
        InstrucaoTrabalho instrucao = obter(id);
        exigirStatus(instrucao, StatusInstrucao.EM_EXECUCAO);
        aplicarConclusaoNoInventario(instrucao);
        instrucao.setStatus(StatusInstrucao.CONCLUIDA);
        instrucao.setConcluidaEm(LocalDateTime.now());
        return instrucaoRepositorio.save(instrucao);
    }

    public InstrucaoTrabalho cancelar(Long id, String justificativa) {
        InstrucaoTrabalho instrucao = obter(id);
        if (instrucao.getStatus() == StatusInstrucao.CONCLUIDA) {
            throw new IllegalStateException("Instrução concluída não pode ser cancelada");
        }
        if (instrucao.getStatus() == StatusInstrucao.CANCELADA) {
            throw new IllegalStateException("Instrução já está cancelada");
        }
        instrucao.setJustificativaCancelamento(obrigatorio(justificativa, "Justificativa do cancelamento"));
        instrucao.setStatus(StatusInstrucao.CANCELADA);
        instrucao.setCanceladaEm(LocalDateTime.now());
        return instrucaoRepositorio.save(instrucao);
    }

    @Transactional(readOnly = true)
    public InstrucaoTrabalho obter(Long id) {
        return instrucaoRepositorio.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Instrução de trabalho não encontrada"));
    }

    @Transactional(readOnly = true)
    public List<InstrucaoTrabalho> pesquisar(StatusInstrucao status, String codigoConteiner) {
        return instrucaoRepositorio.findAll().stream()
                .filter(item -> status == null || item.getStatus() == status)
                .filter(item -> !StringUtils.hasText(codigoConteiner)
                        || item.getCodigoConteiner().equalsIgnoreCase(codigoConteiner.trim()))
                .sorted(Comparator.comparingInt((InstrucaoTrabalho item) -> peso(item.getPrioridade())).reversed()
                        .thenComparing(item -> item.getAgendadaEm() != null ? item.getAgendadaEm() : item.getCreatedAt()))
                .collect(Collectors.toList());
    }

    private void validarMovimentacao(String codigoConteiner, String destino) {
        String destinoNormalizado = obrigatorio(destino, "Destino");
        if (instrucaoRepositorio.existsByDestinoIgnoreCaseAndStatusIn(destinoNormalizado, STATUS_ATIVOS)) {
            throw new IllegalStateException("Já existe uma instrução ativa para o destino informado");
        }

        ConteinerPatio conteiner = conteinerRepositorio.findByCodigoIgnoreCase(codigoConteiner)
                .orElseThrow(() -> new NoSuchElementException("Contêiner não encontrado no pátio"));
        ConteinerPatioRequisicaoDto requisicao = new ConteinerPatioRequisicaoDto();
        requisicao.setCodigo(conteiner.getCodigo());
        requisicao.setLinha(conteiner.getPosicao() != null ? conteiner.getPosicao().getLinha() : 0);
        requisicao.setColuna(conteiner.getPosicao() != null ? conteiner.getPosicao().getColuna() : 0);
        requisicao.setStatus(conteiner.getStatus());
        requisicao.setTipoCarga(conteiner.getTipoCarga() != null ? conteiner.getTipoCarga().name() : null);
        requisicao.setDestino(destinoNormalizado);
        requisicao.setCamadaOperacional("1");
        validadorPlacement.validarAlocacao(requisicao);
    }

    private void aplicarConclusaoNoInventario(InstrucaoTrabalho instrucao) {
        if (instrucao.getTipoOperacao() == TipoOperacaoInstrucao.MOVIMENTACAO) {
            ConteinerPatio conteiner = conteinerRepositorio.findByCodigoIgnoreCase(instrucao.getCodigoConteiner())
                    .orElseThrow(() -> new NoSuchElementException("Contêiner não encontrado no pátio"));
            conteiner.setDestino(instrucao.getDestino());
            conteinerRepositorio.save(conteiner);
        }
        if (StringUtils.hasText(instrucao.getEquipamento())) {
            EquipamentoPatio equipamento = equipamentoRepositorio.findByIdentificador(instrucao.getEquipamento())
                    .orElseThrow(() -> new NoSuchElementException("Equipamento da instrução não encontrado"));
            equipamento.setStatusOperacional(StatusEquipamento.OPERACIONAL);
            atualizarPosicaoEquipamento(equipamento, instrucao.getDestino());
            equipamentoRepositorio.save(equipamento);
        }
    }

    private void atualizarPosicaoEquipamento(EquipamentoPatio equipamento, String destino) {
        if (!StringUtils.hasText(destino)) {
            return;
        }
        Matcher matcher = POSICAO_PATTERN.matcher(destino);
        if (matcher.find()) {
            equipamento.setLinha(Integer.valueOf(matcher.group(1)));
            equipamento.setColuna(Integer.valueOf(matcher.group(2)));
        }
    }

    private void exigirStatus(InstrucaoTrabalho instrucao, StatusInstrucao statusEsperado) {
        if (instrucao.getStatus() != statusEsperado) {
            throw new IllegalStateException("Transição inválida para instrução no status " + instrucao.getStatus());
        }
    }

    private int peso(PrioridadeInstrucao prioridade) {
        if (prioridade == PrioridadeInstrucao.EMERGENCIAL) {
            return 3;
        }
        if (prioridade == PrioridadeInstrucao.ALTA) {
            return 2;
        }
        return 1;
    }

    private String obrigatorio(String valor, String campo) {
        if (!StringUtils.hasText(valor)) {
            throw new IllegalArgumentException(campo + " deve ser informado");
        }
        return valor.trim();
    }

    private String normalizar(String valor) {
        return StringUtils.hasText(valor) ? valor.trim() : null;
    }
}
