package dev.rogacki.aidevs;

import dev.rogacki.aidevs.service.GnomeTaskService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@Slf4j
@SpringBootApplication
public class AiDevsApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiDevsApplication.class, args);
    }

    @Bean
    ApplicationRunner applicationRunner(GnomeTaskService taskService) {
        return _ -> taskService.run();
    }
}
