package br.com.cloudport.servicoyard.patio.servico;

import br.com.cloudport.servicoyard.inventario.modelo.UnidadeInventario;
import br.com.cloudport.servicoyard.inventario.repositorio.UnidadeInventarioRepositorio;
import br.com.cloudport.servicoyard.patio.modelo.DivergenciaPosicaoPatio;
import br.com.cloudport.servicoyard.patio.modelo.InstrucaoTrabalho;
import br.com.cloudport.servicoyard.patio.modelo.PrioridadeInstrucao;
import br.com.cloudport.servicoyard.patio.modelo.StatusDivergenciaPosicao;
import br.com.cloudport.servicoyard.patio.modelo.StatusInstrucao;
import br.com.cloudport.servicoyard.patio.modelo.TipoOperacaoInstrucao;
import br.com.cloudport.servicoyard.patio.repositorio.DivergenciaPosicaoPatioRepositorio;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Transactional
public class DivergenciaPosicaoPatioServico {

    private static final EnumSet<StatusDivergenciaPosicao> STATUS_ATIVOS = EnumSet.of(
            StatusDivergenciaPosicao.ABERTA,
            StatusDivergenciaPosicao.EM_INVESTIGACAO,
            StatusDivergenciaPosicao.CORRECAO_PENDENTE);

    private final DivergenciaPosicaoPatioRepositorio divergenciaRepositorio;
    private final UnidadeInventarioRepositorio unidadeRepositorio;
    private final InstrucaoTrabalhoServico instrucaoServico;

    public DivergenciaPosicaoPatioServico(DivergenciaPosicaoPatioRepositorio divergenciaRepositorio,
                                           UnidadeInventarioRepositorio unidadeRepositorio,
                                           InstrucaoTrabalhoServico instrucaoServico) {
        this.divergenciaRepositorio = divergenciaRepositorio;
        this.unidadeRepositorio = unidadeRepositorio;
        this.instrucaoServico = instrucaoServico;
    }

    public DivergenciaPosicaoPatio abrir(String identificacao,
                                          String posicaoEsperada,
                                          String posicaoEncontrada,
                                          String evidencia,
                                          String abertaPor) {
        UnidadeInventario unidade = unidadeRepositorio.findComBloqueioByIdentificacaoIgnoreCase(
                        obrigatorio(identificacao, "Identificação da unidade"))
                .orElseThrow(() -> new NoSuchElementException("Unidade não encontrada no inventário canônico"));
        String esperada = obrigatorio(posicaoEsperada, "Posição esperada");
        String encontrada = obrigatorio(posicaoEncontrada, "Posição encontrada");
        if (esperada.equalsIgnoreCase(encontrada)) {
            throw new IllegalArgumentException("Posição esperada e encontrada não podem ser iguais");
        }
        divergenciaRepositorio.findFirstByUnidadeIdAndStatusIn(unidade.getId(), STATUS_ATIVOS)
                .ifPresent(caso -> {
                    throw new IllegalStateException("Já existe divergência ativa para a unidade " + identificacao);
                });

        DivergenciaPosicaoPatio caso = new DivergenciaPosicaoPatio();
        caso.setUnidade(unidade);
        caso.setCondicaoAnterior(unidade.getCondicao());
        caso.setIdentificacaoUnidade(unidade.getIdentificacao().toUpperCase(Locale.ROOT));
        caso.setPosicaoEsperada(esperada);
        caso.setPosicaoEncontrada(encontrada);
        caso.setStatus(StatusDivergenciaPosicao.ABERTA);
        caso.setBloqueada(true);
        caso.setEvidencia(normalizar(evidencia));
        caso.setAbertaPor(obrigatorio(abertaPor, "Usuário de abertura"));
        unidade.setCondicao(UnidadeInventario.CondicaoEquipamento.EM_INSPECAO);
        unidadeRepositorio.save(unidade);
        return divergenciaRepositorio.save(caso);
    }

    public DivergenciaPosicaoPatio iniciarInvestigacao(Long id, String responsavel, String evidencia) {
        DivergenciaPosicaoPatio caso = obter(id);
        exigirAtivo(caso);
        caso.setStatus(StatusDivergenciaPosicao.EM_INVESTIGACAO);
        caso.setResponsavel(obrigatorio(responsavel, "Responsável"));
        caso.setEvidencia(acumular(caso.getEvidencia(), evidencia));
        caso.setInvestigacaoIniciadaEm(LocalDateTime.now());
        return divergenciaRepositorio.save(caso);
    }

