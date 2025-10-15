package br.com.cloudport.servicogate.app.configuracoes;

import br.com.cloudport.servicogate.app.configuracoes.dto.EnumResponseDTO;
import br.com.cloudport.servicogate.model.enums.CanalEntrada;
import br.com.cloudport.servicogate.model.enums.MotivoExcecao;
import br.com.cloudport.servicogate.model.enums.NivelEvento;
import br.com.cloudport.servicogate.model.enums.StatusAgendamento;
import br.com.cloudport.servicogate.model.enums.StatusGate;
import br.com.cloudport.servicogate.model.enums.TipoOperacao;
import br.com.cloudport.servicogate.model.enums.TipoOcorrenciaOperador;
import br.com.cloudport.servicogate.app.configuracoes.TransportadoraRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.data.domain.Sort;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/gate/config")
@Tag(name = "Configurações", description = "Endpoints de configuração para combobox dinâmicas")
public class ConfigController {

    private static final CacheControl ENUM_CACHE = CacheControl.maxAge(Duration.ofHours(1)).cachePublic();

    private final TransportadoraRepository transportadoraRepository;

    public ConfigController(TransportadoraRepository transportadoraRepository) {
        this.transportadoraRepository = transportadoraRepository;
    }

    @GetMapping("/tipos-operacao")
    @Operation(summary = "Lista os tipos de operação disponíveis")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<EnumResponseDTO>> listarTiposOperacao() {
        return buildEnumResponse(TipoOperacao.values(), TipoOperacao::getDescricao);
    }

    @GetMapping("/status-agendamento")
    @Operation(summary = "Lista os status possíveis de um agendamento")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<EnumResponseDTO>> listarStatusAgendamento() {
        return buildEnumResponse(StatusAgendamento.values(), StatusAgendamento::getDescricao);
    }

    @GetMapping("/status-gate")
    @Operation(summary = "Lista os status disponíveis para um gate pass")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<EnumResponseDTO>> listarStatusGate() {
        return buildEnumResponse(StatusGate.values(), StatusGate::getDescricao);
    }

    @GetMapping("/motivos-excecao")
    @Operation(summary = "Lista os motivos de exceção configurados")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<EnumResponseDTO>> listarMotivosExcecao() {
        return buildEnumResponse(MotivoExcecao.values(), MotivoExcecao::getDescricao);
    }

    @GetMapping("/canais-entrada")
    @Operation(summary = "Lista os canais de entrada aceitos pela operação")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<EnumResponseDTO>> listarCanaisEntrada() {
        return buildEnumResponse(CanalEntrada.values(), CanalEntrada::getDescricao);
    }

    @GetMapping("/tipos-ocorrencia")
    @Operation(summary = "Lista os tipos de ocorrência disponíveis para o operador")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<EnumResponseDTO>> listarTiposOcorrencia() {
        return buildEnumResponse(TipoOcorrenciaOperador.values(), TipoOcorrenciaOperador::getDescricao);
    }

    @GetMapping("/niveis-evento")
    @Operation(summary = "Lista os níveis de eventos operacionais")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<EnumResponseDTO>> listarNiveisEvento() {
        return buildEnumResponse(NivelEvento.values(), NivelEvento::getDescricao);
    }

    @GetMapping("/transportadoras")
    @Operation(summary = "Lista transportadoras cadastradas para filtros analíticos")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<EnumResponseDTO>> listarTransportadoras() {
        List<EnumResponseDTO> body = transportadoraRepository.findAll(Sort.by(Sort.Direction.ASC, "nome")).stream()
                .map(transportadora -> new EnumResponseDTO(
                        transportadora.getId() != null ? transportadora.getId().toString() : null,
                        transportadora.getNome()))
                .collect(Collectors.toList());
        return ResponseEntity.ok()
                .cacheControl(ENUM_CACHE)
                .body(body);
    }

    private <E extends Enum<E>> ResponseEntity<List<EnumResponseDTO>> buildEnumResponse(E[] values,
                                                                                        Function<E, String> descricaoMapper) {
        List<EnumResponseDTO> body = Arrays.stream(values)
                .map(valor -> EnumResponseDTO.fromEnum(valor, descricaoMapper.apply(valor)))
                .collect(Collectors.toList());
        return ResponseEntity.ok()
                .cacheControl(ENUM_CACHE)
                .body(body);
    }
}
