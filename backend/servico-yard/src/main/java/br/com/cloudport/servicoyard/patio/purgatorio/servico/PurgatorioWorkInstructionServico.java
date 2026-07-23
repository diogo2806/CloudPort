package br.com.cloudport.servicoyard.patio.purgatorio.servico;

import br.com.cloudport.servicoyard.patio.listatrabalho.modelo.OrdemTrabalhoPatio;
import br.com.cloudport.servicoyard.patio.listatrabalho.repositorio.OrdemTrabalhoPatioRepositorio;
import br.com.cloudport.servicoyard.patio.purgatorio.dto.ComandoPurgatorioWorkInstructionDto;
import br.com.cloudport.servicoyard.patio.purgatorio.modelo.CasoPurgatorioWorkInstruction;
import br.com.cloudport.servicoyard.patio.purgatorio.modelo.EstadoPurgatorioWorkInstruction;
import br.com.cloudport.servicoyard.patio.purgatorio.repositorio.CasoPurgatorioWorkInstructionRepositorio;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

@Service
public class PurgatorioWorkInstructionServico {

    private static final EnumSet<EstadoPurgatorioWorkInstruction> ESTADOS_ABERTOS = EnumSet.of(
            EstadoPurgatorioWorkInstruction.ABERTO,
            EstadoPurgatorioWorkInstruction.EM_TRATAMENTO,
            EstadoPurgatorioWorkInstruction.AGUARDANDO_REVALIDACAO);

    private final CasoPurgatorioWorkInstructionRepositorio repositorio;
    private final OrdemTrabalhoPatioRepositorio ordemRepositorio;

    public PurgatorioWorkInstructionServico(CasoPurgatorioWorkInstructionRepositorio repositorio,
                                             OrdemTrabalhoPatioRepositorio ordemRepositorio) {
        this.repositorio = repositorio;
        this.ordemRepositorio = ordemRepositorio;
    }

