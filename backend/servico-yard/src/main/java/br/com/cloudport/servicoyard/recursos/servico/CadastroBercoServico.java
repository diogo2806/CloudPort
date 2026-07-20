package br.com.cloudport.servicoyard.recursos.servico;

import br.com.cloudport.servicoyard.recursos.dto.CadastroBercoDTO;
import br.com.cloudport.servicoyard.recursos.dto.CadastroBercoRespostaDTO;
import br.com.cloudport.servicoyard.recursos.entidade.BercoPortuario;
import br.com.cloudport.servicoyard.recursos.repositorio.BercoPortuarioRepositorio;
import br.com.cloudport.servicoyard.recursos.repositorio.ReservaBercoRepositorio;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class CadastroBercoServico {

    private final BercoPortuarioRepositorio bercoRepositorio;
    private final ReservaBercoRepositorio reservaRepositorio;

    public CadastroBercoServico(BercoPortuarioRepositorio bercoRepositorio,
                                ReservaBercoRepositorio reservaRepositorio) {
        this.bercoRepositorio = bercoRepositorio;
        this.reservaRepositorio = reservaRepositorio;
    }

    @Transactional(readOnly = true)
    public List<CadastroBercoRespostaDTO> listar() {
        return bercoRepositorio.findAllByOrderByCodigoAsc().stream()
                .map(this::converter)
                .collect(Collectors.toList());
    }

    @Transactional
    public CadastroBercoRespostaDTO criar(CadastroBercoDTO dto) {
        String codigo = normalizarCodigo(dto.getCodigo());
        if (bercoRepositorio.findByCodigoIgnoreCase(codigo).isPresent()) {
            throw new IllegalArgumentException("Já existe um berço com o código informado.");
        }
        BercoPortuario berco = new BercoPortuario();
        aplicar(berco, dto, codigo);
        return converter(bercoRepositorio.save(berco));
    }

    @Transactional
    public CadastroBercoRespostaDTO atualizar(Long id, CadastroBercoDTO dto) {
        BercoPortuario berco = buscar(id);
        String codigo = normalizarCodigo(dto.getCodigo());
        bercoRepositorio.findByCodigoIgnoreCase(codigo)
                .filter(existente -> !existente.getId().equals(id))
                .ifPresent(existente -> {
                    throw new IllegalArgumentException("Já existe outro berço com o código informado.");
                });
        aplicar(berco, dto, codigo);
        return converter(bercoRepositorio.save(berco));
    }

    @Transactional
    public void remover(Long id) {
        BercoPortuario berco = buscar(id);
        if (!reservaRepositorio.findByBercoCodigoOrderByChegadaPrevistaAsc(berco.getCodigo()).isEmpty()) {
            throw new IllegalArgumentException("O berço possui reservas ou manutenções vinculadas e não pode ser excluído.");
        }
        bercoRepositorio.delete(berco);
    }

    private BercoPortuario buscar(Long id) {
        return bercoRepositorio.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Berço não encontrado."));
    }

    private void aplicar(BercoPortuario berco, CadastroBercoDTO dto, String codigo) {
        berco.setCodigo(codigo);
        berco.setNome(dto.getNome().trim());
        berco.setComprimentoMetros(dto.getComprimentoMetros());
        berco.setCaladoMetros(dto.getCaladoMetros());
        berco.setGuinchesPermanentes(dto.getGuinchesPermanentes());
        berco.setCapacidadeToneladasDia(dto.getCapacidadeToneladasDia());
        berco.setVoltagem(dto.getVoltagem().trim());
        berco.setAguaPotavel(dto.isAguaPotavel());
        berco.setEnergiaGenerica(dto.isEnergiaGenerica());
        berco.setIluminacaoNoturna(dto.isIluminacaoNoturna());
        berco.setSistemaSeguranca(dto.isSistemaSeguranca());
        berco.setCobertura(dto.isCobertura());
        berco.setCompatContainer(dto.isCompatContainer());
        berco.setCompatBreakbulk(dto.isCompatBreakbulk());
        berco.setCompatRoro(dto.isCompatRoro());
        berco.setCompatCargaGeral(dto.isCompatCargaGeral());
        berco.setCompatReefer(dto.isCompatReefer());
        berco.setCompatPerigosa(dto.isCompatPerigosa());
        berco.setCompatGranel(dto.isCompatGranel());
        berco.setZonaPrimaria(dto.getZonaPrimaria().trim().toUpperCase(Locale.ROOT));
        berco.setZonaSecundaria(limpar(dto.getZonaSecundaria()));
        berco.setDistanciaZonaMetros(dto.getDistanciaZonaMetros());
        berco.setTempoTransporteMinutos(dto.getTempoTransporteMinutos());
        berco.setDiasOperacao(dto.getDiasOperacao().trim());
        berco.setUltimaManutencao(dto.getUltimaManutencao());
        berco.setProximaManutencao(dto.getProximaManutencao());
        berco.setStatus(dto.getStatus());
        berco.setObservacoes(limpar(dto.getObservacoes()));
    }

    private String normalizarCodigo(String codigo) {
        return codigo.trim().toUpperCase(Locale.ROOT);
    }

    private String limpar(String valor) {
        return StringUtils.hasText(valor) ? valor.trim() : null;
    }

    private CadastroBercoRespostaDTO converter(BercoPortuario berco) {
        return new CadastroBercoRespostaDTO(
                berco.getId(),
                berco.getCodigo(),
                berco.getNome(),
                berco.getComprimentoMetros(),
                berco.getCaladoMetros(),
                berco.getGuinchesPermanentes(),
                berco.getCapacidadeToneladasDia(),
                berco.getVoltagem(),
                berco.isAguaPotavel(),
                berco.isEnergiaGenerica(),
                berco.isIluminacaoNoturna(),
                berco.isSistemaSeguranca(),
                berco.isCobertura(),
                berco.isCompatContainer(),
                berco.isCompatBreakbulk(),
                berco.isCompatRoro(),
                berco.isCompatCargaGeral(),
                berco.isCompatReefer(),
                berco.isCompatPerigosa(),
                berco.isCompatGranel(),
                berco.getZonaPrimaria(),
                berco.getZonaSecundaria(),
                berco.getDistanciaZonaMetros(),
                berco.getTempoTransporteMinutos(),
                berco.getDiasOperacao(),
                berco.getUltimaManutencao(),
                berco.getProximaManutencao(),
                berco.getStatus(),
                berco.getObservacoes());
    }
}
