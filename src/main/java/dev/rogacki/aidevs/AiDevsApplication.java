package dev.rogacki.aidevs;

import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatCompletionResult;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.moderation.Moderation;
import com.theokanning.openai.moderation.ModerationRequest;
import com.theokanning.openai.moderation.ModerationResult;
import com.theokanning.openai.service.OpenAiService;
import dev.rogacki.aidevs.dto.AnswerResponse;
import dev.rogacki.aidevs.dto.TokenResponse;
import dev.rogacki.aidevs.external.TaskClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@SpringBootApplication
public class AiDevsApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiDevsApplication.class, args);
    }

    @Bean
    ApplicationRunner applicationRunner(TaskClient taskClient,
                                        OpenAiChatClient openAiChatClient,
                                        @Value("${spring.ai.openai.api-key}") String openAiToken) {
        return _ -> {
//            springAi(openAiChatClient);
//            var answerResponse = getAnswerResponseResponseEntity(taskClient);
//            log.info(answerResponse.toString());
//            theoKanningOpenAi(openAiToken);
            moderationsTask(taskClient, openAiToken);
        };
    }

    private void moderationsTask(TaskClient taskClient, String openAiToken) {
        var token = getToken(taskClient);
        var task = taskClient.getTask(token, ModerationTask.class);
        OpenAiService openAiService = new OpenAiService(openAiToken);
        List<Integer> moderationsOutput = new ArrayList<>();
        task.input.forEach(input -> {
            ModerationRequest moderationRequest = ModerationRequest.builder()
                .input(input)
                .model("text-moderation-latest")
                .build();
            ModerationResult moderation = openAiService.createModeration(moderationRequest);
            boolean anyFlagged = moderation.getResults().stream()
                .anyMatch(Moderation::isFlagged);
            moderationsOutput.add(anyFlagged ? 1 : 0);
        });
        ResponseEntity<AnswerResponse> answerResponseResponseEntity = taskClient.postAnswer(token, moderationsOutput);
    }

    private static String getToken(TaskClient taskClient) {
        var tokenResponse = taskClient.getToken("moderation");
        return Optional.ofNullable(tokenResponse.getBody())
            .map(TokenResponse::token)
            .orElseThrow();
    }

    private void theoKanningOpenAi(String openAiToken) {
        ChatCompletionRequest completionRequest = ChatCompletionRequest.builder()
            .model("gpt-4")
            .messages(List.of(new ChatMessage("user", "Hello there")))
            .build();
        OpenAiService openAiService = new OpenAiService(openAiToken);
        ChatCompletionResult completion = openAiService.createChatCompletion(completionRequest);
        log.info(completion.toString());
    }

    private static void springAi(OpenAiChatClient openAiChatClient) {
        var role = "developer";
        var context = "awdaw";
        var systemTemplate = STR
            ."""
            As a \{role} who answers the questions ultra-concisely using CONTEXT below\
            and nothing more and truthfully says "don't know" when the CONTEXT is not enough to give an answer.

            context###\{context}###
            """;
        var call = openAiChatClient.call(new Prompt("Hello, how are you?"));
        log.info(call.toString());
    }

    private static ResponseEntity<AnswerResponse> getAnswerResponseResponseEntity(TaskClient taskClient) {
        var token = taskClient.getToken("helloapi");
        var tokenBody = Optional.ofNullable(token.getBody()).orElseThrow();
        var task = taskClient.getTask(tokenBody.token(), Map.class);
        var answerResponse = taskClient.postAnswer(token.getBody().token(), task.get("cookie").toString());
        return answerResponse;
    }

    public record ModerationTask(
        Integer code,
        String msg,
        List<String> input
    ) {
    }


}
