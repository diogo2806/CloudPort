package br.com.cloudport.servicorail.ferrovia.inspecao.servico;

import br.com.cloudport.servicorail.ferrovia.inspecao.dto.InspecaoVagaoDto.Defeito;
import br.com.cloudport.servicorail.ferrovia.inspecao.dto.InspecaoVagaoDto.LiberacaoOverride;
import br.com.cloudport.servicorail.ferrovia.inspecao.dto.InspecaoVagaoDto.Registro;
import br.com.cloudport.servicorail.ferrovia.inspecao.dto.InspecaoVagaoDto.Resposta;
import br.com.cloudport.servicorail.ferrovia.inspecao.modelo.DefeitoInspecaoVagao;
import br.com.cloudport.servicorail.ferrovia.inspecao.modelo.InspecaoVagao;
import br.com.cloudport.servicorail.ferrovia.inspecao.modelo.InspecaoVagao.StatusInspecaoVagao;
import br.com.cloudport.servicorail.ferrovia.inspecao.repositorio.InspecaoVagaoRepositorio;
import br.com.cloudport.servicorail.ferrovia.listatrabalho.dto.OrdemMovimentacaoRespostaDto;
import br.com.cloudport.servicorail.ferrovia.listatrabalho.modelo.OrdemMovimentacao;
import br.com.cloudport.servicorail.ferrovia.listatrabalho.modelo.TipoMovimentacaoOrdem;
import br.com.cloudport.servicorail.ferrovia.listatrabalho.repositorio.OrdemMovimentacaoRepositorio;
import br.com.cloudport.servicorail.ferrovia.modelo.OperacaoConteinerVisita;
import br.com.cloudport.servicorail.ferrovia.modelo.VagaoVisita;
import br.com.cloudport.servicorail.ferrovia.modelo.VisitaTrem;
import br.com.cloudport.servicorail.ferrovia.repositorio.VisitaTremRepositorio;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

@Service
public class InspecaoVagaoServico {

    private final InspecaoVagaoRepositorio inspecaoRepositorio;
    private final VisitaTremRepositorio visitaTremRepositorio;
    private final OrdemMovimentacaoRepositorio ordemMovimentacaoRepositorio;

    public InspecaoVagaoServico(InspecaoVagaoRepositorio inspecaoRepositorio,
                                VisitaTremRepositorio visitaTremRepositorio,
                                OrdemMovimentacaoRepositorio ordemMovimentacaoRepositorio) {
        this.inspecaoRepositorio = inspecaoRepositorio;
        this.visitaTremRepositorio = visitaTremRepositorio;
        this.ordemMovimentacaoRepositorio = ordemMovimentacaoRepositorio;
    }

    @Transactional(readOnly = true)
    public List<Resposta> listar(Long idVisita) {
        VisitaTrem visita = buscarVisita(idVisita);
        return inspecaoRepositorio.findByVisitaTremIdOrderByInspecionadoEmDesc(visita.getId())
                .stream()
                .map(Resposta::deEntidade)
                .collect(Collectors.toList());
    }

    @Transactional
    public Resposta registrar(Long idVisita, Registro dto) {
        VisitaTrem visita = buscarVisita(idVisita);
        if (dto == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Os dados da inspeção devem ser informados.");
        }
        String identificadorVagao = normalizarMaiusculo(dto.getIdentificadorVagao());
        validarVagaoNaComposicao(visita, identificadorVagao);

        InspecaoVagao inspecao = new InspecaoVagao();
        inspecao.setVisitaTrem(visita);
        inspecao.setIdentificadorVagao(identificadorVagao);
        inspecao.setRodasAprovadas(dto.getRodasAprovadas());
        inspecao.setFreiosAprovados(dto.getFreiosAprovados());
        inspecao.setEngatesAprovados(dto.getEngatesAprovados());
        inspecao.setEstruturaAprovada(dto.getEstruturaAprovada());
        inspecao.setLacresAprovados(dto.getLacresAprovados());
        inspecao.setResponsavel(normalizar(dto.getResponsavel()));
        inspecao.setObservacao(normalizar(dto.getObservacao()));
        inspecao.setInspecionadoEm(LocalDateTime.now());
        inspecao.definirDefeitos(converterDefeitos(dto.getDefeitos()));
        inspecao.setStatus(inspecaoAprovada(inspecao)
                ? StatusInspecaoVagao.APROVADA
                : StatusInspecaoVagao.REPROVADA);
        return Resposta.deEntidade(inspecaoRepositorio.save(inspecao));
    }

