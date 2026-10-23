package edu.css3008.purchase.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "edu.css3008.purchase")
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}

