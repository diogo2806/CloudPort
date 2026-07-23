package br.com.cloudport.servicorail.ferrovia.servico;

import br.com.cloudport.servicorail.ferrovia.dto.TremCadastroDto;
import br.com.cloudport.servicorail.ferrovia.dto.VagaoVisitaRequisicaoDto;
import br.com.cloudport.servicorail.ferrovia.modelo.TremCadastro;
import br.com.cloudport.servicorail.ferrovia.modelo.VagaoVisita;
import br.com.cloudport.servicorail.ferrovia.repositorio.TremCadastroRepositorio;
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
public class TremCadastroServico {
    private final TremCadastroRepositorio repositorio;

    public TremCadastroServico(TremCadastroRepositorio repositorio) { this.repositorio = repositorio; }

    @Transactional(readOnly = true)
    public List<TremCadastroDto> listar() {
        return repositorio.findAllByOrderByOperadoraFerroviariaAscIdentificadorAsc().stream().map(TremCadastroDto::deEntidade).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public TremCadastroDto consultar(Long id) { return TremCadastroDto.deEntidade(buscar(id)); }

    @Transactional
    public TremCadastroDto criar(TremCadastroDto dto, String usuario) {
        String identificador = obrigatorio(dto.getIdentificador(), "identificador").toUpperCase(Locale.ROOT);
        String operadora = obrigatorio(dto.getOperadoraFerroviaria(), "operadora ferroviária");
        if (repositorio.existsByOperadoraFerroviariaIgnoreCaseAndIdentificadorIgnoreCase(operadora, identificador)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Já existe um trem com esse identificador para a operadora.");
        }
        TremCadastro entidade = new TremCadastro();
        entidade.setCriadoPor(usuario);
        aplicar(entidade, dto, identificador, operadora, usuario);
        return TremCadastroDto.deEntidade(repositorio.save(entidade));
    }

    @Transactional
    public TremCadastroDto atualizar(Long id, TremCadastroDto dto, String usuario) {
        TremCadastro entidade = buscar(id);
        String identificador = obrigatorio(dto.getIdentificador(), "identificador").toUpperCase(Locale.ROOT);
        String operadora = obrigatorio(dto.getOperadoraFerroviaria(), "operadora ferroviária");
        if (repositorio.existsByOperadoraFerroviariaIgnoreCaseAndIdentificadorIgnoreCaseAndIdNot(operadora, identificador, id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Já existe um trem com esse identificador para a operadora.");
        }
        aplicar(entidade, dto, identificador, operadora, usuario);
        return TremCadastroDto.deEntidade(repositorio.save(entidade));
    }

    @Transactional
    public TremCadastroDto alterarSituacao(Long id, boolean ativo, String usuario) {
        TremCadastro entidade = buscar(id);
        entidade.setAtivo(ativo);
        entidade.setAlteradoPor(usuario);
        return TremCadastroDto.deEntidade(repositorio.save(entidade));
    }

    private void aplicar(TremCadastro entidade, TremCadastroDto dto, String identificador, String operadora, String usuario) {
        entidade.setIdentificador(identificador);
        entidade.setOperadoraFerroviaria(operadora);
        entidade.setDescricao(opcional(dto.getDescricao(), 120));
        entidade.setObservacoes(opcional(dto.getObservacoes(), 500));
        entidade.setAtivo(dto.isAtivo());
        entidade.setAlteradoPor(usuario);
        entidade.definirComposicaoPadrao(converter(dto.getComposicaoPadrao()));
    }

    private List<VagaoVisita> converter(List<VagaoVisitaRequisicaoDto> itens) {
        List<VagaoVisita> resultado = new ArrayList<>();
        Set<String> ids = new HashSet<>();
        if (itens == null) return resultado;
        int posicao = 1;
        for (VagaoVisitaRequisicaoDto dto : itens) {
            if (dto == null) continue;
            String id = obrigatorio(dto.getIdentificadorVagao(), "identificador do vagão").toUpperCase(Locale.ROOT);
            if (!ids.add(id)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A composição padrão não pode repetir vagões.");
            VagaoVisita vagao = new VagaoVisita();
            vagao.setIdentificadorVagao(id);
            vagao.setTipoVagao(opcional(dto.getTipoVagao(), 40));
            vagao.setPosicaoNoTrem(posicao++);
            resultado.add(vagao);
        }
        return resultado;
    }

    private TremCadastro buscar(Long id) {
        return repositorio.findWithComposicaoPadraoById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Trem cadastrado não encontrado."));
    }

    private String obrigatorio(String valor, String campo) {
        if (!StringUtils.hasText(valor)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O campo " + campo + " é obrigatório.");
        return valor.trim();
    }

    private String opcional(String valor, int limite) {
        if (!StringUtils.hasText(valor)) return null;
        String texto = valor.trim();
        if (texto.length() > limite) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Campo excede o limite de " + limite + " caracteres.");
        return texto;
    }
}
