package br.com.cloudport.servicorail.ferrovia.servico;

import br.com.cloudport.servicorail.comum.sanitizacao.SanitizadorEntrada;
import br.com.cloudport.servicorail.ferrovia.dto.TremMestreRequisicaoDto;
import br.com.cloudport.servicorail.ferrovia.dto.TremMestreRespostaDto;
import br.com.cloudport.servicorail.ferrovia.dto.VagaoVisitaRequisicaoDto;
import br.com.cloudport.servicorail.ferrovia.modelo.TremMestre;
import br.com.cloudport.servicorail.ferrovia.modelo.VagaoVisita;
import br.com.cloudport.servicorail.ferrovia.repositorio.TremMestreRepositorio;
import br.com.cloudport.servicorail.ferrovia.repositorio.VisitaTremRepositorio;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

@Service
public class TremMestreServico {

    private final TremMestreRepositorio repositorio;
    private final VisitaTremRepositorio visitaRepositorio;
    private final SanitizadorEntrada sanitizadorEntrada;

    public TremMestreServico(TremMestreRepositorio repositorio,
                             VisitaTremRepositorio visitaRepositorio,
                             SanitizadorEntrada sanitizadorEntrada) {
        this.repositorio = repositorio;
        this.visitaRepositorio = visitaRepositorio;
        this.sanitizadorEntrada = sanitizadorEntrada;
    }

    @Transactional(readOnly = true)
    public List<TremMestreRespostaDto> listar(boolean somenteAtivos) {
        List<TremMestre> trens = somenteAtivos
                ? repositorio.findByAtivoTrueOrderByOperadoraFerroviariaAscIdentificadorAsc()
                : repositorio.findAllByOrderByOperadoraFerroviariaAscIdentificadorAsc();
        return trens.stream().map(TremMestreRespostaDto::new).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public TremMestreRespostaDto consultar(Long id) {
        return new TremMestreRespostaDto(buscar(id));
    }

    @Transactional
    public TremMestreRespostaDto criar(TremMestreRequisicaoDto dto, String usuario) {
        TremMestre trem = new TremMestre();
        aplicar(trem, dto, usuario, true);
        return new TremMestreRespostaDto(repositorio.save(trem));
    }

    @Transactional
    public TremMestreRespostaDto atualizar(Long id, TremMestreRequisicaoDto dto, String usuario) {
        TremMestre trem = buscar(id);
        aplicar(trem, dto, usuario, false);
        return new TremMestreRespostaDto(repositorio.save(trem));
    }

    @Transactional
    public TremMestreRespostaDto alterarSituacao(Long id, boolean ativo, String usuario) {
        TremMestre trem = buscar(id);
        trem.setAtivo(ativo);
        trem.setAlteradoPor(usuario(usuario));
        return new TremMestreRespostaDto(repositorio.save(trem));
    }

    @Transactional
    public void excluir(Long id) {
        TremMestre trem = buscar(id);
        if (visitaRepositorio.existsByTremMestreId(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "O trem possui visitas vinculadas e deve ser inativado em vez de excluído.");
        }
        repositorio.delete(trem);
    }

    private TremMestre buscar(Long id) {
        return repositorio.findWithComposicaoPadraoById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Trem não encontrado."));
    }

    private void aplicar(TremMestre trem, TremMestreRequisicaoDto dto, String usuario, boolean novo) {
        String identificador = obrigatorio(dto.getIdentificador(), "identificador", 40).toUpperCase(Locale.ROOT);
        String operadora = obrigatorio(dto.getOperadoraFerroviaria(), "operadora ferroviária", 80);
        String nome = obrigatorio(dto.getNomeOperacional(), "nome operacional", 120);
        boolean duplicado = novo
                ? repositorio.existsByOperadoraFerroviariaIgnoreCaseAndIdentificadorIgnoreCase(operadora, identificador)
                : repositorio.existsByOperadoraFerroviariaIgnoreCaseAndIdentificadorIgnoreCaseAndIdNot(
                        operadora, identificador, trem.getId());
        if (duplicado) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Já existe trem com este identificador para a operadora informada.");
        }
        trem.setIdentificador(identificador);
        trem.setOperadoraFerroviaria(operadora);
        trem.setNomeOperacional(nome);
        trem.setObservacoes(opcional(dto.getObservacoes(), 1000));
        trem.setAtivo(dto.isAtivo());
        trem.definirComposicaoPadrao(converter(dto.getComposicaoPadrao()));
        trem.setAlteradoPor(usuario(usuario));
        if (novo) {
            trem.setCriadoPor(usuario(usuario));
        }
    }

    private List<VagaoVisita> converter(List<VagaoVisitaRequisicaoDto> itens) {
        List<VagaoVisita> resultado = new ArrayList<>();
        Set<Integer> posicoes = new HashSet<>();
        Set<String> identificadores = new HashSet<>();
        if (itens == null) {
            return resultado;
        }
        for (VagaoVisitaRequisicaoDto item : itens) {
            if (item == null || item.getPosicaoNoTrem() == null || item.getPosicaoNoTrem() < 1) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A posição do vagão deve ser maior que zero.");
            }
            String identificador = obrigatorio(item.getIdentificadorVagao(), "identificador do vagão", 35)
                    .toUpperCase(Locale.ROOT);
            if (!posicoes.add(item.getPosicaoNoTrem()) || !identificadores.add(identificador)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "A composição padrão não pode repetir posição ou identificador de vagão.");
            }
            resultado.add(new VagaoVisita(item.getPosicaoNoTrem(), identificador, opcional(item.getTipoVagao(), 40)));
        }
        resultado.sort((a, b) -> a.getPosicaoNoTrem().compareTo(b.getPosicaoNoTrem()));
        return resultado;
    }

    private String obrigatorio(String valor, String campo, int maximo) {
        String limpo = sanitizadorEntrada.limparTexto(valor);
        if (!StringUtils.hasText(limpo)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O campo " + campo + " é obrigatório.");
        }
        limpo = limpo.trim();
        if (limpo.length() > maximo) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O campo " + campo + " excede o tamanho permitido.");
        }
        return limpo;
    }

    private String opcional(String valor, int maximo) {
        String limpo = sanitizadorEntrada.limparTexto(valor);
        if (!StringUtils.hasText(limpo)) {
            return null;
        }
        limpo = limpo.trim();
        if (limpo.length() > maximo) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O campo informado excede o tamanho permitido.");
        }
        return limpo;
    }

    private String usuario(String usuario) {
        return StringUtils.hasText(usuario) ? usuario : "sistema";
    }
}
