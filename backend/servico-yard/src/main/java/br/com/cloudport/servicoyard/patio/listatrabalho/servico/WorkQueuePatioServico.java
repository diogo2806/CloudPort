package br.com.cloudport.servicoyard.patio.listatrabalho.servico;

import br.com.cloudport.servicoyard.patio.listatrabalho.dto.AtualizacaoWorkQueueEquipamentoDto;
import br.com.cloudport.servicoyard.patio.listatrabalho.dto.AtualizacaoWorkQueuePowDto;
import br.com.cloudport.servicoyard.patio.listatrabalho.dto.DispatchWorkQueueDto;
import br.com.cloudport.servicoyard.patio.listatrabalho.dto.OrdemTrabalhoPatioRespostaDto;
import br.com.cloudport.servicoyard.patio.listatrabalho.dto.ResultadoDispatchWorkQueueDto;
import br.com.cloudport.servicoyard.patio.listatrabalho.dto.WorkQueuePatioRequisicaoDto;
import br.com.cloudport.servicoyard.patio.listatrabalho.dto.WorkQueuePatioRespostaDto;
import br.com.cloudport.servicoyard.patio.listatrabalho.modelo.OrdemTrabalhoPatio;
import br.com.cloudport.servicoyard.patio.listatrabalho.modelo.StatusOrdemTrabalhoPatio;
import br.com.cloudport.servicoyard.patio.listatrabalho.modelo.StatusWorkQueuePatio;
import br.com.cloudport.servicoyard.patio.listatrabalho.modelo.WorkQueuePatio;
import br.com.cloudport.servicoyard.patio.listatrabalho.repositorio.OrdemTrabalhoPatioRepositorio;
import br.com.cloudport.servicoyard.patio.listatrabalho.repositorio.WorkQueuePatioRepositorio;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

@Service
public class WorkQueuePatioServico {

    private final WorkQueuePatioRepositorio workQueueRepositorio;
    private final OrdemTrabalhoPatioRepositorio ordemRepositorio;

    public WorkQueuePatioServico(WorkQueuePatioRepositorio workQueueRepositorio,
                                 OrdemTrabalhoPatioRepositorio ordemRepositorio) {
        this.workQueueRepositorio = workQueueRepositorio;
        this.ordemRepositorio = ordemRepositorio;
    }

