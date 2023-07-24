package br.com.cloudport.servicoautenticacao.repositories;

import br.com.cloudport.servicoautenticacao.domain.product.Product;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, String> {
}