    @Transactional
    public Resposta liberarOverride(Long idVisita,
                                    Long idInspecao,
                                    LiberacaoOverride dto) {
        validarId(idVisita, "visita");
        validarId(idInspecao, "inspeção");
        if (dto == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Os dados da liberação devem ser informados.");
        }
        InspecaoVagao inspecao = inspecaoRepositorio.findByIdAndVisitaTremId(idInspecao, idVisita)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Inspeção de vagão não encontrada para a visita informada."));
        if (inspecao.getStatus() != StatusInspecaoVagao.REPROVADA) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Somente inspeções reprovadas podem ser liberadas por override.");
        }
        inspecao.setStatus(StatusInspecaoVagao.LIBERADA_OVERRIDE);
        inspecao.setOverridePor(normalizar(dto.getResponsavel()));
        inspecao.setOverrideMotivo(normalizar(dto.getMotivo()));
        inspecao.setLiberadoEm(LocalDateTime.now());
        return Resposta.deEntidade(inspecaoRepositorio.save(inspecao));
    }

    @Transactional(readOnly = true)
    public List<OrdemMovimentacaoRespostaDto> filtrarOrdensElegiveis(
            Long idVisita,
            List<OrdemMovimentacaoRespostaDto> ordens) {
        VisitaTrem visita = buscarVisita(idVisita);
        if (ordens == null || ordens.isEmpty()) {
            return Collections.emptyList();
        }
        return ordens.stream()
                .filter(ordem -> ordemElegivel(visita,
                        ordem.getIdentificadorVagao(),
                        ordem.getCodigoConteiner(),
                        ordem.getTipoMovimentacao()))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public void validarElegibilidadeOrdem(Long idVisita, Long idOrdem) {
        validarId(idVisita, "visita");
        validarId(idOrdem, "ordem");
        OrdemMovimentacao ordem = ordemMovimentacaoRepositorio.findByIdAndVisitaTremId(idOrdem, idVisita)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Ordem de movimentação não encontrada para a visita informada."));
        VisitaTrem visita = buscarVisita(idVisita);
        String vagao = resolverIdentificadorVagao(visita,
                ordem.getIdentificadorVagao(),
                ordem.getCodigoConteiner(),
                ordem.getTipoMovimentacao());
        if (!StringUtils.hasText(vagao)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "A ordem não possui vagão associado e não pode ser liberada para execução.");
        }
        InspecaoVagao inspecao = ultimaInspecaoAprovadaOuFalhar(visita.getId(), vagao);
        if (!statusLiberaOperacao(inspecao.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    String.format(Locale.ROOT,
                            "O vagão %s está reprovado e não pode executar carga ou descarga sem override autorizado.",
                            vagao));
        }
    }

    private boolean ordemElegivel(VisitaTrem visita,
                                   String vagaoOrdem,
                                   String codigoConteiner,
                                   TipoMovimentacaoOrdem tipoMovimentacao) {
        String vagao = resolverIdentificadorVagao(visita, vagaoOrdem, codigoConteiner, tipoMovimentacao);
        if (!StringUtils.hasText(vagao)) {
            return false;
        }
        return inspecaoRepositorio
                .findFirstByVisitaTremIdAndIdentificadorVagaoIgnoreCaseOrderByInspecionadoEmDesc(
                        visita.getId(), vagao)
                .map(InspecaoVagao::getStatus)
                .map(this::statusLiberaOperacao)
                .orElse(false);
    }

    private InspecaoVagao ultimaInspecaoAprovadaOuFalhar(Long idVisita, String vagao) {
        return inspecaoRepositorio
                .findFirstByVisitaTremIdAndIdentificadorVagaoIgnoreCaseOrderByInspecionadoEmDesc(idVisita, vagao)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT,
                        String.format(Locale.ROOT,
                                "O vagão %s ainda não possui inspeção registrada.", vagao)));
    }

    private boolean statusLiberaOperacao(StatusInspecaoVagao status) {
        return status == StatusInspecaoVagao.APROVADA
                || status == StatusInspecaoVagao.LIBERADA_OVERRIDE;
    }

    private String resolverIdentificadorVagao(VisitaTrem visita,
                                               String vagaoOrdem,
                                               String codigoConteiner,
                                               TipoMovimentacaoOrdem tipoMovimentacao) {
        if (StringUtils.hasText(vagaoOrdem)) {
            return normalizarMaiusculo(vagaoOrdem);
        }
        List<OperacaoConteinerVisita> operacoes = tipoMovimentacao == TipoMovimentacaoOrdem.DESCARGA_TREM
                ? visita.getListaDescarga()
                : visita.getListaCarga();
        return Optional.ofNullable(operacoes)
                .orElseGet(ArrayList::new)
                .stream()
                .filter(item -> codigoConteiner != null
                        && codigoConteiner.equalsIgnoreCase(item.getCodigoConteiner()))
                .map(OperacaoConteinerVisita::getIdentificadorVagao)
                .filter(StringUtils::hasText)
                .map(this::normalizarMaiusculo)
                .findFirst()
                .orElse(null);
    }

    private List<DefeitoInspecaoVagao> converterDefeitos(List<Defeito> defeitos) {
        if (defeitos == null) {
            return Collections.emptyList();
        }
        return defeitos.stream()
                .map(defeito -> new DefeitoInspecaoVagao(
                        normalizarMaiusculo(defeito.getCodigo()),
                        normalizar(defeito.getDescricao()),
                        defeito.getSeveridade(),
                        normalizar(defeito.getEvidencia())))
                .collect(Collectors.toList());
    }

    private boolean inspecaoAprovada(InspecaoVagao inspecao) {
        return Boolean.TRUE.equals(inspecao.getRodasAprovadas())
                && Boolean.TRUE.equals(inspecao.getFreiosAprovados())
                && Boolean.TRUE.equals(inspecao.getEngatesAprovados())
                && Boolean.TRUE.equals(inspecao.getEstruturaAprovada())
                && Boolean.TRUE.equals(inspecao.getLacresAprovados())
                && (inspecao.getDefeitos() == null || inspecao.getDefeitos().isEmpty());
    }

    private void validarVagaoNaComposicao(VisitaTrem visita, String identificadorVagao) {
        boolean existe = Optional.ofNullable(visita.getListaVagoes())
                .orElseGet(ArrayList::new)
                .stream()
                .map(VagaoVisita::getIdentificadorVagao)
                .filter(StringUtils::hasText)
                .anyMatch(identificador -> identificadorVagao.equalsIgnoreCase(identificador));
        if (!existe) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "O vagão informado não pertence à composição da visita.");
        }
    }

    private VisitaTrem buscarVisita(Long idVisita) {
        validarId(idVisita, "visita");
        return visitaTremRepositorio.findOneById(idVisita)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Visita de trem não encontrada."));
    }

    private void validarId(Long id, String campo) {
        if (id == null || id <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    String.format(Locale.ROOT, "O identificador da %s é inválido.", campo));
        }
    }

    private String normalizar(String valor) {
        return valor == null ? null : valor.trim();
    }

    private String normalizarMaiusculo(String valor) {
        String normalizado = normalizar(valor);
        return normalizado == null ? null : normalizado.toUpperCase(Locale.ROOT);
    }
}
