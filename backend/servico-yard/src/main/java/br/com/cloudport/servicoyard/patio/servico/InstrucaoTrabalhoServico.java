package br.com.cloudport.servicoyard.patio.servico;

import br.com.cloudport.servicoyard.patio.dto.ConteinerPatioRequisicaoDto;
import br.com.cloudport.servicoyard.patio.modelo.ConteinerPatio;
import br.com.cloudport.servicoyard.patio.modelo.EquipamentoPatio;
import br.com.cloudport.servicoyard.patio.modelo.InstrucaoTrabalho;
import br.com.cloudport.servicoyard.patio.modelo.PosicaoPatio;
import br.com.cloudport.servicoyard.patio.modelo.PrioridadeInstrucao;
import br.com.cloudport.servicoyard.patio.modelo.StatusEquipamento;
import br.com.cloudport.servicoyard.patio.modelo.StatusInstrucao;
import br.com.cloudport.servicoyard.patio.modelo.TipoOperacaoInstrucao;
import br.com.cloudport.servicoyard.patio.repositorio.ConteinerPatioRepositorio;
import br.com.cloudport.servicoyard.patio.repositorio.EquipamentoPatioRepositorio;
import br.com.cloudport.servicoyard.patio.repositorio.InstrucaoTrabalhoRepositorio;
import br.com.cloudport.servicoyard.patio.repositorio.PosicaoPatioRepositorio;
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
    private static final int TAMANHO_MAXIMO_DESTINO = 100;
    private static final Pattern POSICAO_PATTERN = Pattern.compile("(\\d++)\\D++(\\d++)(?:\\D++(\\d++))?");

    private final InstrucaoTrabalhoRepositorio instrucaoRepositorio;
    private final ConteinerPatioRepositorio conteinerRepositorio;
    private final EquipamentoPatioRepositorio equipamentoRepositorio;
    private final PosicaoPatioRepositorio posicaoRepositorio;
    private final ValidadorYardPlacementService validadorPlacement;
    private final AvisoEstivagemPatioServico avisoEstivagemServico;

    public InstrucaoTrabalhoServico(InstrucaoTrabalhoRepositorio instrucaoRepositorio,
                                     ConteinerPatioRepositorio conteinerRepositorio,
                                     EquipamentoPatioRepositorio equipamentoRepositorio,
                                     PosicaoPatioRepositorio posicaoRepositorio,
                                     ValidadorYardPlacementService validadorPlacement,
                                     AvisoEstivagemPatioServico avisoEstivagemServico) {
        this.instrucaoRepositorio = instrucaoRepositorio;
        this.conteinerRepositorio = conteinerRepositorio;
        this.equipamentoRepositorio = equipamentoRepositorio;
        this.posicaoRepositorio = posicaoRepositorio;
        this.validadorPlacement = validadorPlacement;
        this.avisoEstivagemServico = avisoEstivagemServico;
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
        InstrucaoTrabalho salvo = instrucaoRepositorio.save(instrucao);
        if (instrucao.getTipoOperacao() == TipoOperacaoInstrucao.MOVIMENTACAO) {
            avisoEstivagemServico.reavaliarAposMovimentacao(
                    instrucao.getCodigoConteiner(),
                    instrucao.getOrigem(),
                    instrucao.getDestino(),
                    instrucao.getCriadoPor());
        }
        return salvo;
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
        DestinoPatio destinoPatio = extrairDestino(destinoNormalizado);
        PosicaoPatio posicaoDestino = buscarPosicaoDestino(destinoPatio);
        if (posicaoDestino != null) {
            validarDisponibilidade(posicaoDestino);
        }

        avisoEstivagemServico.validarOperacaoSemAvisoCritico(codigoConteiner, destinoNormalizado);

        ConteinerPatioRequisicaoDto requisicao = new ConteinerPatioRequisicaoDto();
        requisicao.setCodigo(conteiner.getCodigo());
        requisicao.setLinha(destinoPatio != null
                ? destinoPatio.linha
                : conteiner.getPosicao() != null ? conteiner.getPosicao().getLinha() : 0);
        requisicao.setColuna(destinoPatio != null
                ? destinoPatio.coluna
                : conteiner.getPosicao() != null ? conteiner.getPosicao().getColuna() : 0);
        requisicao.setStatus(conteiner.getStatus());
        requisicao.setTipoCarga(conteiner.getTipoCarga() != null ? conteiner.getTipoCarga().name() : null);
        requisicao.setDestino(destinoNormalizado);
        requisicao.setCamadaOperacional(destinoPatio != null ? destinoPatio.camada : "1");
        validadorPlacement.validarAlocacao(requisicao);
    }

    private void aplicarConclusaoNoInventario(InstrucaoTrabalho instrucao) {
        if (instrucao.getTipoOperacao() == TipoOperacaoInstrucao.MOVIMENTACAO) {
            ConteinerPatio conteiner = conteinerRepositorio.findByCodigoIgnoreCase(instrucao.getCodigoConteiner())
                    .orElseThrow(() -> new NoSuchElementException("Contêiner não encontrado no pátio"));
            DestinoPatio destinoPatio = extrairDestino(instrucao.getDestino());
            PosicaoPatio posicaoDestino = buscarPosicaoDestino(destinoPatio);
            if (posicaoDestino != null) {
                validarDisponibilidade(posicaoDestino);
                conteiner.setPosicao(posicaoDestino);
            }
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

    private PosicaoPatio buscarPosicaoDestino(DestinoPatio destino) {
        if (destino == null) {
            return null;
        }
        return posicaoRepositorio.findByLinhaAndColunaAndCamadaOperacional(
                        destino.linha, destino.coluna, destino.camada)
                .orElseThrow(() -> new NoSuchElementException(
                        String.format("Posição de destino %d/%d/%s não encontrada", destino.linha, destino.coluna, destino.camada)));
    }

    private void validarDisponibilidade(PosicaoPatio posicao) {
        if (posicao.isBloqueada() || posicao.isInterditada() || !posicao.isAreaPermitida()) {
            throw new IllegalStateException("A posição de destino está bloqueada, interditada ou fora da área permitida");
        }
    }

    private DestinoPatio extrairDestino(String destino) {
        if (!StringUtils.hasText(destino) || destino.length() > TAMANHO_MAXIMO_DESTINO) {
            return null;
        }
        Matcher matcher = POSICAO_PATTERN.matcher(destino);
        if (!matcher.find()) {
            return null;
        }
        String camada = matcher.group(3) != null ? matcher.group(3) : "1";
        return new DestinoPatio(Integer.valueOf(matcher.group(1)), Integer.valueOf(matcher.group(2)), camada);
    }

    private void atualizarPosicaoEquipamento(EquipamentoPatio equipamento, String destino) {
        DestinoPatio destinoPatio = extrairDestino(destino);
        if (destinoPatio != null) {
            equipamento.setLinha(destinoPatio.linha);
            equipamento.setColuna(destinoPatio.coluna);
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

    private static final class DestinoPatio {
        private final Integer linha;
        private final Integer coluna;
        private final String camada;

        private DestinoPatio(Integer linha, Integer coluna, String camada) {
            this.linha = linha;
            this.coluna = coluna;
            this.camada = camada;
        }
    }
}