    @Transactional(readOnly = true)
    public List<WorkQueuePatioRespostaDto> listar(Long visitaNavioId) {
        List<WorkQueuePatio> filas = visitaNavioId == null
                ? workQueueRepositorio.findAll(Sort.by(Sort.Order.asc("visitaNavioId"), Sort.Order.asc("sequenciaInicial"), Sort.Order.asc("criadoEm")))
                : workQueueRepositorio.findByVisitaNavioIdOrderBySequenciaInicialAscCriadoEmAsc(visitaNavioId);
        if (!filas.isEmpty()) {
            return filas.stream()
                    .map(fila -> WorkQueuePatioRespostaDto.deEntidade(fila, listarJobListDaFila(fila)))
                    .toList();
        }
        if (visitaNavioId != null) {
            return listarFilasDerivadas(visitaNavioId);
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
        fila.setPow(normalizarOpcional(dto.getPow()));
        fila.setPoolOperacional(normalizarOpcional(dto.getPoolOperacional()));
        fila.setAtualizadoEm(LocalDateTime.now());
        WorkQueuePatio salva = workQueueRepositorio.save(fila);
        return WorkQueuePatioRespostaDto.deEntidade(salva, listarJobListDaFila(salva));
    }

    @Transactional
    public WorkQueuePatioRespostaDto atualizarEquipamento(Long id, AtualizacaoWorkQueueEquipamentoDto dto) {
        WorkQueuePatio fila = buscarFila(id);
        fila.setEquipamento(normalizarOpcional(dto.getEquipamento()));
        fila.setAtualizadoEm(LocalDateTime.now());
        WorkQueuePatio salva = workQueueRepositorio.save(fila);
        return WorkQueuePatioRespostaDto.deEntidade(salva, listarJobListDaFila(salva));
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
                : comando.getOrdemIds().stream().collect(Collectors.toSet());
        int despachadas = 0;
        int ignoradas = 0;
        for (OrdemTrabalhoPatio ordem : listarJobListDaFila(fila)) {
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
            despachadas++;
        }
        return new ResultadoDispatchWorkQueueDto(
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
        return OrdemTrabalhoPatioRespostaDto.deEntidade(ordemRepositorio.save(ordem));
    }

    @Transactional
    public OrdemTrabalhoPatioRespostaDto cancelarInstrucao(Long ordemId) {
        OrdemTrabalhoPatio ordem = buscarOrdem(ordemId);
        if (ordem.getStatusOrdem() == StatusOrdemTrabalhoPatio.CONCLUIDA) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Instrucao concluida nao pode ser cancelada por este endpoint.");
        }
        ordem.setStatusOrdem(StatusOrdemTrabalhoPatio.CANCELADA);
        ordem.setAtualizadoEm(LocalDateTime.now());
        return OrdemTrabalhoPatioRespostaDto.deEntidade(ordemRepositorio.save(ordem));
    }

    private WorkQueuePatioRespostaDto alterarStatus(Long id, StatusWorkQueuePatio status) {
        WorkQueuePatio fila = buscarFila(id);
        fila.setStatus(status);
        fila.setAtualizadoEm(LocalDateTime.now());
        WorkQueuePatio salva = workQueueRepositorio.save(fila);
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
        return ordemRepositorio.findByVisitaNavioIdOrderBySequenciaNavioAscCriadoEmAsc(fila.getVisitaNavioId()).stream()
                .filter(ordem -> corresponde(ordem.getDestino(), fila.getBerco()))
                .filter(ordem -> corresponde(ordem.getCamadaDestino(), fila.getBlocoZona()))
                .sorted(Comparator
                        .comparing(OrdemTrabalhoPatio::getPrioridadeOperacional, Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(OrdemTrabalhoPatio::getSequenciaNavio, Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(OrdemTrabalhoPatio::getCriadoEm, Comparator.nullsLast(LocalDateTime::compareTo)))
                .toList();
    }

    private List<WorkQueuePatioRespostaDto> listarFilasDerivadas(Long visitaNavioId) {
        List<OrdemTrabalhoPatio> ordens = ordemRepositorio.findByVisitaNavioIdOrderBySequenciaNavioAscCriadoEmAsc(visitaNavioId);
        Map<String, List<OrdemTrabalhoPatio>> agrupadas = ordens.stream()
                .collect(Collectors.groupingBy(this::chaveDerivada, LinkedHashMap::new, Collectors.toList()));
        return agrupadas.entrySet().stream()
                .map(entry -> WorkQueuePatioRespostaDto.deEntidade(filaDerivada(visitaNavioId, entry.getKey(), entry.getValue()), entry.getValue()))
                .toList();
    }

    private WorkQueuePatio filaDerivada(Long visitaNavioId, String chave, List<OrdemTrabalhoPatio> ordens) {
        String[] partes = chave.split("\\|");
        WorkQueuePatio fila = new WorkQueuePatio();
        fila.setIdentificador("DERIVADA|" + chave);
        fila.setVisitaNavioId(visitaNavioId);
        fila.setBerco(partes.length > 0 ? partes[0] : null);
        fila.setBlocoZona(partes.length > 1 ? partes[1] : null);
        fila.setStatus(StatusWorkQueuePatio.ATIVA);
        fila.setSequenciaInicial(ordens.stream()
                .map(OrdemTrabalhoPatio::getSequenciaNavio)
                .filter(java.util.Objects::nonNull)
                .min(Integer::compareTo)
                .orElse(null));
        return fila;
    }

    private String chaveDerivada(OrdemTrabalhoPatio ordem) {
        return valor(ordem.getDestino(), "SEM_BERCO")
                + "|" + valor(ordem.getCamadaDestino(), "SEM_ZONA")
                + "|" + valor(ordem.getStatusOrdem() == null ? null : ordem.getStatusOrdem().name(), "SEM_STATUS");
    }

    private boolean corresponde(String valorOrdem, String valorFila) {
        if (!StringUtils.hasText(valorFila)) {
            return true;
        }
        return valor(valorOrdem, "").equals(valor(valorFila, ""));
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

    private String normalizarOpcional(String valor) {
        return StringUtils.hasText(valor) ? valor.trim().toUpperCase(Locale.ROOT) : null;
    }

    private String valor(String valor, String padrao) {
        return StringUtils.hasText(valor) ? valor.trim().toUpperCase(Locale.ROOT) : padrao;
    }
}
