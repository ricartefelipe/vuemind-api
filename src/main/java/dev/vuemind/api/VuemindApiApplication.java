package dev.vuemind.api;

import java.time.Clock;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration;
import org.springframework.context.annotation.Bean;

@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
public class VuemindApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(VuemindApiApplication.class, args);
    }

    /** Relógio do PIX agendado; os testes trocam por um relógio controlado. */
    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }
}
