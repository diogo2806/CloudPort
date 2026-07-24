package br.com.cloudport.servicogate.app.frota;

import br.com.cloudport.servicogate.app.cidadao.VeiculoRepository;
import br.com.cloudport.servicogate.app.configuracoes.TransportadoraRepository;
import br.com.cloudport.servicogate.app.frota.VeiculoFrotaDtos.Resposta;
import br.com.cloudport.servicogate.app.frota.VeiculoFrotaDtos.Salvar;
import br.com.cloudport.servicogate.app.frota.VeiculoFrotaDtos.TransportadoraVinculada;
import br.com.cloudport.servicogate.model.Transportadora;
import br.com.cloudport.servicogate.model.Veiculo;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

@Service
public class VeiculoFrotaServico {

    private final VeiculoRepository veiculoRepository;
    private final TransportadoraRepository transportadoraRepository;

    public VeiculoFrotaServico(
            VeiculoRepository veiculoRepository,
            TransportadoraRepository transportadoraRepository) {
        this.veiculoRepository = veiculoRepository;
        this.transportadoraRepository = transportadoraRepository;
    }

    @Transactional(readOnly = true)
    public List<Resposta> listar(String busca, Long transportadoraId, Boolean ativo, Authentication authentication) {
        Stream<Veiculo> stream;
        if (temPapel(authentication, "ROLE_TRANSPORTADORA")) {
            Long vinculadaId = obterTransportadoraAutenticada(authentication).getId();
            stream = veiculoRepository.findByTransportadoraIdOrderByPlacaAsc(vinculadaId).stream();
        } else {
            exigirPapelOperacional(authentication);
            stream = transportadoraId == null
                    ? veiculoRepository.findAll().stream()
                    : veiculoRepository.findByTransportadoraIdOrderByPlacaAsc(transportadoraId).stream();
        }

        String termo = normalizar(busca);
        return stream
                .filter(veiculo -> ativo == null || veiculo.isAtivo() == ativo)
                .filter(veiculo -> termo == null
                        || contem(veiculo.getPlaca(), termo)
                        || contem(veiculo.getPlacaCarreta(), termo)
                        || contem(veiculo.getTipo(), termo)
                        || contem(veiculo.getTransportadora().getNome(), termo))
                .sorted((a, b) -> a.getPlaca().compareToIgnoreCase(b.getPlaca()))
                .map(this::mapear)
                .toList();
    }

    @Transactional(readOnly = true)
    public TransportadoraVinculada obterMinhaTransportadora(Authentication authentication) {
        Transportadora transportadora = obterTransportadoraAutenticada(authentication);
        return new TransportadoraVinculada(
                transportadora.getId(),
                transportadora.getNome(),
                transportadora.getDocumento());
    }

    @Transactional
    public Resposta criar(Salvar dto, Authentication authentication) {
        Transportadora transportadora = resolverTransportadoraAlvo(dto.transportadoraId(), authentication);
        validarPlacas(null, dto.placa(), dto.placaCarreta());
        Veiculo veiculo = new Veiculo();
        aplicar(veiculo, dto, transportadora);
        return mapear(veiculoRepository.save(veiculo));
    }

    @Transactional
    public Resposta atualizar(Long id, Salvar dto, Authentication authentication) {
        Veiculo veiculo = obter(id);
        validarAcesso(veiculo.getTransportadora(), authentication);
        Transportadora transportadora = resolverTransportadoraAlvo(dto.transportadoraId(), authentication);
        validarPlacas(id, dto.placa(), dto.placaCarreta());
        aplicar(veiculo, dto, transportadora);
        return mapear(veiculoRepository.save(veiculo));
    }

    @Transactional
    public Resposta alterarStatus(Long id, boolean ativo, Authentication authentication) {
        Veiculo veiculo = obter(id);
        validarAcesso(veiculo.getTransportadora(), authentication);
        veiculo.setAtivo(ativo);
        return mapear(veiculoRepository.save(veiculo));
    }

    @Transactional(readOnly = true)
    public List<Resposta> listarElegiveis(Long transportadoraId, Authentication authentication) {
        Transportadora transportadora = resolverTransportadoraAlvo(transportadoraId, authentication);
        return veiculoRepository.findByTransportadoraIdAndAtivoTrueOrderByPlacaAsc(transportadora.getId())
                .stream()
                .map(this::mapear)
                .toList();
    }

    private Transportadora resolverTransportadoraAlvo(Long transportadoraId, Authentication authentication) {
        if (temPapel(authentication, "ROLE_TRANSPORTADORA")) {
            Transportadora vinculada = obterTransportadoraAutenticada(authentication);
            if (!Objects.equals(vinculada.getId(), transportadoraId)) {
                throw acessoNegado();
            }
            return vinculada;
        }
        exigirPapelOperacional(authentication);
        return obterTransportadora(transportadoraId);
    }

