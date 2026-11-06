package br.com.abadeus.paellaapi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class PaellaApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(PaellaApiApplication.class, args);
    }
}
