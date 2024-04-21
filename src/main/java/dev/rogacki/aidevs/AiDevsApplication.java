package dev.rogacki.aidevs;

import dev.rogacki.aidevs.service.OptimaldbTaskService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.client.ClientHttpRequestFactories;
import org.springframework.boot.web.client.ClientHttpRequestFactorySettings;
import org.springframework.boot.web.client.RestClientCustomizer;
import org.springframework.context.annotation.Bean;

import java.time.Duration;

@Slf4j
@SpringBootApplication
public class AiDevsApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiDevsApplication.class, args);
    }

    @Bean
    public RestClientCustomizer restClientCustomizer() {
        return restClientBuilder -> restClientBuilder
            .requestFactory(ClientHttpRequestFactories.get(ClientHttpRequestFactorySettings.DEFAULTS
                .withConnectTimeout(Duration.ofSeconds(5))
                .withReadTimeout(Duration.ofSeconds(30))));
    }

    @Bean
    ApplicationRunner applicationRunner(OptimaldbTaskService taskService) {
        return _ -> taskService.run();
    }
}
