package br.com.cloudport.servicogate.controllers;

import br.com.cloudport.servicogate.dto.EnumResponseDTO;
import br.com.cloudport.servicogate.model.enums.TipoOperacao;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/gate/config")
@Tag(name = "Configurações", description = "Endpoints de configuração para combobox dinâmicas")
public class ConfigController {

    @GetMapping("/tipos-operacao")
    @Operation(summary = "Lista os tipos de operação disponíveis")
    public ResponseEntity<List<EnumResponseDTO>> listarTiposOperacao() {
        List<EnumResponseDTO> body = Arrays.stream(TipoOperacao.values())
                .map(tipo -> new EnumResponseDTO(tipo.name(), tipo.getDescricao()))
                .collect(Collectors.toList());
        CacheControl cacheControl = CacheControl.maxAge(Duration.ofHours(1)).cachePublic();
        return ResponseEntity.ok()
                .cacheControl(cacheControl)
                .body(body);
    }
}
