package br.com.cloudport.servicoyard.patio.servico;

import br.com.cloudport.servicoyard.patio.dto.ExcluirGeometriaPatioDto;
import br.com.cloudport.servicoyard.patio.dto.GeometriaPatioDto;
import br.com.cloudport.servicoyard.patio.dto.GeometriaPatioRequisicaoDto;
import br.com.cloudport.servicoyard.patio.modelo.GeometriaPatio;
import br.com.cloudport.servicoyard.patio.modelo.TipoGeometriaPatio;
import br.com.cloudport.servicoyard.patio.repositorio.GeometriaPatioRepositorio;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class GeometriaPatioServico {

    private static final String USUARIO_PADRAO = "operador";

    private final GeometriaPatioRepositorio geometriaPatioRepositorio;
    private final ObjectMapper objectMapper;

    public GeometriaPatioServico(GeometriaPatioRepositorio geometriaPatioRepositorio,
                                  ObjectMapper objectMapper) {
        this.geometriaPatioRepositorio = geometriaPatioRepositorio;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public List<GeometriaPatioDto> listarAtivas() {
        return geometriaPatioRepositorio.findAllByAtivaTrueOrderByTipoAscCodigoAsc().stream()
                .map(this::converter)
                .toList();
    }

    @Transactional
    public GeometriaPatioDto criar(GeometriaPatioRequisicaoDto requisicao) {
        String codigo = normalizarCodigo(requisicao.getCodigo());
        geometriaPatioRepositorio.findByCodigoIgnoreCase(codigo).ifPresent(geometria -> {
            throw new IllegalArgumentException("Já existe uma geometria cadastrada com o código informado.");
        });

        LocalDateTime agora = LocalDateTime.now();
        String usuario = normalizarUsuario(requisicao.getUsuario());
        GeometriaPatio geometria = new GeometriaPatio();
        geometria.setCriadoEm(agora);
        geometria.setCriadoPor(usuario);
        aplicarDados(geometria, requisicao, codigo, usuario, agora);
        return converter(geometriaPatioRepositorio.save(geometria));
    }

    @Transactional
    public GeometriaPatioDto atualizar(Long id, GeometriaPatioRequisicaoDto requisicao) {
        GeometriaPatio geometria = buscar(id);
        String codigo = normalizarCodigo(requisicao.getCodigo());
        geometriaPatioRepositorio.findByCodigoIgnoreCase(codigo)
                .filter(encontrada -> !encontrada.getId().equals(id))
                .ifPresent(encontrada -> {
                    throw new IllegalArgumentException("Já existe outra geometria cadastrada com o código informado.");
                });

        LocalDateTime agora = LocalDateTime.now();
        String usuario = normalizarUsuario(requisicao.getUsuario());
        aplicarDados(geometria, requisicao, codigo, usuario, agora);
        return converter(geometriaPatioRepositorio.save(geometria));
    }

    @Transactional
    public void desativar(Long id, ExcluirGeometriaPatioDto requisicao) {
        GeometriaPatio geometria = buscar(id);
        geometria.setAtiva(false);
        geometria.setAtualizadoEm(LocalDateTime.now());
        geometria.setAtualizadoPor(normalizarUsuario(requisicao.getUsuario()));
        geometria.setMotivoAtualizacao(requisicao.getMotivo().trim());
        geometriaPatioRepositorio.save(geometria);
    }

    private void aplicarDados(GeometriaPatio geometria,
                               GeometriaPatioRequisicaoDto requisicao,
                               String codigo,
                               String usuario,
                               LocalDateTime agora) {
        validarGeoJson(requisicao.getGeoJson());
        validarVinculoPilha(requisicao);
        geometria.setCodigo(codigo);
        geometria.setTipo(requisicao.getTipo());
        geometria.setBloco(normalizarOpcional(requisicao.getBloco()));
        geometria.setLinha(requisicao.getLinha());
        geometria.setColuna(requisicao.getColuna());
        geometria.setGeoJson(serializar(requisicao.getGeoJson()));
        geometria.setAtiva(true);
        geometria.setAtualizadoEm(agora);
        geometria.setAtualizadoPor(usuario);
        geometria.setMotivoAtualizacao(requisicao.getMotivo().trim());
    }

    private void validarVinculoPilha(GeometriaPatioRequisicaoDto requisicao) {
        if (requisicao.getTipo() != TipoGeometriaPatio.PILHA) {
            return;
        }
        if (!StringUtils.hasText(requisicao.getBloco())
                || requisicao.getLinha() == null
                || requisicao.getColuna() == null) {
            throw new IllegalArgumentException("Geometrias do tipo PILHA exigem bloco, linha e coluna.");
        }
    }

    private void validarGeoJson(JsonNode geoJson) {
        if (geoJson == null || !geoJson.isObject()) {
            throw new IllegalArgumentException("O GeoJSON deve ser um objeto válido.");
        }

        JsonNode geometria = "Feature".equalsIgnoreCase(geoJson.path("type").asText())
                ? geoJson.path("geometry")
                : geoJson;
        if (!"Polygon".equalsIgnoreCase(geometria.path("type").asText())) {
            throw new IllegalArgumentException("Somente geometrias GeoJSON do tipo Polygon são aceitas.");
        }

        JsonNode anel = geometria.path("coordinates").path(0);
        if (!anel.isArray() || anel.size() < 4) {
            throw new IllegalArgumentException("O polígono deve possuir ao menos três vértices e o ponto de fechamento.");
        }

        for (JsonNode coordenada : anel) {
            validarCoordenada(coordenada);
        }

        JsonNode primeira = anel.get(0);
        JsonNode ultima = anel.get(anel.size() - 1);
        if (Double.compare(primeira.get(0).asDouble(), ultima.get(0).asDouble()) != 0
                || Double.compare(primeira.get(1).asDouble(), ultima.get(1).asDouble()) != 0) {
            throw new IllegalArgumentException("O primeiro e o último ponto do polígono devem ser iguais.");
        }
    }

    private void validarCoordenada(JsonNode coordenada) {
        if (!coordenada.isArray() || coordenada.size() < 2
                || !coordenada.get(0).isNumber() || !coordenada.get(1).isNumber()) {
            throw new IllegalArgumentException("Cada coordenada deve conter longitude e latitude numéricas.");
        }
        double longitude = coordenada.get(0).asDouble();
        double latitude = coordenada.get(1).asDouble();
        if (longitude < -180 || longitude > 180 || latitude < -85 || latitude > 85) {
            throw new IllegalArgumentException("O polígono contém uma coordenada geográfica fora dos limites permitidos.");
        }
    }

    private GeometriaPatio buscar(Long id) {
        return geometriaPatioRepositorio.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Geometria do pátio não encontrada."));
    }

    private GeometriaPatioDto converter(GeometriaPatio geometria) {
        return new GeometriaPatioDto(
                geometria.getId(),
                geometria.getCodigo(),
                geometria.getTipo(),
                geometria.getBloco(),
                geometria.getLinha(),
                geometria.getColuna(),
                desserializar(geometria.getGeoJson()),
                geometria.getCriadoEm(),
                geometria.getAtualizadoEm(),
                geometria.getCriadoPor(),
                geometria.getAtualizadoPor(),
                geometria.getMotivoAtualizacao());
    }

    private String serializar(JsonNode geoJson) {
        try {
            return objectMapper.writeValueAsString(geoJson);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Não foi possível serializar o GeoJSON informado.", exception);
        }
    }

    private JsonNode desserializar(String geoJson) {
        try {
            return objectMapper.readTree(geoJson);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("A geometria persistida possui GeoJSON inválido.", exception);
        }
    }

    private String normalizarCodigo(String codigo) {
        return codigo.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizarUsuario(String usuario) {
        return StringUtils.hasText(usuario) ? usuario.trim() : USUARIO_PADRAO;
    }

    private String normalizarOpcional(String valor) {
        return StringUtils.hasText(valor) ? valor.trim().toUpperCase(Locale.ROOT) : null;
    }
}
