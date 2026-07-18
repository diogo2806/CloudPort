package br.com.cloudport.servicocargageral.servico;

import br.com.cloudport.servicocargageral.dominio.IdentificacaoCargoLot;
import br.com.cloudport.servicocargageral.dominio.LoteCarga;
import br.com.cloudport.servicocargageral.dto.IdentificacaoCargoLotDTOs.IdentificacaoResposta;
import br.com.cloudport.servicocargageral.dto.IdentificacaoCargoLotDTOs.RegistrarIdentificacaoRequest;
import br.com.cloudport.servicocargageral.repositorio.IdentificacaoCargoLotRepositorio;
import br.com.cloudport.servicocargageral.repositorio.LoteCargaRepositorio;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class IdentificacaoCargoLotServico {

    private final IdentificacaoCargoLotRepositorio identificacaoRepositorio;
    private final LoteCargaRepositorio loteRepositorio;

    public IdentificacaoCargoLotServico(
            IdentificacaoCargoLotRepositorio identificacaoRepositorio,
            LoteCargaRepositorio loteRepositorio) {
        this.identificacaoRepositorio = identificacaoRepositorio;
        this.loteRepositorio = loteRepositorio;
    }

    @Transactional
    public IdentificacaoResposta registrar(RegistrarIdentificacaoRequest request) {
        LoteCarga lote = buscarLote(request.loteId());
        IdentificacaoCargoLot existente = identificacaoRepositorio
                .findByCodigoIgnoreCaseAndAtivoTrue(request.codigo())
                .orElse(null);
        if (existente != null) {
            if (!existente.getLoteId().equals(request.loteId())) {
                throw conflito("Identificação física já pertence a outro cargo lot.");
            }
            return mapear(existente, lote);
        }
        if (identificacaoRepositorio.existsByCodigoIgnoreCase(request.codigo())) {
            throw conflito("Identificação física inativa não pode ser reutilizada.");
        }
        IdentificacaoCargoLot identificacao = new IdentificacaoCargoLot();
        identificacao.setCodigo(request.codigo());
        identificacao.setTipo(request.tipo());
        identificacao.setLoteId(request.loteId());
        identificacao.setEmbalagemReferencia(request.embalagemReferencia());
        identificacao.setRegistradoPor(request.usuario());
        return mapear(identificacaoRepositorio.save(identificacao), lote);
    }

    @Transactional(readOnly = true)
    public IdentificacaoResposta resolver(String codigo) {
        IdentificacaoCargoLot identificacao = buscarIdentificacao(codigo);
        return mapear(identificacao, buscarLote(identificacao.getLoteId()));
    }

    @Transactional(readOnly = true)
    public List<IdentificacaoResposta> listar(UUID loteId) {
        LoteCarga lote = buscarLote(loteId);
        return identificacaoRepositorio.findByLoteIdOrderByRegistradoEmDesc(loteId).stream()
                .map(item -> mapear(item, lote))
                .toList();
    }

    @Transactional(readOnly = true)
    public UUID resolverLoteId(String codigo) {
        IdentificacaoCargoLot identificacao = identificacaoRepositorio
                .findByCodigoIgnoreCaseAndAtivoTrue(codigo)
                .orElse(null);
        if (identificacao != null) {
            return identificacao.getLoteId();
        }
        return loteRepositorio.findByCodigoIgnoreCase(codigo)
                .map(LoteCarga::getId)
                .orElseThrow(() -> naoEncontrada("Código de barras, QR ou cargo lot não encontrado."));
    }

    private IdentificacaoCargoLot buscarIdentificacao(String codigo) {
        return identificacaoRepositorio.findByCodigoIgnoreCaseAndAtivoTrue(codigo)
                .orElseThrow(() -> naoEncontrada("Identificação física não encontrada ou inativa."));
    }

    private LoteCarga buscarLote(UUID id) {
        return loteRepositorio.findById(id)
                .orElseThrow(() -> naoEncontrada("Cargo lot não encontrado."));
    }

    private IdentificacaoResposta mapear(IdentificacaoCargoLot identificacao, LoteCarga lote) {
        return new IdentificacaoResposta(
                identificacao.getId(),
                identificacao.getCodigo(),
                identificacao.getTipo(),
                identificacao.getLoteId(),
                lote.getCodigo(),
                identificacao.getEmbalagemReferencia(),
                identificacao.isAtivo(),
                identificacao.getRegistradoPor(),
                identificacao.getRegistradoEm());
    }

    private ResponseStatusException conflito(String mensagem) {
        return new ResponseStatusException(HttpStatus.CONFLICT, mensagem);
    }

    private ResponseStatusException naoEncontrada(String mensagem) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, mensagem);
    }
}