    @Transactional
    public CasoPurgatorioWorkInstruction abrir(ComandoPurgatorioWorkInstructionDto dto) {
        return repositorio.findByChaveIdempotencia(dto.getChaveIdempotencia()).orElseGet(() -> {
            validarAbertura(dto);
            OrdemTrabalhoPatio ordem = ordemRepositorio.findById(dto.getOrdemTrabalhoPatioId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                            "Work instruction de patio nao encontrada."));
            if (!ordem.getWorkQueueId().equals(dto.getWorkQueueId())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "A work instruction nao pertence a work queue informada.");
            }
            LocalDateTime agora = LocalDateTime.now();
            CasoPurgatorioWorkInstruction caso = new CasoPurgatorioWorkInstruction();
            caso.setOrdemTrabalhoPatioId(ordem.getId());
            caso.setWorkQueueId(ordem.getWorkQueueId());
            caso.setCausa(dto.getCausa());
            caso.setSeveridade(dto.getSeveridade());
            caso.setEstado(EstadoPurgatorioWorkInstruction.ABERTO);
            caso.setChaveIdempotencia(dto.getChaveIdempotencia().trim());
            caso.setUsuario(usuario(dto));
            caso.setMotivo(dto.getMotivo().trim());
            caso.setOrigem(dto.getOrigem());
            caso.setCorrelationId(dto.getCorrelationId());
            caso.setSnapshotOriginal(valor(dto.getSnapshotOriginal(), montarSnapshot(ordem)));
            caso.setSnapshotAtual(dto.getSnapshotAtual());
            caso.setEvidencias(dto.getEvidencias());
            caso.setHistorico(evento(agora, "ABERTO", usuario(dto), dto.getMotivo()));
            caso.setCriadoEm(agora);
            caso.setAtualizadoEm(agora);
            return repositorio.save(caso);
        });
    }

    @Transactional(readOnly = true)
    public List<CasoPurgatorioWorkInstruction> listarAbertos() {
        return repositorio.findByEstadoInOrderByCriadoEmAsc(ESTADOS_ABERTOS);
    }

    @Transactional(readOnly = true)
    public List<CasoPurgatorioWorkInstruction> listarPorFila(Long workQueueId) {
        return repositorio.findByWorkQueueIdOrderByCriadoEmAsc(workQueueId);
    }

    @Transactional(readOnly = true)
    public void validarDispatch(Long workQueueId) {
        if (repositorio.existsByWorkQueueIdAndEstadoIn(workQueueId, ESTADOS_ABERTOS)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Dispatch bloqueado: existem work instructions no purgatorio operacional.");
        }
    }

    @Transactional
    public CasoPurgatorioWorkInstruction corrigir(Long id, ComandoPurgatorioWorkInstructionDto dto) {
        return alterar(id, dto, EstadoPurgatorioWorkInstruction.AGUARDANDO_REVALIDACAO, "CORRIGIDO");
    }

    @Transactional
    public CasoPurgatorioWorkInstruction substituir(Long id, ComandoPurgatorioWorkInstructionDto dto) {
        return alterar(id, dto, EstadoPurgatorioWorkInstruction.AGUARDANDO_REVALIDACAO, "SUBSTITUIDO");
    }

    @Transactional
    public CasoPurgatorioWorkInstruction reencaminhar(Long id, ComandoPurgatorioWorkInstructionDto dto) {
        return alterar(id, dto, EstadoPurgatorioWorkInstruction.AGUARDANDO_REVALIDACAO, "REENCAMINHADO");
    }

    @Transactional
    public CasoPurgatorioWorkInstruction cancelar(Long id, ComandoPurgatorioWorkInstructionDto dto) {
        return alterar(id, dto, EstadoPurgatorioWorkInstruction.CANCELADO, "CANCELADO_COM_COMPENSACAO");
    }

    @Transactional
    public CasoPurgatorioWorkInstruction revalidar(Long id, ComandoPurgatorioWorkInstructionDto dto) {
        CasoPurgatorioWorkInstruction caso = buscar(id);
        if (Boolean.TRUE.equals(dto.getRevalidacaoBemSucedida())) {
            caso.setEstado(EstadoPurgatorioWorkInstruction.RESOLVIDO);
            caso.setResolucao(valor(dto.getResolucao(), dto.getMotivo()));
            caso.setResolvidoEm(LocalDateTime.now());
            registrar(caso, "REVALIDACAO_BEM_SUCEDIDA", dto);
        } else {
            caso.setEstado(EstadoPurgatorioWorkInstruction.EM_TRATAMENTO);
            registrar(caso, "REVALIDACAO_FALHOU", dto);
        }
        caso.setSnapshotAtual(dto.getSnapshotAtual());
        caso.setEvidencias(dto.getEvidencias());
        return repositorio.save(caso);
    }

    private CasoPurgatorioWorkInstruction alterar(Long id,
                                                   ComandoPurgatorioWorkInstructionDto dto,
                                                   EstadoPurgatorioWorkInstruction estado,
                                                   String acao) {
        CasoPurgatorioWorkInstruction caso = buscar(id);
        if (caso.getEstado() == EstadoPurgatorioWorkInstruction.RESOLVIDO
                || caso.getEstado() == EstadoPurgatorioWorkInstruction.CANCELADO) {
            return caso;
        }
        caso.setEstado(estado);
        caso.setSnapshotAtual(dto.getSnapshotAtual());
        caso.setEvidencias(dto.getEvidencias());
        caso.setResolucao(dto.getResolucao());
        registrar(caso, acao, dto);
        return repositorio.save(caso);
    }

    private CasoPurgatorioWorkInstruction buscar(Long id) {
        return repositorio.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Caso de purgatorio nao encontrado."));
    }

    private void registrar(CasoPurgatorioWorkInstruction caso,
                           String acao,
                           ComandoPurgatorioWorkInstructionDto dto) {
        LocalDateTime agora = LocalDateTime.now();
        caso.setAtualizadoEm(agora);
        caso.setUsuario(usuario(dto));
        caso.setCorrelationId(dto.getCorrelationId());
        caso.setHistorico(caso.getHistorico() + "\n" + evento(agora, acao, usuario(dto), dto.getMotivo()));
    }

    private void validarAbertura(ComandoPurgatorioWorkInstructionDto dto) {
        if (dto == null || dto.getOrdemTrabalhoPatioId() == null || dto.getWorkQueueId() == null
                || dto.getCausa() == null || dto.getSeveridade() == null
                || !StringUtils.hasText(dto.getChaveIdempotencia()) || !StringUtils.hasText(dto.getMotivo())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Ordem, fila, causa, severidade, chave idempotente e motivo sao obrigatorios.");
        }
    }

    private String montarSnapshot(OrdemTrabalhoPatio ordem) {
        return "ordemId=" + ordem.getId()
                + ";status=" + ordem.getStatusOrdem()
                + ";conteiner=" + ordem.getCodigoConteiner()
                + ";destino=" + ordem.getDestino()
                + ";linha=" + ordem.getLinhaDestino()
                + ";coluna=" + ordem.getColunaDestino()
                + ";camada=" + ordem.getCamadaDestino();
    }

    private String evento(LocalDateTime data, String acao, String usuario, String motivo) {
        return data + "|" + acao + "|" + usuario + "|" + valor(motivo, "SEM_MOTIVO");
    }

    private String usuario(ComandoPurgatorioWorkInstructionDto dto) {
        return valor(dto.getUsuario(), "sistema");
    }

    private String valor(String valor, String padrao) {
        return StringUtils.hasText(valor) ? valor.trim() : padrao;
    }
}
