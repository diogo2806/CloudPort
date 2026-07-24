package br.com.cloudport.servicocargageral.servico;

import br.com.cloudport.servicocargageral.dominio.AuditoriaVinculoEmpresaCarga;
import br.com.cloudport.servicocargageral.dominio.Empresa;
import br.com.cloudport.servicocargageral.dominio.Empresa.PapelEmpresa;
import br.com.cloudport.servicocargageral.dominio.VinculoEmpresaCarga;
import br.com.cloudport.servicocargageral.dominio.VinculoEmpresaCarga.TipoRecursoCarga;
import br.com.cloudport.servicocargageral.dto.VinculoEmpresaCargaDTOs.AtualizarVinculosRequest;
import br.com.cloudport.servicocargageral.dto.VinculoEmpresaCargaDTOs.VinculoEmpresaRequest;
import br.com.cloudport.servicocargageral.dto.VinculoEmpresaCargaDTOs.VinculoEmpresaResposta;
import br.com.cloudport.servicocargageral.repositorio.AuditoriaVinculoEmpresaCargaRepositorio;
import br.com.cloudport.servicocargageral.repositorio.ConhecimentoCargaRepositorio;
import br.com.cloudport.servicocargageral.repositorio.EmpresaRepositorio;
import br.com.cloudport.servicocargageral.repositorio.LoteCargaRepositorio;
import br.com.cloudport.servicocargageral.repositorio.VinculoEmpresaCargaRepositorio;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class VinculoEmpresaCargaServico {

    private static final Set<PapelEmpresa> PAPEIS_CONHECIMENTO =
            EnumSet.allOf(PapelEmpresa.class);
    private static final Set<PapelEmpresa> PAPEIS_LOTE = EnumSet.of(
            PapelEmpresa.CLIENTE,
            PapelEmpresa.DONO_CARGA,
            PapelEmpresa.OPERADOR,
            PapelEmpresa.TRANSPORTADORA);

    private final VinculoEmpresaCargaRepositorio vinculoRepositorio;
    private final AuditoriaVinculoEmpresaCargaRepositorio auditoriaRepositorio;
    private final EmpresaRepositorio empresaRepositorio;
    private final ConhecimentoCargaRepositorio conhecimentoRepositorio;
    private final LoteCargaRepositorio loteRepositorio;

    public VinculoEmpresaCargaServico(
            VinculoEmpresaCargaRepositorio vinculoRepositorio,
            AuditoriaVinculoEmpresaCargaRepositorio auditoriaRepositorio,
            EmpresaRepositorio empresaRepositorio,
            ConhecimentoCargaRepositorio conhecimentoRepositorio,
            LoteCargaRepositorio loteRepositorio) {
        this.vinculoRepositorio = vinculoRepositorio;
        this.auditoriaRepositorio = auditoriaRepositorio;
        this.empresaRepositorio = empresaRepositorio;
        this.conhecimentoRepositorio = conhecimentoRepositorio;
        this.loteRepositorio = loteRepositorio;
    }

    @Transactional(readOnly = true)
    public List<VinculoEmpresaResposta> listarConhecimento(UUID conhecimentoId) {
        validarRecurso(TipoRecursoCarga.CONHECIMENTO, conhecimentoId);
        return listar(TipoRecursoCarga.CONHECIMENTO, conhecimentoId);
    }

    @Transactional
    public List<VinculoEmpresaResposta> atualizarConhecimento(
            UUID conhecimentoId,
            AtualizarVinculosRequest request) {
        return atualizar(TipoRecursoCarga.CONHECIMENTO, conhecimentoId, request, PAPEIS_CONHECIMENTO);
    }

    @Transactional(readOnly = true)
    public List<VinculoEmpresaResposta> listarLote(UUID loteId) {
        validarRecurso(TipoRecursoCarga.LOTE, loteId);
        return listar(TipoRecursoCarga.LOTE, loteId);
    }

    @Transactional
    public List<VinculoEmpresaResposta> atualizarLote(
            UUID loteId,
            AtualizarVinculosRequest request) {
        return atualizar(TipoRecursoCarga.LOTE, loteId, request, PAPEIS_LOTE);
    }

    private List<VinculoEmpresaResposta> atualizar(
            TipoRecursoCarga tipoRecurso,
            UUID recursoId,
            AtualizarVinculosRequest request,
            Set<PapelEmpresa> papeisPermitidos) {
        validarRecurso(tipoRecurso, recursoId);
        List<VinculoEmpresaRequest> solicitados = request.vinculos() == null
                ? List.of()
                : request.vinculos();
        validarPapeis(solicitados, papeisPermitidos, tipoRecurso);

        Map<PapelEmpresa, VinculoEmpresaCarga> existentes = vinculoRepositorio
                .findByTipoRecursoAndRecursoIdOrderByPapelAsc(tipoRecurso, recursoId)
                .stream()
                .collect(Collectors.toMap(
                        VinculoEmpresaCarga::getPapel,
                        item -> item,
                        (primeiro, segundo) -> primeiro,
                        () -> new EnumMap<>(PapelEmpresa.class)));
        String usuario = usuarioAtual();

        for (VinculoEmpresaRequest solicitado : solicitados) {
            Empresa empresa = validarEmpresa(solicitado.empresaId(), solicitado.papel());
            VinculoEmpresaCarga vinculo = existentes.remove(solicitado.papel());
            if (vinculo == null) {
                vinculo = VinculoEmpresaCarga.criar(
                        tipoRecurso,
                        recursoId,
                        solicitado.papel(),
                        empresa,
                        usuario);
                vinculoRepositorio.save(vinculo);
                auditar(tipoRecurso, recursoId, solicitado.papel(), null, empresa.getId(), "INCLUSAO", usuario);
            } else if (!vinculo.getEmpresa().getId().equals(empresa.getId())) {
                UUID empresaAnteriorId = vinculo.getEmpresa().getId();
                vinculo.alterarEmpresa(empresa, usuario);
                vinculoRepositorio.save(vinculo);
                auditar(
                        tipoRecurso,
                        recursoId,
                        solicitado.papel(),
                        empresaAnteriorId,
                        empresa.getId(),
                        "ALTERACAO",
                        usuario);
            }
        }

        for (VinculoEmpresaCarga removido : existentes.values()) {
            vinculoRepositorio.delete(removido);
            auditar(
                    tipoRecurso,
                    recursoId,
                    removido.getPapel(),
                    removido.getEmpresa().getId(),
                    null,
                    "REMOCAO",
                    usuario);
        }

        return listar(tipoRecurso, recursoId);
    }

    private List<VinculoEmpresaResposta> listar(TipoRecursoCarga tipoRecurso, UUID recursoId) {
        return vinculoRepositorio.findByTipoRecursoAndRecursoIdOrderByPapelAsc(tipoRecurso, recursoId)
                .stream()
                .map(this::mapear)
                .collect(Collectors.toList());
    }

    private void validarPapeis(
            List<VinculoEmpresaRequest> solicitados,
            Set<PapelEmpresa> papeisPermitidos,
            TipoRecursoCarga tipoRecurso) {
        Set<PapelEmpresa> encontrados = new HashSet<>();
        for (VinculoEmpresaRequest solicitado : solicitados) {
            if (!papeisPermitidos.contains(solicitado.papel())) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "O papel " + solicitado.papel() + " não é aplicável ao recurso " + tipoRecurso + ".");
            }
            if (!encontrados.add(solicitado.papel())) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "O papel " + solicitado.papel() + " foi informado mais de uma vez.");
            }
        }
    }

    private Empresa validarEmpresa(UUID empresaId, PapelEmpresa papel) {
        Empresa empresa = empresaRepositorio.findById(empresaId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Empresa não encontrada para o papel " + papel + "."));
        if (!empresa.isAtivo()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "A empresa " + empresa.getCodigo() + " está inativa e não pode ser vinculada como " + papel + ".");
        }
        if (!empresa.getPapeis().contains(papel)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "A empresa " + empresa.getCodigo() + " não possui o papel " + papel + ".");
        }
        return empresa;
    }

    private void validarRecurso(TipoRecursoCarga tipoRecurso, UUID recursoId) {
        boolean existe = tipoRecurso == TipoRecursoCarga.CONHECIMENTO
                ? conhecimentoRepositorio.existsById(recursoId)
                : loteRepositorio.existsById(recursoId);
        if (!existe) {
            String nome = tipoRecurso == TipoRecursoCarga.CONHECIMENTO ? "Bill of Lading" : "Cargo lot";
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, nome + " não encontrado.");
        }
    }

    private void auditar(
            TipoRecursoCarga tipoRecurso,
            UUID recursoId,
            PapelEmpresa papel,
            UUID empresaAnteriorId,
            UUID empresaNovaId,
            String acao,
            String usuario) {
        auditoriaRepositorio.save(AuditoriaVinculoEmpresaCarga.registrar(
                tipoRecurso,
                recursoId,
                papel,
                empresaAnteriorId,
                empresaNovaId,
                acao,
                usuario));
    }

    private VinculoEmpresaResposta mapear(VinculoEmpresaCarga vinculo) {
        Empresa empresa = vinculo.getEmpresa();
        return new VinculoEmpresaResposta(
                vinculo.getPapel(),
                empresa.getId(),
                empresa.getCodigo(),
                empresa.getRazaoSocial(),
                empresa.getNomeFantasia(),
                empresa.isAtivo(),
                Set.copyOf(empresa.getPapeis()),
                vinculo.getVinculadoPor(),
                vinculo.getAtualizadoEm());
    }

    private String usuarioAtual() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null || authentication.getName().isBlank()) {
            return "sistema";
        }
        return authentication.getName();
    }
}
