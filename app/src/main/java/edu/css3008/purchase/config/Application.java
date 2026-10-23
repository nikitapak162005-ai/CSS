package edu.css3008.purchase.config;

import java.util.Locale;

import edu.css3008.purchase.persistence.RequestEntity;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;

@SpringBootApplication(scanBasePackages = "edu.css3008.purchase")
@EntityScan(basePackageClasses = RequestEntity.class)
public class Application {
    public static void main(String[] args) {
        Locale.setDefault(Locale.ENGLISH);
        SpringApplication.run(Application.class, args);
    }
}

