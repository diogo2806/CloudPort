package br.com.cloudport.servicoyard.repository;

import br.com.cloudport.servicoyard.model.Container;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ContainerRepository extends JpaRepository<Container, Long> {
}
