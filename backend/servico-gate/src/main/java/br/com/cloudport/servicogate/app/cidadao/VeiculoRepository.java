package br.com.cloudport.servicogate.app.cidadao;

import br.com.cloudport.servicogate.model.Veiculo;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VeiculoRepository extends JpaRepository<Veiculo, Long> {

    Optional<Veiculo> findByPlaca(String placa);

    List<Veiculo> findByTransportadoraIdOrderByPlacaAsc(Long transportadoraId);
}
