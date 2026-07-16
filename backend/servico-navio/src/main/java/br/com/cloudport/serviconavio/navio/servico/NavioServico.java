package br.com.cloudport.serviconavio.navio.servico;

import br.com.cloudport.contracts.evento.EventoCadastroNavioV1;
import br.com.cloudport.serviconavio.comum.validacao.SanitizadorEntrada;
import br.com.cloudport.serviconavio.navio.dto.AtualizacaoNavioDTO;
import br.com.cloudport.serviconavio.navio.dto.CadastroNavioDTO;
import br.com.cloudport.serviconavio.navio.dto.NavioDetalheDTO;
import br.com.cloudport.serviconavio.navio.dto.NavioResumoDTO;
import br.com.cloudport.serviconavio.navio.entidade.Navio;
import br.com.cloudport.serviconavio.navio.repositorio.NavioRepositorio;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

@Service
public class NavioServico {

    private static final Set<String> CAMPOS_CADASTRO = Set.of(
            "nome",
            "codigoImo",
            "paisBandeira",
            "empresaArmadora",
            "capacidadeTeu",
            "loaMetros",
            "caladoMaximoMetros",
            "callSign"
    );

    private final NavioRepositorio navioRepositorio;
    private final SanitizadorEntrada sanitizadorEntrada;
    private final ApplicationEventPublisher eventPublisher;

    public NavioServico(NavioRepositorio navioRepositorio,
                         SanitizadorEntrada sanitizadorEntrada,
                         ApplicationEventPublisher eventPublisher) {
        this.navioRepositorio = navioRepositorio;
        this.sanitizadorEntrada = sanitizadorEntrada;
        this.eventPublisher = eventPublisher;
    }

