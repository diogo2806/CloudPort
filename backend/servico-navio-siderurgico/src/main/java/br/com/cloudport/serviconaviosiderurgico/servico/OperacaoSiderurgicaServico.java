package br.com.cloudport.serviconaviosiderurgico.servico;

import br.com.cloudport.serviconaviosiderurgico.dominio.OperacaoSiderurgica;
import br.com.cloudport.serviconaviosiderurgico.dominio.StatusOperacaoSiderurgica;
import br.com.cloudport.serviconaviosiderurgico.dto.OperacaoSiderurgicaDTO;
import br.com.cloudport.serviconaviosiderurgico.repositorio.OperacaoSiderurgicaRepositorio;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OperacaoSiderurgicaServico {

    private final OperacaoSiderurgicaRepositorio repositorio;
    private final NavioSiderurgicoServico navioServico;

    public OperacaoSiderurgicaServico(OperacaoSiderurgicaRepositorio repositorio, NavioSiderurgicoServico navioServico) {
        this.repositorio = repositorio;
        this.navioServico = navioServico;
    }

    @Transactional(readOnly = true)
    public List<OperacaoSiderurgicaDTO> listar(Long navioId) {
        return (navioId == null ? repositorio.findAll() : repositorio.findByNavioIdOrderByEtaDesc(navioId))
                .stream().map(OperacaoSiderurgicaDTO::de).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public OperacaoSiderurgica buscarEntidade(Long id) {
        return repositorio.findById(id).orElseThrow(() -> new IllegalArgumentException("Operacao siderurgica nao encontrada."));
    }

    @Transactional
    public OperacaoSiderurgicaDTO criar(OperacaoSiderurgicaDTO dto) {
        OperacaoSiderurgica operacao = new OperacaoSiderurgica();
        operacao.setNavio(navioServico.buscarEntidade(dto.navioId()));
        operacao.setTipoOperacao(dto.tipoOperacao());
        operacao.setStatus(dto.status() == null ? StatusOperacaoSiderurgica.PLANEJADA : dto.status());
        operacao.setBerco(dto.berco());
        operacao.setViagem(dto.viagem());
        operacao.setEta(dto.eta());
        operacao.setInicioOperacao(dto.inicioOperacao());
        operacao.setFimOperacao(dto.fimOperacao());
        operacao.setOrigem(dto.origem());
        operacao.setDestino(dto.destino());
        operacao.setObservacoes(dto.observacoes());
        return OperacaoSiderurgicaDTO.de(repositorio.save(operacao));
    }
}
