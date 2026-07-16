package br.com.cloudport.servicoyard;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class ServicoYardApplication {
    public static void main(String[] args) {
        SpringApplication.run(ServicoYardApplication.class, args);
    }
}
