package br.com.cloudport.servicogate.app.frota;

import br.com.cloudport.servicogate.app.cidadao.VeiculoRepository;
import br.com.cloudport.servicogate.app.configuracoes.TransportadoraRepository;
import br.com.cloudport.servicogate.app.frota.VeiculoFrotaDtos.Resposta;
import br.com.cloudport.servicogate.app.frota.VeiculoFrotaDtos.Salvar;
import br.com.cloudport.servicogate.model.Transportadora;
import br.com.cloudport.servicogate.model.Veiculo;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Stream;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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
        Stream<Veiculo> stream = transportadoraId == null
                ? veiculoRepository.findAll().stream()
                : veiculoRepository.findByTransportadoraIdOrderByPlacaAsc(transportadoraId).stream();
        String termo = normalizar(busca);
        return stream
                .filter(veiculo -> podeAcessar(veiculo.getTransportadora(), authentication))
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

    @Transactional
    public Resposta criar(Salvar dto, Authentication authentication) {
        Transportadora transportadora = obterTransportadora(dto.transportadoraId());
        validarAcesso(transportadora, authentication);
        validarPlacas(null, dto.placa(), dto.placaCarreta());
        Veiculo veiculo = new Veiculo();
        aplicar(veiculo, dto, transportadora);
        return mapear(veiculoRepository.save(veiculo));
    }

    @Transactional
    public Resposta atualizar(Long id, Salvar dto, Authentication authentication) {
        Veiculo veiculo = obter(id);
        validarAcesso(veiculo.getTransportadora(), authentication);
        Transportadora transportadora = obterTransportadora(dto.transportadoraId());
        validarAcesso(transportadora, authentication);
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
        Transportadora transportadora = obterTransportadora(transportadoraId);
        validarAcesso(transportadora, authentication);
        return veiculoRepository.findByTransportadoraIdAndAtivoTrueOrderByPlacaAsc(transportadoraId)
                .stream().map(this::mapear).toList();
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
                .ifPresent(encontrado -> { throw conflito("Já existe um veículo cadastrado com a placa " + principal + "."); });
        if (carreta != null) {
            veiculoRepository.findByPlacaCarreta(carreta)
                    .filter(encontrado -> !Objects.equals(encontrado.getId(), id))
                    .ifPresent(encontrado -> { throw conflito("Já existe uma carreta cadastrada com a placa " + carreta + "."); });
        }
    }

    private boolean podeAcessar(Transportadora transportadora, Authentication authentication) {
        if (temPapel(authentication, "ROLE_ADMIN_PORTO", "ROLE_OPERADOR_GATE", "ROLE_PLANEJADOR")) {
            return true;
        }
        if (!temPapel(authentication, "ROLE_TRANSPORTADORA")) {
            return false;
        }
        String usuario = normalizar(authentication == null ? null : authentication.getName());
        return usuario != null && (usuario.equals(normalizar(transportadora.getDocumento()))
                || usuario.equals(normalizar(transportadora.getContato())));
    }

    private void validarAcesso(Transportadora transportadora, Authentication authentication) {
        if (!podeAcessar(transportadora, authentication)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "A transportadora autenticada não pode acessar veículos de outra transportadora.");
        }
    }

    private boolean temPapel(Authentication authentication, String... papeis) {
        if (authentication == null) return false;
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
        return new Resposta(veiculo.getId(), veiculo.getPlaca(), veiculo.getPlacaCarreta(),
                veiculo.getModelo(), veiculo.getTipo(), veiculo.getTransportadora().getId(),
                veiculo.getTransportadora().getNome(), veiculo.isAtivo());
    }

    private boolean contem(String valor, String termo) {
        return normalizar(valor) != null && normalizar(valor).contains(termo);
    }

    private String limpar(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }

    private String normalizar(String valor) {
        String limpo = limpar(valor);
        return limpo == null ? null : limpo.toUpperCase(Locale.ROOT);
    }

    private String normalizarPlaca(String valor) {
        String limpo = limpar(valor);
        return limpo == null ? null : limpo.toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]", "");
    }

    private ResponseStatusException conflito(String mensagem) {
        return new ResponseStatusException(HttpStatus.CONFLICT, mensagem);
    }
}
