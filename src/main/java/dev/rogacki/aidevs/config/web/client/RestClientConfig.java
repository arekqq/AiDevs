package dev.rogacki.aidevs.config.web.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

    @Value("${tasks.baseUrl}")
    public String baseUrl;

    @Bean
    //@Qualifier("taskClient")
    public RestClient tasksClient() {
        return RestClient.create(baseUrl);
    }
}
