package org.salvationarmy.whatsapp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class WhatsAppDataCollectionApplication {

    public static void main(String[] args) {
        SpringApplication.run(WhatsAppDataCollectionApplication.class, args);
    }
}






