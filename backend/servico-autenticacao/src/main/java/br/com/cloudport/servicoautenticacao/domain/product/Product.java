package br.com.cloudport.servicoautenticacao.domain.product;

import javax.persistence.*;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Table(name = "product")
@Entity(name = "product")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class Product {
    @Id
    private UUID id;

    @Column
    private String name;

    @Column
    private Integer price;

    public Product(ProductRequestDTO data){
        this.id = UUID.randomUUID(); // Generate a random UUID
        this.price = data.getPrice();
        this.name = data.getName();
    }
}
