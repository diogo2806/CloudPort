package br.com.cloudport.serviconaviosiderurgico.servico;

import br.com.cloudport.serviconaviosiderurgico.cliente.NavioCadastroCliente;
import br.com.cloudport.serviconaviosiderurgico.cliente.NavioCadastroCliente.NavioCanonicoDTO;
import br.com.cloudport.serviconaviosiderurgico.dominio.NavioSiderurgico;
import br.com.cloudport.serviconaviosiderurgico.dominio.StatusNavioSiderurgico;
import br.com.cloudport.serviconaviosiderurgico.dto.NavioSiderurgicoDTO;
import br.com.cloudport.serviconaviosiderurgico.repositorio.NavioSiderurgicoRepositorio;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NavioSiderurgicoServico {

    private static final Logger LOGGER = LoggerFactory.getLogger(NavioSiderurgicoServico.class);

    private final NavioSiderurgicoRepositorio repositorio;
    private final NavioCadastroCliente navioCadastroCliente;

    public NavioSiderurgicoServico(NavioSiderurgicoRepositorio repositorio,
                                    NavioCadastroCliente navioCadastroCliente) {
        this.repositorio = repositorio;
        this.navioCadastroCliente = navioCadastroCliente;
    }

    @Transactional(readOnly = true)
    public List<NavioSiderurgicoDTO> listar() {
        return repositorio.findAll().stream()
                .map(NavioSiderurgicoDTO::de)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public NavioSiderurgico buscarEntidade(Long id) {
        return repositorio.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Navio siderurgico nao encontrado."));
    }

    @Transactional
    public NavioSiderurgicoDTO criar(NavioSiderurgicoDTO dto) {
        NavioCanonicoDTO canonico = dto.navioCadastroId() == null
                ? navioCadastroCliente.buscarPorImo(dto.codigoImo())
                : navioCadastroCliente.buscarPorId(dto.navioCadastroId());
        if (canonico == null || canonico.getIdentificador() == null) {
            throw new IllegalArgumentException("O navio deve existir no cadastro canônico antes de receber dados siderúrgicos.");
        }
        if (repositorio.existsByNavioCadastroId(canonico.getIdentificador())) {
            throw new IllegalArgumentException("O navio canônico já possui extensão operacional siderúrgica.");
        }
        if (repositorio.existsByCodigoImoIgnoreCase(canonico.getCodigoImo())) {
            throw new IllegalArgumentException("Já existe navio siderúrgico com este IMO.");
        }

        NavioSiderurgico navio = new NavioSiderurgico();
        preencherDadosCanonicos(navio, canonico);
        preencherDadosOperacionais(navio, dto);
        return NavioSiderurgicoDTO.de(repositorio.save(navio));
    }

    @Scheduled(fixedDelayString = "${cloudport.integracao.navio.sincronizacao-ms:300000}")
    @Transactional
    public void sincronizarCadastroCanonico() {
        repositorio.findAll().forEach(this::sincronizarComTratamento);
    }

    private void sincronizarComTratamento(NavioSiderurgico navio) {
        try {
            NavioCanonicoDTO canonico = navio.getNavioCadastroId() == null
                    ? navioCadastroCliente.buscarPorImo(navio.getCodigoImo())
                    : navioCadastroCliente.buscarPorId(navio.getNavioCadastroId());
            if (atualizarDadosCanonicosSeNecessario(navio, canonico)) {
                repositorio.save(navio);
                LOGGER.info("Projeção siderúrgica do navio {} sincronizada com o cadastro canônico {}.",
                        navio.getId(), canonico.getIdentificador());
            }
        } catch (RuntimeException ex) {
            LOGGER.warn("Não foi possível sincronizar o navio siderúrgico {} com o cadastro canônico: {}",
                    navio.getId(), ex.getMessage());
        }
    }

    private boolean atualizarDadosCanonicosSeNecessario(NavioSiderurgico navio, NavioCanonicoDTO canonico) {
        boolean alterado = !Objects.equals(navio.getNavioCadastroId(), canonico.getIdentificador())
                || !Objects.equals(navio.getNome(), canonico.getNome())
                || !Objects.equals(navio.getCodigoImo(), canonico.getCodigoImo())
                || !Objects.equals(navio.getPaisBandeira(), canonico.getPaisBandeira())
                || !Objects.equals(navio.getEmpresaArmadora(), canonico.getEmpresaArmadora())
                || !Objects.equals(navio.getLoaMetros(), canonico.getLoaMetros());
        if (alterado) {
            preencherDadosCanonicos(navio, canonico);
        }
        return alterado;
    }

    private void preencherDadosCanonicos(NavioSiderurgico navio, NavioCanonicoDTO canonico) {
        navio.setNavioCadastroId(canonico.getIdentificador());
        navio.setNome(canonico.getNome().trim());
        navio.setCodigoImo(canonico.getCodigoImo().trim().toUpperCase());
        navio.setPaisBandeira(canonico.getPaisBandeira().trim());
        navio.setEmpresaArmadora(canonico.getEmpresaArmadora().trim());
        navio.setLoaMetros(canonico.getLoaMetros());
    }

    private void preencherDadosOperacionais(NavioSiderurgico navio, NavioSiderurgicoDTO dto) {
        navio.setTipoNavio(dto.tipoNavio().trim());
        navio.setDwtToneladas(dto.dwtToneladas());
        navio.setQuantidadePoroes(dto.quantidadePoroes());
        navio.setStatus(dto.status() == null ? StatusNavioSiderurgico.PLANEJADO : dto.status());
    }
}
