package br.com.cloudport.serviconaviosiderurgico.servico;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class SeletorCamposPublicos {

    private final ObjectMapper objectMapper;

    public SeletorCamposPublicos(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public Map<String, Object> selecionar(Object origem, String campos, Set<String> permitidos) {
        Map<String, Object> valores = objectMapper.convertValue(origem, new TypeReference<>() { });
        Set<String> selecionados = camposSolicitados(campos, permitidos);
        Map<String, Object> resposta = new LinkedHashMap<>();
        selecionados.forEach(campo -> resposta.put(campo, valores.get(campo)));
        return resposta;
    }

    private Set<String> camposSolicitados(String campos, Set<String> permitidos) {
        if (!StringUtils.hasText(campos)) {
            return new LinkedHashSet<>(permitidos);
        }
        Set<String> solicitados = Arrays.stream(campos.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Set<String> invalidos = solicitados.stream()
                .filter(campo -> !permitidos.contains(campo))
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (!invalidos.isEmpty()) {
            throw new IllegalArgumentException("Campos nao permitidos: " + String.join(", ", invalidos) + ".");
        }
        return solicitados;
    }
}
