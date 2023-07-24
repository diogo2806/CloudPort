package br.com.cloudport.servicoautenticacao.domain.product;

public class ProductResponseDTO {
    private final String id;
    private final String name;
    private final Integer price;

    public ProductResponseDTO(Product product){
        this.id = product.getId().toString();
        this.name = product.getName();
        this.price = product.getPrice();
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Integer getPrice() {
        return price;
    }
}
