package dev.rogacki.aidevs.external;

import dev.rogacki.aidevs.dto.AnswerResponse;
import dev.rogacki.aidevs.dto.TokenResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Component
public class TaskClient {

    private final RestClient taskRestClient;

    @Value("${tasks.apiKey}")
    private String apiKey;

    public TaskClient(RestClient taskRestClient) {
        this.taskRestClient = taskRestClient;
    }

    public ResponseEntity<TokenResponse> getToken(String taskName) {
        return taskRestClient.post()
            .uri("/token/{taskName}", taskName)
            .body(Map.of("apikey", apiKey))
            .retrieve()
            .toEntity(TokenResponse.class);
    }

    public <T> T getTask(String token, Class<T> type) {
        return taskRestClient.get()
            .uri("/task/{token}", token)
            .retrieve()
            .body(type);
    }

    public ResponseEntity<AnswerResponse> postAnswer(String token, Object answer) {
        return taskRestClient.post()
            .uri("/answer/{token}", token)
            .body(Map.of("answer", answer))
            .retrieve()
            .toEntity(AnswerResponse.class);
    }
}
