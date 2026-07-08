package br.com.cloudport.servicoyard.patio.listatrabalho.servico;

import br.com.cloudport.servicoyard.patio.dto.ConteinerPatioRequisicaoDto;
import br.com.cloudport.servicoyard.patio.listatrabalho.dto.AtualizacaoStatusOrdemTrabalhoDto;
import br.com.cloudport.servicoyard.patio.listatrabalho.dto.OrdemTrabalhoPatioRequisicaoDto;
import br.com.cloudport.servicoyard.patio.listatrabalho.dto.OrdemTrabalhoPatioRespostaDto;
import br.com.cloudport.servicoyard.patio.listatrabalho.modelo.OrdemTrabalhoPatio;
import br.com.cloudport.servicoyard.patio.listatrabalho.modelo.StatusOrdemTrabalhoPatio;
import br.com.cloudport.servicoyard.patio.listatrabalho.repositorio.OrdemTrabalhoPatioRepositorio;
import br.com.cloudport.servicoyard.patio.modelo.ConteinerPatio;
import br.com.cloudport.servicoyard.patio.modelo.StatusConteiner;
import br.com.cloudport.servicoyard.patio.modelo.TipoMovimentoPatio;
import br.com.cloudport.servicoyard.patio.repositorio.ConteinerPatioRepositorio;
import br.com.cloudport.servicoyard.patio.servico.MapaPatioServico;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

@Service
public class OrdemTrabalhoPatioServico {

    private final OrdemTrabalhoPatioRepositorio ordemRepositorio;
    private final ConteinerPatioRepositorio conteinerRepositorio;
    private final MapaPatioServico mapaPatioServico;
    private final OtimizadorRotasPatioServico otimizadorRotas;

    public OrdemTrabalhoPatioServico(OrdemTrabalhoPatioRepositorio ordemRepositorio,
                                     ConteinerPatioRepositorio conteinerRepositorio,
                                     MapaPatioServico mapaPatioServico,
                                     OtimizadorRotasPatioServico otimizadorRotas) {
        this.ordemRepositorio = ordemRepositorio;
        this.conteinerRepositorio = conteinerRepositorio;
        this.mapaPatioServico = mapaPatioServico;
        this.otimizadorRotas = otimizadorRotas;
    }

