package br.com.cloudport.servicoautenticacao.repositories;

import com.example.auth.domain.product.Product;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, String> {
}