    public DivergenciaPosicaoPatio criarInstrucaoCorretiva(Long id,
                                                             String equipamento,
                                                             String equipe,
                                                             String operador) {
        DivergenciaPosicaoPatio caso = obter(id);
        if (caso.getStatus() != StatusDivergenciaPosicao.EM_INVESTIGACAO) {
            throw new IllegalStateException("A investigação deve estar iniciada antes da correção");
        }
        if (caso.getInstrucaoCorretiva() != null) {
            throw new IllegalStateException("A divergência já possui instrução corretiva");
        }
        InstrucaoTrabalho instrucao = instrucaoServico.criar(
                caso.getIdentificacaoUnidade(),
                TipoOperacaoInstrucao.MOVIMENTACAO,
                caso.getPosicaoEncontrada(),
                caso.getPosicaoEsperada(),
                PrioridadeInstrucao.ALTA,
                LocalDateTime.now(),
                equipamento,
                equipe,
                "Correção da divergência de posição #" + caso.getId(),
                obrigatorio(operador, "Operador"));
        caso.setInstrucaoCorretiva(instrucao);
        caso.setStatus(StatusDivergenciaPosicao.CORRECAO_PENDENTE);
        return divergenciaRepositorio.save(caso);
    }

    public DivergenciaPosicaoPatio resolver(Long id, String decisao) {
        DivergenciaPosicaoPatio caso = obter(id);
        String decisaoResolucao = obrigatorio(decisao, "Decisão da investigação");
        if (caso.getStatus() != StatusDivergenciaPosicao.CORRECAO_PENDENTE
                || caso.getInstrucaoCorretiva() == null) {
            throw new IllegalStateException("A divergência não possui correção pendente");
        }
        InstrucaoTrabalho instrucao = instrucaoServico.obter(caso.getInstrucaoCorretiva().getId());
        if (instrucao.getStatus() != StatusInstrucao.CONCLUIDA) {
            throw new IllegalStateException("A instrução corretiva deve estar concluída");
        }
        UnidadeInventario unidade = caso.getUnidade();
        unidade.setPosicaoAtual(caso.getPosicaoEsperada());
        restaurarCondicaoAnterior(caso);
        caso.setBloqueada(false);
        caso.setStatus(StatusDivergenciaPosicao.RESOLVIDA);
        caso.setDecisao(decisaoResolucao);
        caso.setResolvidaEm(LocalDateTime.now());
        return divergenciaRepositorio.save(caso);
    }

    public DivergenciaPosicaoPatio cancelar(Long id, String decisao) {
        DivergenciaPosicaoPatio caso = obter(id);
        exigirAtivo(caso);
        String decisaoCancelamento = obrigatorio(decisao, "Decisão do cancelamento");
        cancelarInstrucaoCorretiva(caso);
        restaurarCondicaoAnterior(caso);
        caso.setBloqueada(false);
        caso.setStatus(StatusDivergenciaPosicao.CANCELADA);
        caso.setDecisao(decisaoCancelamento);
        caso.setCanceladaEm(LocalDateTime.now());
        return divergenciaRepositorio.save(caso);
    }

    @Transactional(readOnly = true)
    public DivergenciaPosicaoPatio obter(Long id) {
        return divergenciaRepositorio.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Divergência de posição não encontrada"));
    }

    @Transactional(readOnly = true)
    public List<DivergenciaPosicaoPatio> listar() {
        return divergenciaRepositorio.findAllByOrderByAbertaEmDesc();
    }

    private void cancelarInstrucaoCorretiva(DivergenciaPosicaoPatio caso) {
        if (caso.getInstrucaoCorretiva() == null) {
            return;
        }
        InstrucaoTrabalho instrucao = instrucaoServico.obter(caso.getInstrucaoCorretiva().getId());
        if (instrucao.getStatus() == StatusInstrucao.CONCLUIDA) {
            throw new IllegalStateException(
                    "A instrução corretiva já foi concluída; a divergência deve ser resolvida");
        }
        if (instrucao.getStatus() != StatusInstrucao.CANCELADA) {
            instrucaoServico.cancelar(
                    instrucao.getId(),
                    "Cancelamento da divergência de posição #" + caso.getId());
        }
    }

    private void restaurarCondicaoAnterior(DivergenciaPosicaoPatio caso) {
        UnidadeInventario unidade = caso.getUnidade();
        if (unidade.getCondicao() == UnidadeInventario.CondicaoEquipamento.EM_INSPECAO
                && caso.getCondicaoAnterior() != null) {
            unidade.setCondicao(caso.getCondicaoAnterior());
        }
        unidadeRepositorio.save(unidade);
    }

    private void exigirAtivo(DivergenciaPosicaoPatio caso) {
        if (!STATUS_ATIVOS.contains(caso.getStatus())) {
            throw new IllegalStateException("Divergência não está ativa");
        }
    }

    private String acumular(String atual, String novaEvidencia) {
        if (!StringUtils.hasText(novaEvidencia)) {
            return atual;
        }
        return StringUtils.hasText(atual) ? atual + "\n" + novaEvidencia.trim() : novaEvidencia.trim();
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
