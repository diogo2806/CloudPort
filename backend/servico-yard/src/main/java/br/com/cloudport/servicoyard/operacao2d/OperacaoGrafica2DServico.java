package br.com.cloudport.servicoyard.operacao2d;

import br.com.cloudport.servicoyard.operacao2d.OperacaoGrafica2DDtos.ComandoResponse;
import br.com.cloudport.servicoyard.operacao2d.OperacaoGrafica2DDtos.RegistrarComandoRequest;
import br.com.cloudport.servicoyard.operacao2d.OperacaoGrafica2DDtos.SalvarWorkspaceRequest;
import br.com.cloudport.servicoyard.operacao2d.OperacaoGrafica2DDtos.WorkspaceResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OperacaoGrafica2DServico {

    private static final Set<String> ESCOPOS = Set.of("INDIVIDUAL", "EQUIPE", "PAPEL", "PADRAO");
    private static final Set<String> TIPOS_COMANDO = Set.of(
            "APLICAR_FLUXO_PLANEJAMENTO",
            "REPROGRAMAR_QUAY_COMMANDER",
            "ALTERAR_ALCANCE_CHE",
            "SALVAR_WORKSPACE_2D",
            "PUBLICAR_GEOMETRIA_2D",
            "CONFIRMAR_PLANO_RAIL_YARD");

    private final ComandoOperacaoGrafica2DRepositorio comandoRepositorio;
    private final WorkspaceGrafico2DRepositorio workspaceRepositorio;
    private final ObjectMapper objectMapper;

    public OperacaoGrafica2DServico(
            ComandoOperacaoGrafica2DRepositorio comandoRepositorio,
            WorkspaceGrafico2DRepositorio workspaceRepositorio,
            ObjectMapper objectMapper) {
        this.comandoRepositorio = comandoRepositorio;
        this.workspaceRepositorio = workspaceRepositorio;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public ComandoResponse registrarComando(RegistrarComandoRequest request, String usuario) {
        String commandId = normalizarObrigatorio(request.commandId(), "commandId");
        ComandoOperacaoGrafica2D existente = comandoRepositorio.findByCommandId(commandId).orElse(null);
        if (existente != null) {
            return mapearComando(existente);
        }

        String tipo = normalizarEnum(request.type());
        if (!TIPOS_COMANDO.contains(tipo)) {
            throw new IllegalArgumentException("Tipo de comando gráfico 2D não suportado: " + tipo + ".");
        }
        validarPayload(tipo, request.payload());

        ComandoOperacaoGrafica2D comando = new ComandoOperacaoGrafica2D();
        comando.setCommandId(commandId);
        comando.setTipo(tipo);
        comando.setStatus("CONFIRMADO");
        comando.setMotivo(normalizarOpcional(request.reason(), 1000));
        comando.setPayloadJson(escreverJson(request.payload()));
        comando.setSolicitadoPor(normalizarUsuario(usuario));
        try {
            comando = comandoRepositorio.saveAndFlush(comando);
        } catch (DataIntegrityViolationException exception) {
            return comandoRepositorio.findByCommandId(commandId)
                    .map(this::mapearComando)
                    .orElseThrow(() -> exception);
        }
        return mapearComando(comando);
    }

    @Transactional(readOnly = true)
    public ComandoResponse buscarComando(String commandId) {
        return comandoRepositorio.findByCommandId(normalizarObrigatorio(commandId, "commandId"))
                .map(this::mapearComando)
                .orElseThrow(() -> new IllegalArgumentException("Comando gráfico 2D não encontrado."));
    }

    @Transactional
    public WorkspaceResponse salvarWorkspace(SalvarWorkspaceRequest request, String usuario, Set<String> papeis) {
        String nome = normalizarObrigatorio(request.name(), "Nome");
        String escopo = normalizarEnum(request.scope());
        if (!ESCOPOS.contains(escopo)) {
            throw new IllegalArgumentException("Escopo de workspace inválido.");
        }
        String papel = normalizarOpcional(request.role(), 80);
        if ("PAPEL".equals(escopo) && papel == null) {
            throw new IllegalArgumentException("O papel é obrigatório para workspace por papel.");
        }
        if ("PADRAO".equals(escopo) && !papeis.contains("ROLE_ADMIN_PORTO")) {
            throw new IllegalArgumentException("Somente ADMIN_PORTO pode publicar o workspace padrão.");
        }

        String proprietario = normalizarUsuario(usuario);
        Long versao = workspaceRepositorio
                .findTopByNomeAndEscopoAndProprietarioOrderByVersaoDesc(nome, escopo, proprietario)
                .map(WorkspaceGrafico2D::getVersao)
                .map(valor -> valor + 1L)
                .orElse(1L);

        WorkspaceGrafico2D workspace = new WorkspaceGrafico2D();
        workspace.setNome(nome);
        workspace.setEscopo(escopo);
        workspace.setPapel(papel == null ? null : normalizarEnum(papel));
        workspace.setProprietario(proprietario);
        workspace.setVersao(versao);
        workspace.setConteudoJson(escreverJson(request.content()));
        return mapearWorkspace(workspaceRepositorio.saveAndFlush(workspace));
    }

    @Transactional(readOnly = true)
    public List<WorkspaceResponse> listarWorkspaces(String usuario, Set<String> papeis) {
        String proprietario = normalizarUsuario(usuario);
        return workspaceRepositorio.findAllByOrderByCriadoEmDesc().stream()
                .filter(workspace -> podeVisualizar(workspace, proprietario, papeis))
                .map(this::mapearWorkspace)
                .collect(Collectors.toList());
    }

    private boolean podeVisualizar(WorkspaceGrafico2D workspace, String usuario, Set<String> papeis) {
        if (workspace.getProprietario().equals(usuario)) {
            return true;
        }
        if ("PADRAO".equals(workspace.getEscopo()) || "EQUIPE".equals(workspace.getEscopo())) {
            return true;
        }
        return "PAPEL".equals(workspace.getEscopo())
                && workspace.getPapel() != null
                && papeis.contains(workspace.getPapel().startsWith("ROLE_")
                        ? workspace.getPapel()
                        : "ROLE_" + workspace.getPapel());
    }

    private void validarPayload(String tipo, JsonNode payload) {
        if (payload == null || payload.isNull() || !payload.isObject()) {
            throw new IllegalArgumentException("O payload do comando deve ser um objeto JSON.");
        }
        if ("APLICAR_FLUXO_PLANEJAMENTO".equals(tipo)) {
            JsonNode movimentos = payload.path("moves");
            if (!movimentos.isArray() || movimentos.isEmpty()) {
                throw new IllegalArgumentException("O fluxo de planejamento deve possuir movimentos.");
            }
        }
        if ("REPROGRAMAR_QUAY_COMMANDER".equals(tipo)
                && (!payload.path("queues").isArray() || payload.path("queues").isEmpty())) {
            throw new IllegalArgumentException("A reprogramação deve possuir filas.");
        }
        if ("ALTERAR_ALCANCE_CHE".equals(tipo)
                && (payload.path("equipmentId").asText().isBlank() || payload.path("rangeMeters").asInt() <= 0)) {
            throw new IllegalArgumentException("Equipamento e alcance são obrigatórios.");
        }
        if ("PUBLICAR_GEOMETRIA_2D".equals(tipo)
                && (!payload.path("geometry").path("elements").isArray()
                || payload.path("geometry").path("elements").isEmpty())) {
            throw new IllegalArgumentException("A geometria deve possuir elementos físicos.");
        }
        if ("CONFIRMAR_PLANO_RAIL_YARD".equals(tipo)
                && (!payload.path("assignments").isArray() || payload.path("assignments").isEmpty())) {
            throw new IllegalArgumentException("O plano Rail × Yard deve possuir atribuições.");
        }
    }

    private ComandoResponse mapearComando(ComandoOperacaoGrafica2D comando) {
        return new ComandoResponse(
                comando.getId(),
                comando.getCommandId(),
                comando.getTipo(),
                comando.getStatus(),
                comando.getMotivo(),
                lerJson(comando.getPayloadJson()),
                comando.getSolicitadoPor(),
                comando.getCriadoEm());
    }

    private WorkspaceResponse mapearWorkspace(WorkspaceGrafico2D workspace) {
        return new WorkspaceResponse(
                workspace.getId(),
                workspace.getNome(),
                workspace.getEscopo(),
                workspace.getPapel(),
                workspace.getProprietario(),
                workspace.getVersao(),
                lerJson(workspace.getConteudoJson()),
                workspace.getCriadoEm());
    }

    private String escreverJson(JsonNode value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("JSON operacional inválido.", exception);
        }
    }

    private JsonNode lerJson(String value) {
        try {
            return objectMapper.readTree(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("JSON operacional persistido está inválido.", exception);
        }
    }

    private String normalizarObrigatorio(String value, String campo) {
        String normalized = value == null ? "" : value.trim();
        if (normalized.isBlank()) {
            throw new IllegalArgumentException(campo + " é obrigatório.");
        }
        return normalized;
    }

    private String normalizarOpcional(String value, int limite) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim();
        return normalized.length() <= limite ? normalized : normalized.substring(0, limite);
    }

    private String normalizarEnum(String value) {
        return normalizarObrigatorio(value, "Valor")
                .toUpperCase(Locale.ROOT)
                .replaceAll("[^A-Z0-9_]+", "_")
                .replaceAll("^_+|_+$", "");
    }

    private String normalizarUsuario(String usuario) {
        return usuario == null || usuario.isBlank() ? "operador" : usuario.trim();
    }
}
