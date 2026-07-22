package br.com.cloudport.servicocargageral.servico;

import br.com.cloudport.servicocargageral.dominio.AmarradoCarga;
import br.com.cloudport.servicocargageral.dominio.ItemConhecimentoCarga;
import br.com.cloudport.servicocargageral.dominio.LoteCarga;
import br.com.cloudport.servicocargageral.dto.AmarradoCargaDTOs.AmarradoResposta;
import br.com.cloudport.servicocargageral.dto.AmarradoCargaDTOs.CriarAmarradoRequest;
import br.com.cloudport.servicocargageral.dto.AmarradoCargaDTOs.ReferenciaAmarradoResposta;
import br.com.cloudport.servicocargageral.repositorio.AmarradoCargaRepositorio;
import br.com.cloudport.servicocargageral.repositorio.LoteCargaRepositorio;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AmarradoCargaServico {

    private final AmarradoCargaRepositorio amarradoRepositorio;
    private final LoteCargaRepositorio loteRepositorio;

    public AmarradoCargaServico(
            AmarradoCargaRepositorio amarradoRepositorio,
            LoteCargaRepositorio loteRepositorio) {
        this.amarradoRepositorio = amarradoRepositorio;
        this.loteRepositorio = loteRepositorio;
    }

    @Transactional
    public AmarradoResposta criar(CriarAmarradoRequest request) {
        String codigo = request.codigo().trim().toUpperCase();
        String visitaNavioId = request.visitaNavioId().trim();
        if (amarradoRepositorio.existsByCodigoIgnoreCase(codigo)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Já existe um amarrado com esse código.");
        }

        validarIdsDuplicados(request.loteIds());
        List<LoteCarga> lotes = loteRepositorio.findAllById(request.loteIds());
        if (lotes.size() != request.loteIds().size()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Um ou mais cargo lots não foram encontrados.");
        }

        lotes.forEach(lote -> validarLoteDisponivel(lote, visitaNavioId));

        AmarradoCarga amarrado = new AmarradoCarga();
        amarrado.setCodigo(codigo);
        amarrado.setVisitaNavioId(visitaNavioId);
        lotes.stream()
                .sorted(Comparator.comparing(LoteCarga::getCodigo, String.CASE_INSENSITIVE_ORDER))
                .forEach(amarrado::adicionarLote);

        return mapear(amarradoRepositorio.save(amarrado));
    }

    @Transactional(readOnly = true)
    public List<AmarradoResposta> listar(String visitaNavioId, UUID loteId) {
        if (loteId != null) {
            return amarradoRepositorio.findByLotes_Id(loteId)
                    .map(amarrado -> List.of(mapear(amarrado)))
                    .orElseGet(List::of);
        }

        List<AmarradoCarga> amarrados = visitaNavioId == null || visitaNavioId.isBlank()
                ? amarradoRepositorio.findAllByOrderByAtualizadoEmDesc()
                : amarradoRepositorio.findByVisitaNavioIdIgnoreCaseOrderByAtualizadoEmDesc(visitaNavioId.trim());
        return amarrados.stream().map(this::mapear).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public AmarradoResposta obter(UUID id) {
        AmarradoCarga amarrado = amarradoRepositorio.findDetalhadoById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Amarrado não encontrado."));
        return mapear(amarrado);
    }

    private void validarIdsDuplicados(List<UUID> loteIds) {
        Set<UUID> idsUnicos = new HashSet<>(loteIds);
        if (idsUnicos.size() != loteIds.size()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A lista de cargo lots contém referências duplicadas.");
        }
    }

    private void validarLoteDisponivel(LoteCarga lote, String visitaNavioId) {
        if (amarradoRepositorio.existsByLotes_Id(lote.getId())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "O cargo lot " + lote.getCodigo() + " já pertence a outro amarrado íntegro.");
        }
        if (lote.getVisitaNavioId() == null || !lote.getVisitaNavioId().equalsIgnoreCase(visitaNavioId)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Todos os cargo lots do amarrado devem pertencer à visita de navio informada.");
        }
    }

    private AmarradoResposta mapear(AmarradoCarga amarrado) {
        List<ReferenciaAmarradoResposta> referencias = amarrado.getLotes().stream()
                .sorted(Comparator.comparing(LoteCarga::getCodigo, String.CASE_INSENSITIVE_ORDER))
                .map(this::mapearReferencia)
                .collect(Collectors.toList());
        return new AmarradoResposta(
                amarrado.getId(),
                amarrado.getCodigo(),
                amarrado.getVisitaNavioId(),
                amarrado.isMisto(),
                amarrado.isIntegro(),
                referencias.size(),
                amarrado.getGruposArmazenagem(),
                referencias,
                amarrado.getCriadoEm(),
                amarrado.getAtualizadoEm());
    }

    private ReferenciaAmarradoResposta mapearReferencia(LoteCarga lote) {
        ItemConhecimentoCarga item = lote.getItem();
        return new ReferenciaAmarradoResposta(
                lote.getId(),
                lote.getCodigo(),
                item.getConhecimento().getNumero(),
                item.getSequencia(),
                item.getDescricao(),
                item.getCodigoArmazenagem());
    }
}
