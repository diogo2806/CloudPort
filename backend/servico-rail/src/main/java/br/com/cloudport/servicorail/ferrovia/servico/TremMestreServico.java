package br.com.cloudport.servicorail.ferrovia.servico;

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

    public TremMestreServico(TremMestreRepositorio repositorio, VisitaTremRepositorio visitaRepositorio) {
        this.repositorio = repositorio; this.visitaRepositorio = visitaRepositorio;
    }

    @Transactional(readOnly = true)
    public List<TremMestreRespostaDto> listar() {
        return repositorio.findAllByOrderByAtivoDescOperadoraFerroviariaAscIdentificadorAsc().stream().map(TremMestreRespostaDto::new).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public TremMestreRespostaDto consultar(Long id) { return new TremMestreRespostaDto(buscar(id)); }

    @Transactional
    public TremMestreRespostaDto criar(TremMestreRequisicaoDto dto, String usuario) {
        TremMestre trem = new TremMestre(); aplicar(trem, dto, usuario, true); return new TremMestreRespostaDto(repositorio.save(trem));
    }

    @Transactional
    public TremMestreRespostaDto atualizar(Long id, TremMestreRequisicaoDto dto, String usuario) {
        TremMestre trem = buscar(id); aplicar(trem, dto, usuario, false); return new TremMestreRespostaDto(repositorio.save(trem));
    }

    @Transactional
    public TremMestreRespostaDto alterarSituacao(Long id, boolean ativo, String usuario) {
        TremMestre trem = buscar(id); trem.setAtivo(ativo); trem.setAtualizadoPor(usuario); return new TremMestreRespostaDto(repositorio.save(trem));
    }

    @Transactional
    public void excluir(Long id) {
        TremMestre trem = buscar(id);
        if (visitaRepositorio.existsByIdentificadorTremIgnoreCaseAndOperadoraFerroviariaIgnoreCase(trem.getIdentificador(), trem.getOperadoraFerroviaria())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "O trem possui visitas vinculadas e deve ser inativado.");
        }
        repositorio.delete(trem);
    }

    private TremMestre buscar(Long id) { return repositorio.findComposicaoById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Trem não encontrado.")); }

    private void aplicar(TremMestre trem, TremMestreRequisicaoDto dto, String usuario, boolean novo) {
        String identificador = obrigatorio(dto.getIdentificador(), "identificador", 40).toUpperCase(Locale.ROOT);
        String operadora = obrigatorio(dto.getOperadoraFerroviaria(), "operadora ferroviária", 80);
        boolean duplicado = novo ? repositorio.existsByOperadoraFerroviariaIgnoreCaseAndIdentificadorIgnoreCase(operadora, identificador)
                : repositorio.existsByOperadoraFerroviariaIgnoreCaseAndIdentificadorIgnoreCaseAndIdNot(operadora, identificador, trem.getId());
        if (duplicado) throw new ResponseStatusException(HttpStatus.CONFLICT, "Já existe um trem com este identificador para a operadora.");
        trem.setIdentificador(identificador); trem.setOperadoraFerroviaria(operadora); trem.setDescricao(opcional(dto.getDescricao(), 120));
        trem.setAtivo(dto.isAtivo()); trem.setObservacoes(opcional(dto.getObservacoes(), 500)); trem.setAtualizadoPor(usuario);
        if (novo) trem.setCriadoPor(usuario);
        trem.definirComposicaoPadrao(converter(dto.getComposicaoPadrao()));
    }

    private List<VagaoVisita> converter(List<VagaoVisitaRequisicaoDto> itens) {
        List<VagaoVisita> resultado = new ArrayList<>(); Set<String> ids = new HashSet<>(); Set<Integer> posicoes = new HashSet<>();
        if (itens == null) return resultado;
        for (VagaoVisitaRequisicaoDto item : itens) {
            String id = obrigatorio(item.getIdentificadorVagao(), "identificador do vagão", 35).toUpperCase(Locale.ROOT);
            Integer posicao = item.getPosicaoNoTrem();
            if (posicao == null || posicao < 1 || !ids.add(id) || !posicoes.add(posicao)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A composição possui vagão ou posição duplicada/inválida.");
            VagaoVisita vagao = new VagaoVisita(); vagao.setIdentificadorVagao(id); vagao.setPosicaoNoTrem(posicao); vagao.setTipoVagao(opcional(item.getTipoVagao(), 40)); resultado.add(vagao);
        }
        resultado.sort((a, b) -> a.getPosicaoNoTrem().compareTo(b.getPosicaoNoTrem())); return resultado;
    }

    private String obrigatorio(String valor, String campo, int max) { if (!StringUtils.hasText(valor)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O campo " + campo + " é obrigatório."); String v = valor.trim(); if (v.length() > max) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O campo " + campo + " excede o tamanho permitido."); return v; }
    private String opcional(String valor, int max) { if (!StringUtils.hasText(valor)) return null; String v = valor.trim(); if (v.length() > max) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Campo excede o tamanho permitido."); return v; }
}
