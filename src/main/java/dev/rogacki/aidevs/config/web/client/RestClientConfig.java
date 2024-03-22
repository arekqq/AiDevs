package dev.rogacki.aidevs.config.web.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Configuration
@Slf4j
@RequiredArgsConstructor
public class RestClientConfig {

    @Value("${tasks.baseUrl}")
    public String baseUrl;

    public final LoggingInterceptor loggingInterceptor;

    @Bean
    //@Qualifier("taskClient")
    public RestClient tasksClient() {
        return RestClient.builder()
            .baseUrl(baseUrl)
            .requestInterceptor(loggingInterceptor)
            .build();
    }
}