    private Transportadora obterTransportadoraAutenticada(Authentication authentication) {
        if (!temPapel(authentication, "ROLE_TRANSPORTADORA")) {
            throw acessoNegado();
        }

        String documentoClaim = null;
        if (authentication instanceof JwtAuthenticationToken jwtAuthenticationToken) {
            Jwt token = jwtAuthenticationToken.getToken();
            documentoClaim = token.getClaimAsString("transportadoraDocumento");
            if (!StringUtils.hasText(documentoClaim)) {
                documentoClaim = token.getClaimAsString("transportadoraCnpj");
            }
        }

        String documentoNormalizado = normalizarDocumento(documentoClaim);
        if (documentoNormalizado != null) {
            Optional<Transportadora> porDocumento = transportadoraRepository.findByDocumento(documentoNormalizado);
            if (porDocumento.isPresent()) {
                return porDocumento.get();
            }
        }

        String principal = normalizar(authentication == null ? null : authentication.getName());
        return transportadoraRepository.findAll().stream()
                .filter(item -> Objects.equals(documentoNormalizado, normalizarDocumento(item.getDocumento()))
                        || Objects.equals(principal, normalizar(item.getDocumento()))
                        || Objects.equals(principal, normalizar(item.getContato())))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "Transportadora autenticada sem vínculo cadastral válido."));
    }

    private void aplicar(Veiculo veiculo, Salvar dto, Transportadora transportadora) {
        veiculo.setPlaca(normalizarPlaca(dto.placa()));
        veiculo.setPlacaCarreta(normalizarPlaca(dto.placaCarreta()));
        veiculo.setModelo(limpar(dto.modelo()));
        veiculo.setTipo(limpar(dto.tipo()).toUpperCase(Locale.ROOT));
        veiculo.setTransportadora(transportadora);
        veiculo.setAtivo(dto.ativo() == null || dto.ativo());
    }

    private void validarPlacas(Long id, String placa, String placaCarreta) {
        String principal = normalizarPlaca(placa);
        String carreta = normalizarPlaca(placaCarreta);
        if (principal != null && principal.equals(carreta)) {
            throw conflito("A placa do veículo e a placa da carreta devem ser diferentes.");
        }
        veiculoRepository.findByPlaca(principal)
                .filter(encontrado -> !Objects.equals(encontrado.getId(), id))
                .ifPresent(encontrado -> {
                    throw conflito("Já existe um veículo cadastrado com a placa " + principal + ".");
                });
        if (carreta != null) {
            veiculoRepository.findByPlacaCarreta(carreta)
                    .filter(encontrado -> !Objects.equals(encontrado.getId(), id))
                    .ifPresent(encontrado -> {
                        throw conflito("Já existe uma carreta cadastrada com a placa " + carreta + ".");
                    });
        }
    }

    private void validarAcesso(Transportadora transportadora, Authentication authentication) {
        if (temPapel(authentication, "ROLE_ADMIN_PORTO", "ROLE_OPERADOR_GATE", "ROLE_PLANEJADOR")) {
            return;
        }
        Transportadora vinculada = obterTransportadoraAutenticada(authentication);
        if (!Objects.equals(vinculada.getId(), transportadora.getId())) {
            throw acessoNegado();
        }
    }

    private void exigirPapelOperacional(Authentication authentication) {
        if (!temPapel(authentication, "ROLE_ADMIN_PORTO", "ROLE_OPERADOR_GATE", "ROLE_PLANEJADOR")) {
            throw acessoNegado();
        }
    }

    private boolean temPapel(Authentication authentication, String... papeis) {
        if (authentication == null) {
            return false;
        }
        List<String> permitidos = List.of(papeis);
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(permitidos::contains);
    }

    private Veiculo obter(Long id) {
        return veiculoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Veículo não encontrado."));
    }

    private Transportadora obterTransportadora(Long id) {
        return transportadoraRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Transportadora não encontrada."));
    }

    private Resposta mapear(Veiculo veiculo) {
        return new Resposta(
                veiculo.getId(),
                veiculo.getPlaca(),
                veiculo.getPlacaCarreta(),
                veiculo.getModelo(),
                veiculo.getTipo(),
                veiculo.getTransportadora().getId(),
                veiculo.getTransportadora().getNome(),
                veiculo.isAtivo());
    }

    private boolean contem(String valor, String termo) {
        String normalizado = normalizar(valor);
        return normalizado != null && normalizado.contains(termo);
    }

    private String limpar(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }

    private String normalizar(String valor) {
        String limpo = limpar(valor);
        return limpo == null ? null : limpo.toUpperCase(Locale.ROOT);
    }

    private String normalizarDocumento(String valor) {
        String limpo = limpar(valor);
        return limpo == null ? null : limpo.replaceAll("[^0-9A-Za-z]", "").toUpperCase(Locale.ROOT);
    }

    private String normalizarPlaca(String valor) {
        String limpo = limpar(valor);
        return limpo == null ? null : limpo.toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]", "");
    }

    private ResponseStatusException conflito(String mensagem) {
        return new ResponseStatusException(HttpStatus.CONFLICT, mensagem);
    }

    private ResponseStatusException acessoNegado() {
        return new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "A transportadora autenticada não pode acessar veículos de outra transportadora.");
    }
}
