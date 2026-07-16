package br.com.cloudport.serviconaviosiderurgico.servico;

import br.com.cloudport.serviconaviosiderurgico.dominio.NavioSiderurgico;
import br.com.cloudport.serviconaviosiderurgico.dominio.StatusNavioSiderurgico;
import br.com.cloudport.serviconaviosiderurgico.dto.NavioSiderurgicoDTO;
import br.com.cloudport.serviconaviosiderurgico.porta.CadastroNavioPorta;
import br.com.cloudport.serviconaviosiderurgico.porta.NavioCanonico;
import br.com.cloudport.serviconaviosiderurgico.repositorio.NavioSiderurgicoRepositorio;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NavioSiderurgicoServico {

    private static final Logger LOGGER = LoggerFactory.getLogger(NavioSiderurgicoServico.class);

    private final NavioSiderurgicoRepositorio repositorio;
    private final CadastroNavioPorta cadastroNavioPorta;

    public NavioSiderurgicoServico(NavioSiderurgicoRepositorio repositorio,
                                   CadastroNavioPorta cadastroNavioPorta) {
        this.repositorio = repositorio;
        this.cadastroNavioPorta = cadastroNavioPorta;
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
        NavioCanonico canonico = dto.navioCadastroId() == null
                ? cadastroNavioPorta.buscarPorImo(dto.codigoImo())
                : cadastroNavioPorta.buscarPorId(dto.navioCadastroId());
        if (canonico == null || canonico.identificador() == null) {
            throw new IllegalArgumentException("O navio deve existir no cadastro canônico antes de receber dados siderúrgicos.");
        }
        if (repositorio.existsByNavioCadastroId(canonico.identificador())) {
            throw new IllegalArgumentException("O navio canônico já possui extensão operacional siderúrgica.");
        }
        if (repositorio.existsByCodigoImoIgnoreCase(canonico.codigoImo())) {
            throw new IllegalArgumentException("Já existe navio siderúrgico com este IMO.");
        }

        NavioSiderurgico navio = new NavioSiderurgico();
        preencherDadosCanonicos(navio, canonico);
        preencherDadosOperacionais(navio, dto);
        return NavioSiderurgicoDTO.de(repositorio.save(navio));
    }

    @Transactional
    public boolean sincronizarCadastroCanonico(Long navioCadastroId) {
        return repositorio.findByNavioCadastroId(navioCadastroId)
                .map(this::sincronizar)
                .orElse(false);
    }

    @Transactional
    public boolean cancelarPorCadastroRemovido(Long navioCadastroId) {
        return repositorio.findByNavioCadastroId(navioCadastroId)
                .map(navio -> {
                    if (navio.getStatus() == StatusNavioSiderurgico.CANCELADO) {
                        return false;
                    }
                    navio.setStatus(StatusNavioSiderurgico.CANCELADO);
                    repositorio.save(navio);
                    LOGGER.info("Projecao siderurgica {} cancelada apos remocao do navio canonico {}.",
                            navio.getId(), navioCadastroId);
                    return true;
                })
                .orElse(false);
    }

    @Transactional
    public int reconciliarCadastrosDesatualizados(LocalDateTime limiteAtualizacao) {
        int atualizados = 0;
        List<NavioSiderurgico> candidatos = repositorio
                .findTop100ByAtualizadoEmBeforeAndStatusNotOrderByAtualizadoEmAsc(
                        limiteAtualizacao,
                        StatusNavioSiderurgico.CANCELADO
                );
        for (NavioSiderurgico navio : candidatos) {
            if (sincronizarComTratamento(navio)) {
                atualizados++;
            }
        }
        return atualizados;
    }

    private boolean sincronizar(NavioSiderurgico navio) {
        NavioCanonico canonico = navio.getNavioCadastroId() == null
                ? cadastroNavioPorta.buscarPorImo(navio.getCodigoImo())
                : cadastroNavioPorta.buscarPorId(navio.getNavioCadastroId());
        if (!atualizarDadosCanonicosSeNecessario(navio, canonico)) {
            return false;
        }
        repositorio.save(navio);
        LOGGER.info("Projecao siderurgica do navio {} sincronizada com o cadastro canonico {}.",
                navio.getId(), canonico.identificador());
        return true;
    }

    private boolean sincronizarComTratamento(NavioSiderurgico navio) {
        try {
            return sincronizar(navio);
        } catch (RuntimeException ex) {
            LOGGER.warn("Nao foi possivel reconciliar o navio siderurgico {} com o cadastro canonico: {}",
                    navio.getId(), ex.getMessage());
            return false;
        }
    }

    private boolean atualizarDadosCanonicosSeNecessario(NavioSiderurgico navio, NavioCanonico canonico) {
        if (canonico == null || canonico.identificador() == null) {
            throw new IllegalArgumentException("Cadastro canonico do navio nao encontrado.");
        }
        boolean alterado = !Objects.equals(navio.getNavioCadastroId(), canonico.identificador())
                || !Objects.equals(navio.getNome(), canonico.nome())
                || !Objects.equals(navio.getCodigoImo(), canonico.codigoImo())
                || !Objects.equals(navio.getPaisBandeira(), canonico.paisBandeira())
                || !Objects.equals(navio.getEmpresaArmadora(), canonico.empresaArmadora())
                || !Objects.equals(navio.getLoaMetros(), canonico.loaMetros());
        if (alterado) {
            preencherDadosCanonicos(navio, canonico);
        }
        return alterado;
    }

    private void preencherDadosCanonicos(NavioSiderurgico navio, NavioCanonico canonico) {
        navio.setNavioCadastroId(canonico.identificador());
        navio.setNome(canonico.nome().trim());
        navio.setCodigoImo(canonico.codigoImo().trim().toUpperCase());
        navio.setPaisBandeira(canonico.paisBandeira().trim());
        navio.setEmpresaArmadora(canonico.empresaArmadora().trim());
        navio.setLoaMetros(canonico.loaMetros());
    }

    private void preencherDadosOperacionais(NavioSiderurgico navio, NavioSiderurgicoDTO dto) {
        navio.setTipoNavio(dto.tipoNavio().trim());
        navio.setDwtToneladas(dto.dwtToneladas());
        navio.setQuantidadePoroes(dto.quantidadePoroes());
        navio.setStatus(dto.status() == null ? StatusNavioSiderurgico.PLANEJADO : dto.status());
    }
}
