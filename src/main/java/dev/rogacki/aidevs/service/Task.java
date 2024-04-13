package dev.rogacki.aidevs.service;

import dev.rogacki.aidevs.dto.AnswerResponse;
import dev.rogacki.aidevs.dto.TokenResponse;
import dev.rogacki.aidevs.external.TaskClient;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.openai.OpenAiChatClient;
import org.springframework.http.ResponseEntity;

import java.util.Optional;

@Slf4j
@RequiredArgsConstructor
public abstract class Task implements Runnable {

    private final TaskClient taskClient;
    protected final OpenAiChatClient springClient;

    @Getter
    private String token;

    protected ResponseEntity<AnswerResponse> postAnswer(Object answer) {
        try {
            log.info("Posting answer: {}", answer);
            ResponseEntity<AnswerResponse> answerResponseResponseEntity = taskClient.postAnswer(token, answer);
            log.info(answerResponseResponseEntity.getStatusCode().toString());
            return answerResponseResponseEntity;
        } catch (Exception e) {
            log.error("Error posting answer", e);
            return ResponseEntity.badRequest().build();
        }
    }

    public <T> T getTask(String taskName, Class<T> type) {
        token = getToken(taskName);
        return taskClient.getTask(token, type);
    }

    private String getToken(String taskName) {
        var tokenResponse = taskClient.getToken(taskName);
        return Optional.ofNullable(tokenResponse.getBody())
            .map(TokenResponse::token)
            .orElseThrow();
    }
}
