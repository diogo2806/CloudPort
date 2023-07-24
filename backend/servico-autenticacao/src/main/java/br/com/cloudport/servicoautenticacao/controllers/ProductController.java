package br.com.cloudport.servicoautenticacao.controllers;

import br.com.cloudport.servicoautenticacao.domain.product.Product;
import br.com.cloudport.servicoautenticacao.domain.product.ProductRequestDTO;
import br.com.cloudport.servicoautenticacao.domain.product.ProductResponseDTO;
import br.com.cloudport.servicoautenticacao.repositories.ProductRepository;
import javax.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController()
@RequestMapping("product")
public class ProductController {

    @Autowired
    ProductRepository repository;

    @PostMapping
    public ResponseEntity postProduct(@RequestBody @Valid ProductRequestDTO body){
        Product newProduct = new Product(body);

        this.repository.save(newProduct);
        return ResponseEntity.ok().build();
    }

    @GetMapping
    public ResponseEntity getAllProducts(){
        List<ProductResponseDTO> productList = this.repository.findAll().stream().map(ProductResponseDTO::new).collect(Collectors.toList());

        return ResponseEntity.ok(productList);
    }
}
