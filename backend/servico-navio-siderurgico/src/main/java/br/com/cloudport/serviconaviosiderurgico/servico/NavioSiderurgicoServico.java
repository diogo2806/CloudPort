package br.com.cloudport.serviconaviosiderurgico.servico;

import br.com.cloudport.serviconaviosiderurgico.dominio.NavioSiderurgico;
import br.com.cloudport.serviconaviosiderurgico.dominio.StatusNavioSiderurgico;
import br.com.cloudport.serviconaviosiderurgico.dto.NavioSiderurgicoDTO;
import br.com.cloudport.serviconaviosiderurgico.repositorio.NavioSiderurgicoRepositorio;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NavioSiderurgicoServico {

    private final NavioSiderurgicoRepositorio repositorio;

    public NavioSiderurgicoServico(NavioSiderurgicoRepositorio repositorio) {
        this.repositorio = repositorio;
    }

    @Transactional(readOnly = true)
    public List<NavioSiderurgicoDTO> listar() {
        return repositorio.findAll().stream().map(NavioSiderurgicoDTO::de).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public NavioSiderurgico buscarEntidade(Long id) {
        return repositorio.findById(id).orElseThrow(() -> new IllegalArgumentException("Navio siderurgico nao encontrado."));
    }

    @Transactional
    public NavioSiderurgicoDTO criar(NavioSiderurgicoDTO dto) {
        String imo = normalizar(dto.codigoImo());
        if (repositorio.existsByCodigoImoIgnoreCase(imo)) {
            throw new IllegalArgumentException("Ja existe navio siderurgico com este IMO.");
        }
        NavioSiderurgico navio = new NavioSiderurgico();
        preencher(navio, dto, imo);
        return NavioSiderurgicoDTO.de(repositorio.save(navio));
    }

    private void preencher(NavioSiderurgico navio, NavioSiderurgicoDTO dto, String imo) {
        navio.setNome(dto.nome().trim());
        navio.setCodigoImo(imo);
        navio.setPaisBandeira(dto.paisBandeira().trim());
        navio.setEmpresaArmadora(dto.empresaArmadora().trim());
        navio.setTipoNavio(dto.tipoNavio().trim());
        navio.setLoaMetros(dto.loaMetros());
        navio.setDwtToneladas(dto.dwtToneladas());
        navio.setQuantidadePoroes(dto.quantidadePoroes());
        navio.setStatus(dto.status() == null ? StatusNavioSiderurgico.PLANEJADO : dto.status());
    }

    private String normalizar(String valor) {
        return valor.trim().toUpperCase(Locale.ROOT);
    }
}
