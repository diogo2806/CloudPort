package br.com.cloudport.servicoyard.operacao2d;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkspaceGrafico2DRepositorio extends JpaRepository<WorkspaceGrafico2D, Long> {

    Optional<WorkspaceGrafico2D> findTopByNomeAndEscopoAndProprietarioOrderByVersaoDesc(
            String nome,
            String escopo,
            String proprietario);

    List<WorkspaceGrafico2D> findAllByOrderByCriadoEmDesc();
}
