package br.com.cloudport.servicoyard.patio.listatrabalho.servico;

import br.com.cloudport.servicoyard.patio.listatrabalho.dto.AtualizacaoWorkQueueEquipamentoDto;
import br.com.cloudport.servicoyard.patio.listatrabalho.dto.AtualizacaoWorkQueuePowDto;
import br.com.cloudport.servicoyard.patio.listatrabalho.dto.DispatchWorkQueueDto;
import br.com.cloudport.servicoyard.patio.listatrabalho.dto.OrdemTrabalhoPatioRespostaDto;
import br.com.cloudport.servicoyard.patio.listatrabalho.dto.ResultadoDispatchWorkQueueDto;
import br.com.cloudport.servicoyard.patio.listatrabalho.dto.WorkQueuePatioRequisicaoDto;
import br.com.cloudport.servicoyard.patio.listatrabalho.dto.WorkQueuePatioRespostaDto;
import br.com.cloudport.servicoyard.patio.listatrabalho.modelo.HistoricoOperacaoPatio;
import br.com.cloudport.servicoyard.patio.listatrabalho.modelo.OrdemTrabalhoPatio;
import br.com.cloudport.servicoyard.patio.listatrabalho.modelo.StatusOrdemTrabalhoPatio;
import br.com.cloudport.servicoyard.patio.listatrabalho.modelo.StatusWorkQueuePatio;
import br.com.cloudport.servicoyard.patio.listatrabalho.modelo.WorkQueuePatio;
import br.com.cloudport.servicoyard.patio.listatrabalho.repositorio.HistoricoOperacaoPatioRepositorio;
import br.com.cloudport.servicoyard.patio.listatrabalho.repositorio.OrdemTrabalhoPatioRepositorio;
import br.com.cloudport.servicoyard.patio.listatrabalho.repositorio.WorkQueuePatioRepositorio;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

@Service
public class WorkQueuePatioServico {

    private final WorkQueuePatioRepositorio workQueueRepositorio;
    private final OrdemTrabalhoPatioRepositorio ordemRepositorio;
    private final HistoricoOperacaoPatioRepositorio historicoRepositorio;

    public WorkQueuePatioServico(WorkQueuePatioRepositorio workQueueRepositorio,
                                  OrdemTrabalhoPatioRepositorio ordemRepositorio,
                                  HistoricoOperacaoPatioRepositorio historicoRepositorio) {
        this.workQueueRepositorio = workQueueRepositorio;
        this.ordemRepositorio = ordemRepositorio;
        this.historicoRepositorio = historicoRepositorio;
    }

    @Transactional
    public List<WorkQueuePatioRespostaDto> listar(Long visitaNavioId) {
        List<WorkQueuePatio> filas = visitaNavioId == null
                ? workQueueRepositorio.findAll(Sort.by(Sort.Order.asc("visitaNavioId"), Sort.Order.asc("sequenciaInicial"), Sort.Order.asc("criadoEm")))
                : workQueueRepositorio.findByVisitaNavioIdOrderBySequenciaInicialAscCriadoEmAsc(visitaNavioId);
        if (!filas.isEmpty()) {
            vincularOrdensDeterministicas(filas, visitaNavioId);
            List<WorkQueuePatioRespostaDto> resultado = new ArrayList<>(filas.stream()
                    .map(fila -> WorkQueuePatioRespostaDto.deEntidade(fila, listarJobListDaFila(fila)))
                    .toList());
            if (visitaNavioId != null) {
                List<OrdemTrabalhoPatio> semFila = ordemRepositorio
                        .findByVisitaNavioIdOrderBySequenciaNavioAscCriadoEmAsc(visitaNavioId)
                        .stream()
                        .filter(ordem -> ordem.getWorkQueueId() == null)
                        .toList();
                resultado.addAll(listarFilasDerivadas(visitaNavioId, semFila));
            }
            return resultado;
        }
        if (visitaNavioId != null) {
            return listarFilasDerivadas(
                    visitaNavioId,
                    ordemRepositorio.findByVisitaNavioIdOrderBySequenciaNavioAscCriadoEmAsc(visitaNavioId)
            );
        }
        return List.of();
    }