    @Transactional(readOnly = true)
    public List<NavioResumoDTO> listarResumo() {
        return navioRepositorio.findAll(Sort.by(Sort.Direction.ASC, "nome")).stream()
                .map(this::mapearResumo)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public NavioDetalheDTO buscarDetalhe(Long identificador) {
        Navio navio = obterNavio(identificador);
        return mapearDetalhe(navio);
    }

    @Transactional
    public NavioDetalheDTO registrar(CadastroNavioDTO dto) {
        Navio navio = new Navio();
        preencherDadosObrigatorios(dto, navio);
        navio.setLoaMetros(dto.getLoaMetros());
        navio.setCaladoMaximoMetros(dto.getCaladoMaximoMetros());
        navio.setCallSign(tratarTextoOpcional(dto.getCallSign()));
        validarCodigoImoDisponivel(navio.getCodigoImo(), null);
        Navio salvo = navioRepositorio.save(navio);
        publicarEvento(salvo, "CRIADO", CAMPOS_CADASTRO);
        return mapearDetalhe(salvo);
    }

    @Transactional
    public NavioDetalheDTO atualizar(Long identificador, AtualizacaoNavioDTO dto) {
        Navio navio = obterNavio(identificador);
        Set<String> camposAlterados = new LinkedHashSet<>();

        if (StringUtils.hasText(dto.getNome())) {
            String valor = sanitizadorEntrada.limparTextoObrigatorio(dto.getNome(), "nome do navio");
            if (!Objects.equals(navio.getNome(), valor)) {
                navio.setNome(valor);
                camposAlterados.add("nome");
            }
        }
        if (StringUtils.hasText(dto.getCodigoImo())) {
            String valor = sanitizadorEntrada
                    .limparTextoObrigatorio(dto.getCodigoImo(), "código IMO")
                    .toUpperCase(Locale.ROOT);
            validarCodigoImoDisponivel(valor, navio.getIdentificador());
            if (!Objects.equals(navio.getCodigoImo(), valor)) {
                navio.setCodigoImo(valor);
                camposAlterados.add("codigoImo");
            }
        }
        if (StringUtils.hasText(dto.getPaisBandeira())) {
            String valor = sanitizadorEntrada
                    .limparTextoObrigatorio(dto.getPaisBandeira(), "país da bandeira");
            if (!Objects.equals(navio.getPaisBandeira(), valor)) {
                navio.setPaisBandeira(valor);
                camposAlterados.add("paisBandeira");
            }
        }
        if (StringUtils.hasText(dto.getEmpresaArmadora())) {
            String valor = sanitizadorEntrada
                    .limparTextoObrigatorio(dto.getEmpresaArmadora(), "empresa armadora");
            if (!Objects.equals(navio.getEmpresaArmadora(), valor)) {
                navio.setEmpresaArmadora(valor);
                camposAlterados.add("empresaArmadora");
            }
        }
        if (dto.getCapacidadeTeu() != null
                && !Objects.equals(navio.getCapacidadeTeu(), dto.getCapacidadeTeu())) {
            navio.setCapacidadeTeu(dto.getCapacidadeTeu());
            camposAlterados.add("capacidadeTeu");
        }
        if (dto.getLoaMetros() != null
                && !Objects.equals(navio.getLoaMetros(), dto.getLoaMetros())) {
            navio.setLoaMetros(dto.getLoaMetros());
            camposAlterados.add("loaMetros");
        }
        if (dto.getCaladoMaximoMetros() != null
                && !Objects.equals(navio.getCaladoMaximoMetros(), dto.getCaladoMaximoMetros())) {
            navio.setCaladoMaximoMetros(dto.getCaladoMaximoMetros());
            camposAlterados.add("caladoMaximoMetros");
        }
        if (dto.getCallSign() != null) {
            String valor = tratarTextoOpcional(dto.getCallSign());
            if (!Objects.equals(navio.getCallSign(), valor)) {
                navio.setCallSign(valor);
                camposAlterados.add("callSign");
            }
        }

        Navio salvo = navioRepositorio.save(navio);
        if (!camposAlterados.isEmpty()) {
            publicarEvento(salvo, "ATUALIZADO", camposAlterados);
        }
        return mapearDetalhe(salvo);
    }

    @Transactional
    public void remover(Long identificador) {
        Navio navio = obterNavio(identificador);
        navioRepositorio.delete(navio);
        publicarEvento(navio, "REMOVIDO", Set.of());
    }

    private Navio obterNavio(Long identificador) {
        return navioRepositorio.findById(identificador)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Navio não encontrado."));
    }

    private void preencherDadosObrigatorios(CadastroNavioDTO dto, Navio navio) {
        navio.setNome(sanitizadorEntrada.limparTextoObrigatorio(dto.getNome(), "nome do navio"));
        String codigoImo = sanitizadorEntrada
                .limparTextoObrigatorio(dto.getCodigoImo(), "código IMO")
                .toUpperCase(Locale.ROOT);
        navio.setCodigoImo(codigoImo);
        navio.setPaisBandeira(sanitizadorEntrada
                .limparTextoObrigatorio(dto.getPaisBandeira(), "país da bandeira"));
        navio.setEmpresaArmadora(sanitizadorEntrada
                .limparTextoObrigatorio(dto.getEmpresaArmadora(), "empresa armadora"));
        navio.setCapacidadeTeu(dto.getCapacidadeTeu());
    }

    private void validarCodigoImoDisponivel(String codigoImo, Long identificadorAtual) {
        if (identificadorAtual == null) {
            if (navioRepositorio.existsByCodigoImoIgnoreCase(codigoImo)) {
                throw new IllegalArgumentException("Já existe um navio cadastrado com o código IMO informado.");
            }
        } else if (navioRepositorio.existsByCodigoImoIgnoreCaseAndIdentificadorNot(codigoImo, identificadorAtual)) {
            throw new IllegalArgumentException("Já existe outro navio cadastrado com o código IMO informado.");
        }
    }

    private String tratarTextoOpcional(String valor) {
        String limpo = sanitizadorEntrada.limparTexto(valor);
        return StringUtils.hasText(limpo) ? limpo : null;
    }

    private void publicarEvento(Navio navio, String tipoAlteracao, Set<String> camposAlterados) {
        eventPublisher.publishEvent(EventoCadastroNavioV1.criar(
                navio.getIdentificador(),
                navio.getCodigoImo(),
                tipoAlteracao,
                camposAlterados,
                null
        ));
    }

    private NavioResumoDTO mapearResumo(Navio navio) {
        return new NavioResumoDTO(
                navio.getIdentificador(),
                navio.getNome(),
                navio.getCodigoImo(),
                navio.getEmpresaArmadora(),
                navio.getCapacidadeTeu()
        );
    }

    private NavioDetalheDTO mapearDetalhe(Navio navio) {
        return new NavioDetalheDTO(
                navio.getIdentificador(),
                navio.getNome(),
                navio.getCodigoImo(),
                navio.getPaisBandeira(),
                navio.getEmpresaArmadora(),
                navio.getCapacidadeTeu(),
                navio.getLoaMetros(),
                navio.getCaladoMaximoMetros(),
                navio.getCallSign()
        );
    }
}
