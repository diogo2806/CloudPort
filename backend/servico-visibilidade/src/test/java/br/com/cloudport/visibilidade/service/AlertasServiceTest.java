package br.com.cloudport.visibilidade.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.cloudport.visibilidade.dto.AlertaResumoDTO;
import br.com.cloudport.visibilidade.entity.Alerta;
import br.com.cloudport.visibilidade.repository.AlertaRepository;
import br.com.cloudport.visibilidade.repository.CapacidadeYardRepository;
import br.com.cloudport.visibilidade.repository.StatusNavioRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
class AlertasServiceTest {

    @Mock
    private AlertaRepository alertaRepository;

    @Mock
    private StatusNavioRepository statusNavioRepository;

    @Mock
    private CapacidadeYardRepository capacidadeYardRepository;

    private AlertasService service;

    @BeforeEach
    void configurar() {
        service = new AlertasService(alertaRepository, statusNavioRepository, capacidadeYardRepository);
    }

    @Test
    void deveMontarResumoDosAlertasAtivos() {
        when(alertaRepository.countByStatus("ativo")).thenReturn(8L);
        when(alertaRepository.countByStatusAndSeveridadeIgnoreCase("ativo", "critica")).thenReturn(2L);
        when(alertaRepository.countByStatusAndSeveridadeIgnoreCase("ativo", "alta")).thenReturn(3L);
        when(alertaRepository.countByStatusAndSeveridadeIgnoreCase("ativo", "media")).thenReturn(2L);
        when(alertaRepository.countByStatusAndSeveridadeIgnoreCase("ativo", "baixa")).thenReturn(1L);
        when(alertaRepository.countByStatusAndDataReconhecimentoIsNull("ativo")).thenReturn(4L);

        AlertaResumoDTO resumo = service.obterResumoAtivos();

        assertEquals(8L, resumo.getTotalAtivos());
        assertEquals(2L, resumo.getCriticos());
        assertEquals(3L, resumo.getAltos());
        assertEquals(4L, resumo.getNaoReconhecidos());
    }

    @Test
    void deveReconhecerAlertaSomenteUmaVez() {
        Alerta alerta = alertaAtivo(10L);
        when(alertaRepository.findById(10L)).thenReturn(Optional.of(alerta));
        when(alertaRepository.save(alerta)).thenReturn(alerta);

        Alerta reconhecido = service.reconhecerAlerta(10L, "Operador Gate");

        assertNotNull(reconhecido.getDataReconhecimento());
        assertEquals("Operador Gate", reconhecido.getReconhecidoPor());
        verify(alertaRepository).save(alerta);
    }

    @Test
    void deveResolverEReconhecerAlertaAtivo() {
        Alerta alerta = alertaAtivo(11L);
        when(alertaRepository.findById(11L)).thenReturn(Optional.of(alerta));
        when(alertaRepository.save(alerta)).thenReturn(alerta);

        Alerta resolvido = service.resolverAlerta(11L, "Planejador");

        assertEquals("resolvido", resolvido.getStatus());
        assertEquals("Planejador", resolvido.getReconhecidoPor());
        assertEquals("Planejador", resolvido.getResolvidoPor());
        assertNotNull(resolvido.getDataResolucao());
    }

    @Test
    void deveAceitarFiltrosOpcionaisNaConsultaPaginada() {
        PageRequest pageable = PageRequest.of(0, 20);
        Page<Alerta> expected = new PageImpl<>(List.of(alertaAtivo(12L)), pageable, 1);
        when(alertaRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(expected);

        Page<Alerta> result = service.buscarAlertasFiltrados(null, null, "ativo", pageable);

        assertEquals(1, result.getTotalElements());
    }

    private Alerta alertaAtivo(Long id) {
        Alerta alerta = new Alerta();
        alerta.setId(id);
        alerta.setTipo("ATRASO_NAVIO");
        alerta.setSeveridade("alta");
        alerta.setStatus("ativo");
        return alerta;
    }
}