    @Transactional
    public WorkQueuePatioRespostaDto criar(WorkQueuePatioRequisicaoDto dto) {
        if (dto.getVisitaNavioId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A visita de navio deve ser informada.");
        }
        LocalDateTime agora = LocalDateTime.now();
        WorkQueuePatio fila = new WorkQueuePatio();
        fila.setVisitaNavioId(dto.getVisitaNavioId());
        fila.setBerco(normalizarOpcional(dto.getBerco()));
        fila.setPorao(dto.getPorao());
        fila.setBlocoZona(normalizarOpcional(dto.getBlocoZona()));
        fila.setSequenciaInicial(dto.getSequenciaInicial());
        fila.setPow(normalizarOpcional(dto.getPow()));
        fila.setPoolOperacional(normalizarOpcional(dto.getPoolOperacional()));
        fila.setEquipamento(normalizarOpcional(dto.getEquipamento()));
        fila.setPrioridadeOperacional(dto.getPrioridadeOperacional());
        fila.setStatus(StatusWorkQueuePatio.ATIVA);
        fila.setCriadoEm(agora);
        fila.setAtualizadoEm(agora);
        fila.setIdentificador(identificador(dto));
        workQueueRepositorio.findByIdentificadorIgnoreCase(fila.getIdentificador())
                .ifPresent(existente -> {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "Ja existe work queue com este identificador.");
                });
        WorkQueuePatio salva = workQueueRepositorio.save(fila);
        if (!CollectionUtils.isEmpty(dto.getOrdemIds())) {
            vincularOrdensInformadas(salva, dto.getOrdemIds());
        } else {
            vincularOrdensDeterministicas(
                    workQueueRepositorio.findByVisitaNavioIdOrderBySequenciaInicialAscCriadoEmAsc(dto.getVisitaNavioId()),
                    dto.getVisitaNavioId()
            );
        }
        registrarHistorico(salva.getId(), null, "WORK_QUEUE_CRIADA", null,
                "Work queue " + salva.getIdentificador() + " criada.", usuarioAtual());
        return WorkQueuePatioRespostaDto.deEntidade(salva, listarJobListDaFila(salva));
    }

    @Transactional
    public WorkQueuePatioRespostaDto ativar(Long id) {
        return alterarStatus(id, StatusWorkQueuePatio.ATIVA);
    }

    @Transactional
    public WorkQueuePatioRespostaDto desativar(Long id) {
        return alterarStatus(id, StatusWorkQueuePatio.INATIVA);
    }

    @Transactional
    public WorkQueuePatioRespostaDto atualizarPow(Long id, AtualizacaoWorkQueuePowDto dto) {
        WorkQueuePatio fila = buscarFila(id);
        fila.setPow(normalizarOpcional(dto == null ? null : dto.getPow()));
        fila.setPoolOperacional(normalizarOpcional(dto == null ? null : dto.getPoolOperacional()));
        fila.setAtualizadoEm(LocalDateTime.now());
        WorkQueuePatio salva = workQueueRepositorio.save(fila);
        registrarHistorico(id, null, "POW_POOL_ATUALIZADO", null,
                "POW=" + valor(salva.getPow(), "SEM_POW") + "; pool=" + valor(salva.getPoolOperacional(), "SEM_POOL"), usuarioAtual());
        return WorkQueuePatioRespostaDto.deEntidade(salva, listarJobListDaFila(salva));
    }

    @Transactional
    public WorkQueuePatioRespostaDto atualizarEquipamento(Long id, AtualizacaoWorkQueueEquipamentoDto dto) {
        WorkQueuePatio fila = buscarFila(id);
        fila.setEquipamento(normalizarOpcional(dto == null ? null : dto.getEquipamento()));
        fila.setAtualizadoEm(LocalDateTime.now());
        WorkQueuePatio salva = workQueueRepositorio.save(fila);
        registrarHistorico(id, null, "EQUIPAMENTO_ATUALIZADO", null,
                "Equipamento=" + valor(salva.getEquipamento(), "SEM_EQUIPAMENTO"), usuarioAtual());
        return WorkQueuePatioRespostaDto.deEntidade(salva, listarJobListDaFila(salva));
    }

    @Transactional
    public WorkQueuePatioRespostaDto atualizarOrdens(Long id, List<Long> ordemIds) {
        WorkQueuePatio fila = buscarFila(id);
        List<OrdemTrabalhoPatio> atuais = listarJobListDaFila(fila);
        atuais.forEach(ordem -> {
            ordem.setWorkQueueId(null);
            ordem.setAtualizadoEm(LocalDateTime.now());
        });
        ordemRepositorio.saveAll(atuais);
        vincularOrdensInformadas(fila, ordemIds == null ? List.of() : ordemIds);
        registrarHistorico(id, null, "JOB_LIST_ATUALIZADA", null,
                "Total de ordens vinculadas=" + listarJobListDaFila(fila).size(), usuarioAtual());
        return WorkQueuePatioRespostaDto.deEntidade(fila, listarJobListDaFila(fila));
    }

    @Transactional(readOnly = true)
    public List<OrdemTrabalhoPatioRespostaDto> listarJobList(Long id) {
        return listarJobListDaFila(buscarFila(id)).stream()
                .map(OrdemTrabalhoPatioRespostaDto::deEntidade)
                .toList();
    }

    @Transactional
    public ResultadoDispatchWorkQueueDto despachar(Long id, DispatchWorkQueueDto dto) {
        WorkQueuePatio fila = buscarFila(id);
        if (fila.getStatus() != StatusWorkQueuePatio.ATIVA) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A work queue precisa estar ativa para dispatch.");
        }
        DispatchWorkQueueDto comando = dto == null ? new DispatchWorkQueueDto() : dto;
        Set<Long> idsSelecionados = CollectionUtils.isEmpty(comando.getOrdemIds())
                ? Set.of()
                : comando.getOrdemIds().stream().filter(Objects::nonNull).collect(Collectors.toSet());
        int limite = comando.limiteOrdensEfetivo();
        int despachadas = 0;
        int ignoradas = 0;
        for (OrdemTrabalhoPatio ordem : listarJobListDaFila(fila)) {
            if (despachadas >= limite) {
                ignoradas++;
                continue;
            }
            if (!idsSelecionados.isEmpty() && !idsSelecionados.contains(ordem.getId())) {
                continue;
            }
            if (comando.somentePendentesEfetivo() && ordem.getStatusOrdem() != StatusOrdemTrabalhoPatio.PENDENTE) {
                ignoradas++;
                continue;
            }
            if (ordem.getStatusOrdem() == StatusOrdemTrabalhoPatio.CONCLUIDA
                    || ordem.getStatusOrdem() == StatusOrdemTrabalhoPatio.CANCELADA) {
                ignoradas++;
                continue;
            }
            ordem.setStatusOrdem(StatusOrdemTrabalhoPatio.EM_EXECUCAO);
            ordem.setAtualizadoEm(LocalDateTime.now());
            ordemRepositorio.save(ordem);
            registrarHistorico(id, ordem.getId(), "WORK_INSTRUCTION_DESPACHADA", comando.getObservacao(),
                    "Work instruction despachada pela fila " + fila.getIdentificador() + ".", comando.usuarioEfetivo());
            despachadas++;
        }
        registrarHistorico(id, null, "WORK_QUEUE_DESPACHADA", comando.getObservacao(),
                "Despachadas=" + despachadas + "; ignoradas=" + ignoradas + ".", comando.usuarioEfetivo());
        return new ResultadoDispatchWorkQueueDto(
                id,
                despachadas,
                ignoradas,
                "Dispatch executado para a work queue " + fila.getIdentificador() + ".",
                listarJobList(id)
        );
    }

    @Transactional
    public OrdemTrabalhoPatioRespostaDto resetarInstrucao(Long ordemId) {
        OrdemTrabalhoPatio ordem = buscarOrdem(ordemId);
        if (ordem.getStatusOrdem() == StatusOrdemTrabalhoPatio.CONCLUIDA) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Instrucao concluida nao pode ser resetada por este endpoint.");
        }
        ordem.setStatusOrdem(StatusOrdemTrabalhoPatio.PENDENTE);
        ordem.setConcluidoEm(null);
        ordem.setAtualizadoEm(LocalDateTime.now());
        OrdemTrabalhoPatio salva = ordemRepositorio.save(ordem);
        registrarHistorico(ordem.getWorkQueueId(), ordemId, "WORK_INSTRUCTION_RESETADA", null,
                "Work instruction retornada para PENDENTE.", usuarioAtual());
        return OrdemTrabalhoPatioRespostaDto.deEntidade(salva);
    }

    @Transactional
    public OrdemTrabalhoPatioRespostaDto cancelarInstrucao(Long ordemId) {
        OrdemTrabalhoPatio ordem = buscarOrdem(ordemId);
        if (ordem.getStatusOrdem() == StatusOrdemTrabalhoPatio.CONCLUIDA) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Instrucao concluida nao pode ser cancelada por este endpoint.");
        }
        ordem.setStatusOrdem(StatusOrdemTrabalhoPatio.CANCELADA);
        ordem.setAtualizadoEm(LocalDateTime.now());
        OrdemTrabalhoPatio salva = ordemRepositorio.save(ordem);
        registrarHistorico(ordem.getWorkQueueId(), ordemId, "WORK_INSTRUCTION_CANCELADA", null,
                "Work instruction cancelada.", usuarioAtual());
        return OrdemTrabalhoPatioRespostaDto.deEntidade(salva);
    }

    private WorkQueuePatioRespostaDto alterarStatus(Long id, StatusWorkQueuePatio status) {
        WorkQueuePatio fila = buscarFila(id);
        fila.setStatus(status);
        fila.setAtualizadoEm(LocalDateTime.now());
        WorkQueuePatio salva = workQueueRepositorio.save(fila);
        registrarHistorico(id, null, status == StatusWorkQueuePatio.ATIVA ? "WORK_QUEUE_ATIVADA" : "WORK_QUEUE_DESATIVADA",
                null, "Status alterado para " + status.name() + ".", usuarioAtual());
        return WorkQueuePatioRespostaDto.deEntidade(salva, listarJobListDaFila(salva));
    }

    private WorkQueuePatio buscarFila(Long id) {
        return workQueueRepositorio.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Work queue de patio nao encontrada."));
    }

    private OrdemTrabalhoPatio buscarOrdem(Long ordemId) {
        return ordemRepositorio.findById(ordemId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Work instruction de patio nao encontrada."));
    }

    private List<OrdemTrabalhoPatio> listarJobListDaFila(WorkQueuePatio fila) {
        if (fila.getId() == null) {
            return List.of();
        }
        return ordemRepositorio.findByWorkQueueIdOrderByPrioridadeOperacionalAscSequenciaNavioAscCriadoEmAsc(fila.getId());
    }

    private void vincularOrdensInformadas(WorkQueuePatio fila, List<Long> ordemIds) {
        for (Long ordemId : ordemIds.stream().filter(Objects::nonNull).distinct().toList()) {
            OrdemTrabalhoPatio ordem = buscarOrdem(ordemId);
            if (!Objects.equals(fila.getVisitaNavioId(), ordem.getVisitaNavioId())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "A ordem " + ordemId + " nao pertence a visita da work queue.");
            }
            Long filaAnterior = ordem.getWorkQueueId();
            ordem.setWorkQueueId(fila.getId());
            ordem.setAtualizadoEm(LocalDateTime.now());
            ordemRepositorio.save(ordem);
            registrarHistorico(fila.getId(), ordemId, "WORK_INSTRUCTION_VINCULADA", null,
                    "Fila anterior=" + filaAnterior + "; fila atual=" + fila.getId() + ".", usuarioAtual());
        }
    }

    private void vincularOrdensDeterministicas(List<WorkQueuePatio> filas, Long visitaNavioId) {
        if (visitaNavioId == null || filas.isEmpty()) {
            return;
        }
        List<OrdemTrabalhoPatio> ordens = ordemRepositorio.findByVisitaNavioIdOrderBySequenciaNavioAscCriadoEmAsc(visitaNavioId);
        for (OrdemTrabalhoPatio ordem : ordens) {
            if (ordem.getWorkQueueId() != null) {
                continue;
            }
            List<WorkQueuePatio> candidatas = filas.stream()
                    .filter(fila -> correspondeZona(ordem, fila))
                    .toList();
            if (candidatas.size() != 1) {
                continue;
            }
            WorkQueuePatio fila = candidatas.get(0);
            ordem.setWorkQueueId(fila.getId());
            ordem.setAtualizadoEm(LocalDateTime.now());
            ordemRepositorio.save(ordem);
            registrarHistorico(fila.getId(), ordem.getId(), "VINCULO_AUTOMATICO", null,
                    "Ordem vinculada automaticamente por destino/bloco-zona sem ambiguidade.", "sistema");
        }
    }

    private boolean correspondeZona(OrdemTrabalhoPatio ordem, WorkQueuePatio fila) {
        if (!StringUtils.hasText(fila.getBlocoZona())) {
            return true;
        }
        String destino = valor(ordem.getDestino(), "");
        String zona = valor(fila.getBlocoZona(), "");
        return destino.equals(zona)
                || destino.startsWith(zona + "-")
                || destino.startsWith(zona + "/")
                || destino.startsWith(zona + " ");
    }

    private List<WorkQueuePatioRespostaDto> listarFilasDerivadas(Long visitaNavioId,
                                                                  List<OrdemTrabalhoPatio> ordens) {
        if (ordens == null || ordens.isEmpty()) {
            return List.of();
        }
        Map<String, List<OrdemTrabalhoPatio>> agrupadas = ordens.stream()
                .collect(Collectors.groupingBy(this::chaveDerivada, LinkedHashMap::new, Collectors.toList()));
        return agrupadas.entrySet().stream()
                .map(entry -> WorkQueuePatioRespostaDto.deEntidade(
                        filaDerivada(visitaNavioId, entry.getKey(), entry.getValue()),
                        entry.getValue()
                ))
                .toList();
    }

    private WorkQueuePatio filaDerivada(Long visitaNavioId, String chave, List<OrdemTrabalhoPatio> ordens) {
        String[] partes = chave.split("\\|", -1);
        WorkQueuePatio fila = new WorkQueuePatio();
        fila.setIdentificador("SEM_FILA|" + chave);
        fila.setVisitaNavioId(visitaNavioId);
        fila.setBerco(null);
        fila.setBlocoZona(partes.length > 0 ? partes[0] : null);
        fila.setStatus(StatusWorkQueuePatio.ATIVA);
        fila.setSequenciaInicial(ordens.stream()
                .map(OrdemTrabalhoPatio::getSequenciaNavio)
                .filter(Objects::nonNull)
                .min(Integer::compareTo)
                .orElse(null));
        return fila;
    }

    private String chaveDerivada(OrdemTrabalhoPatio ordem) {
        return valor(ordem.getDestino(), "SEM_ZONA")
                + "|" + valor(ordem.getStatusOrdem() == null ? null : ordem.getStatusOrdem().name(), "SEM_STATUS");
    }

    private String identificador(WorkQueuePatioRequisicaoDto dto) {
        if (StringUtils.hasText(dto.getIdentificador())) {
            return valor(dto.getIdentificador(), "");
        }
        return "VISITA-" + dto.getVisitaNavioId()
                + "|" + valor(dto.getBerco(), "SEM_BERCO")
                + "|" + valor(dto.getBlocoZona(), "SEM_ZONA")
                + "|" + valor(dto.getPow(), "SEM_POW");
    }

    private void registrarHistorico(Long workQueueId,
                                    Long ordemId,
                                    String acao,
                                    String motivo,
                                    String detalhes,
                                    String usuario) {
        HistoricoOperacaoPatio historico = new HistoricoOperacaoPatio();
        historico.setWorkQueueId(workQueueId);
        historico.setOrdemTrabalhoPatioId(ordemId);
        historico.setAcao(acao);
        historico.setUsuario(StringUtils.hasText(usuario) ? usuario.trim() : "sistema");
        historico.setMotivo(limitar(motivo, 500));
        historico.setDetalhes(limitar(detalhes, 2000));
        historico.setCriadoEm(LocalDateTime.now());
        historicoRepositorio.save(historico);
    }

    private String usuarioAtual() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && StringUtils.hasText(authentication.getName())
                ? authentication.getName()
                : "sistema";
    }

    private String limitar(String valor, int limite) {
        if (valor == null || valor.length() <= limite) {
            return valor;
        }
        return valor.substring(0, limite);
    }

    private String normalizarOpcional(String valor) {
        return StringUtils.hasText(valor) ? valor.trim().toUpperCase(Locale.ROOT) : null;
    }

    private String valor(String valor, String padrao) {
        return StringUtils.hasText(valor) ? valor.trim().toUpperCase(Locale.ROOT) : padrao;
    }
}