    @Transactional(readOnly = true)
    public List<OrdemTrabalhoPatioRespostaDto> listarOrdens(StatusOrdemTrabalhoPatio status) {
        List<StatusOrdemTrabalhoPatio> filtros = status != null
                ? List.of(status)
                : new ArrayList<>(EnumSet.allOf(StatusOrdemTrabalhoPatio.class));
        return ordemRepositorio.findByStatusOrdemInOrderByCriadoEmAsc(filtros).stream()
                .map(OrdemTrabalhoPatioRespostaDto::deEntidade)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<OrdemTrabalhoPatioRespostaDto> listarOrdensPorVisitaNavio(Long visitaNavioId) {
        return ordemRepositorio.findByVisitaNavioIdOrderBySequenciaNavioAscCriadoEmAsc(visitaNavioId).stream()
                .map(OrdemTrabalhoPatioRespostaDto::deEntidade)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<OrdemTrabalhoPatioRespostaDto> listarOrdensOtimizadas(StatusOrdemTrabalhoPatio status) {
        List<OrdemTrabalhoPatio> ordens = status != null
                ? ordemRepositorio.findByStatusOrdemOrderByCriadoEmAsc(status)
                : ordemRepositorio.findByStatusOrdemInOrderByCriadoEmAsc(
                new ArrayList<>(EnumSet.allOf(StatusOrdemTrabalhoPatio.class)));

        if (ordens.isEmpty() || status != StatusOrdemTrabalhoPatio.PENDENTE) {
            return ordens.stream()
                    .map(OrdemTrabalhoPatioRespostaDto::deEntidade)
                    .toList();
        }

        List<OrdemTrabalhoPatio> ordensOtimizadas = otimizadorRotas.otimizarRota();
        return ordensOtimizadas.stream()
                .map(OrdemTrabalhoPatioRespostaDto::deEntidade)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<OrdemTrabalhoPatioRespostaDto> listarOrdensOtimizadasComDualCycling() {
        List<OrdemTrabalhoPatio> ordensOtimizadas = otimizadorRotas.otimizarRotaComProximidade();
        return ordensOtimizadas.stream()
                .map(OrdemTrabalhoPatioRespostaDto::deEntidade)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<OrdemTrabalhoPatio> listarOrdensOriginais(StatusOrdemTrabalhoPatio status) {
        return ordemRepositorio.findByStatusOrdemOrderByCriadoEmAsc(status);
    }

    @Transactional
    public OrdemTrabalhoPatioRespostaDto registrarOrdem(OrdemTrabalhoPatioRequisicaoDto dto) {
        validarCamposObrigatorios(dto);
        String codigoNormalizado = dto.getCodigoConteiner().toUpperCase(Locale.ROOT);
        List<StatusOrdemTrabalhoPatio> ativos = List.of(
                StatusOrdemTrabalhoPatio.PENDENTE,
                StatusOrdemTrabalhoPatio.EM_EXECUCAO,
                StatusOrdemTrabalhoPatio.SUSPENSA
        );
        if (dto.getItemOperacaoNavioId() != null
                && ordemRepositorio.existsByItemOperacaoNavioIdAndStatusOrdemIn(dto.getItemOperacaoNavioId(), ativos)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Já existe uma ordem ativa para este item de operação de navio.");
        }
        if (ordemRepositorio.existsByCodigoConteinerIgnoreCaseAndStatusOrdemIn(codigoNormalizado, ativos)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Já existe uma ordem pendente ou em execução para este contêiner.");
        }
        ConteinerPatio conteinerExistente = conteinerRepositorio.findByCodigoIgnoreCase(codigoNormalizado).orElse(null);
        LocalDateTime agora = LocalDateTime.now();
        String tipoCargaNormalizada = Optional.ofNullable(dto.getTipoCarga())
                .map(valor -> valor.toUpperCase(Locale.ROOT))
                .orElse(null);

        OrdemTrabalhoPatio ordem = new OrdemTrabalhoPatio(
                conteinerExistente,
                codigoNormalizado,
                tipoCargaNormalizada,
                dto.getDestino(),
                dto.getLinhaDestino(),
                dto.getColunaDestino(),
                dto.getCamadaDestino(),
                dto.getTipoMovimento(),
                StatusOrdemTrabalhoPatio.PENDENTE,
                dto.getStatusConteinerDestino(),
                agora,
                agora
        );
        ordem.setVisitaNavioId(dto.getVisitaNavioId());
        ordem.setItemOperacaoNavioId(dto.getItemOperacaoNavioId());
        ordem.setPlanoEstivaNavioId(dto.getPlanoEstivaNavioId());
        ordem.setTipoOrigem(StringUtils.hasText(dto.getTipoOrigem()) ? dto.getTipoOrigem().toUpperCase(Locale.ROOT) : null);
        ordem.setTipoDestino(StringUtils.hasText(dto.getTipoDestino()) ? dto.getTipoDestino().toUpperCase(Locale.ROOT) : null);
        ordem.setSequenciaNavio(dto.getSequenciaNavio());
        ordem.setPrioridadeOperacional(dto.getPrioridadeOperacional());
        OrdemTrabalhoPatio salvo = ordemRepositorio.save(ordem);
        return OrdemTrabalhoPatioRespostaDto.deEntidade(salvo);
    }

    @Transactional
    public OrdemTrabalhoPatioRespostaDto atualizarStatus(Long id, AtualizacaoStatusOrdemTrabalhoDto dto) {
        StatusOrdemTrabalhoPatio novoStatus = Optional.ofNullable(dto.getStatusOrdem())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "O novo status deve ser informado."));
        OrdemTrabalhoPatio ordem = ordemRepositorio.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Ordem de trabalho não encontrada."));
        validarTransicaoStatus(ordem.getStatusOrdem(), novoStatus);
        if (ordem.getStatusOrdem() == novoStatus) {
            return OrdemTrabalhoPatioRespostaDto.deEntidade(ordem);
        }
        ordem.setStatusOrdem(novoStatus);
        ordem.setAtualizadoEm(LocalDateTime.now());
        if (novoStatus == StatusOrdemTrabalhoPatio.CONCLUIDA) {
            ordem.setConcluidoEm(LocalDateTime.now());
            aplicarAtualizacaoInventario(ordem);
        }
        OrdemTrabalhoPatio atualizado = ordemRepositorio.save(ordem);
        return OrdemTrabalhoPatioRespostaDto.deEntidade(atualizado);
    }

    private void aplicarAtualizacaoInventario(OrdemTrabalhoPatio ordem) {
        ConteinerPatioRequisicaoDto requisicao = new ConteinerPatioRequisicaoDto();
        requisicao.setCodigo(ordem.getCodigoConteiner());
        requisicao.setLinha(ordem.getLinhaDestino());
        requisicao.setColuna(ordem.getColunaDestino());
        requisicao.setStatus(ordem.getStatusConteinerDestino());
        requisicao.setTipoCarga(ordem.getTipoCarga());
        requisicao.setDestino(ordem.getDestino());
        requisicao.setCamadaOperacional(ordem.getCamadaDestino());
        mapaPatioServico.registrarOuAtualizarConteiner(requisicao);
        conteinerRepositorio.findByCodigoIgnoreCase(ordem.getCodigoConteiner())
                .ifPresent(ordem::setConteiner);
    }

    private void validarCamposObrigatorios(OrdemTrabalhoPatioRequisicaoDto dto) {
        if (!StringUtils.hasText(dto.getCodigoConteiner())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "O código do contêiner é obrigatório.");
        }
        if (dto.getLinhaDestino() == null || dto.getLinhaDestino() < 0
                || dto.getColunaDestino() == null || dto.getColunaDestino() < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "As coordenadas da posição de destino devem ser válidas.");
        }
        if (!StringUtils.hasText(dto.getDestino())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "O destino da carga é obrigatório.");
        }
        if (!StringUtils.hasText(dto.getCamadaDestino())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "A camada operacional é obrigatória.");
        }
        if (dto.getTipoMovimento() == TipoMovimentoPatio.REMOCAO) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "O tipo de movimento de remoção deve ser tratado pelo módulo de expedição.");
        }
        if (dto.getStatusConteinerDestino() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "O status final do contêiner é obrigatório e deve ser válido.");
        }
    }

    private void validarTransicaoStatus(StatusOrdemTrabalhoPatio statusAtual,
                                        StatusOrdemTrabalhoPatio novoStatus) {
        if (statusAtual == novoStatus) {
            return;
        }
        if (novoStatus == StatusOrdemTrabalhoPatio.CANCELADA
                && statusAtual != StatusOrdemTrabalhoPatio.CONCLUIDA) {
            return;
        }
        if (novoStatus == StatusOrdemTrabalhoPatio.BLOQUEADA
                && statusAtual != StatusOrdemTrabalhoPatio.CONCLUIDA) {
            return;
        }
        if (novoStatus == StatusOrdemTrabalhoPatio.SUSPENSA
                && statusAtual == StatusOrdemTrabalhoPatio.EM_EXECUCAO) {
            return;
        }
        if (statusAtual == StatusOrdemTrabalhoPatio.PENDENTE
                && (novoStatus == StatusOrdemTrabalhoPatio.EM_EXECUCAO
                || novoStatus == StatusOrdemTrabalhoPatio.CONCLUIDA)) {
            return;
        }
        if (statusAtual == StatusOrdemTrabalhoPatio.EM_EXECUCAO
                && novoStatus == StatusOrdemTrabalhoPatio.CONCLUIDA) {
            return;
        }
        if ((statusAtual == StatusOrdemTrabalhoPatio.SUSPENSA || statusAtual == StatusOrdemTrabalhoPatio.BLOQUEADA)
                && novoStatus == StatusOrdemTrabalhoPatio.PENDENTE) {
            return;
        }
        throw new ResponseStatusException(HttpStatus.CONFLICT,
                String.format(Locale.ROOT,
                        "A transição de status de %s para %s não é permitida.",
                        statusAtual, novoStatus));
    }
}
