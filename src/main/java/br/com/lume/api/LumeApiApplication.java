package br.com.lume.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.modulith.Modulithic;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
@ConfigurationPropertiesScan
@Modulithic(sharedModules = {"shared", "contract", "model"})
public class LumeApiApplication {

    static void main(String[] args) {
        SpringApplication.run(LumeApiApplication.class, args);
    }
}