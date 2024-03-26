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
import org.springframework.ai.chat.ChatResponse;
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
                                        OpenAiChatClient springOpenAiClient,
                                        @Value("${spring.ai.openai.api-key}") String openAiToken) {
        return _ -> {
//            springAi(springOpenAiClient);
//            var answerResponse = getAnswerResponseResponseEntity(taskClient);
//            log.info(answerResponse.toString());
//            theoKanningOpenAi(openAiToken);
//            moderationsTask(taskClient, openAiToken);
//            bloggerTask(taskClient, openAiToken);
//            liarTask(taskClient, springOpenAiClient, openAiToken);
//            inpromptTask(taskClient, springOpenAiClient);
        };
    }

    private void inpromptTask(TaskClient taskClient, OpenAiChatClient springOpenAiClient) {
        var token = getToken(taskClient, "inprompt");
        var task = taskClient.getTask(token, InpromptTask.class);
        String name = springOpenAiClient.call(STR
            ."""
            Return a name from below question. The name start with capital letter:
            \{task.question}
            """);
        List<String> contextFilteredByName = task.input.stream()
            .filter(input -> input.contains(name))
            .toList();
        String answer = springOpenAiClient.call(STR
            ."""
            Answer below question using the context:
            context:
            \{contextFilteredByName}
            question:
            \{task.question}
            """);
        ResponseEntity<AnswerResponse> answerResponseResponseEntity = taskClient.postAnswer(token, answer);
        log.info(answerResponseResponseEntity.toString());
    }

    private void liarTask(TaskClient taskClient, OpenAiChatClient openAiChatClient) {
        var token = getToken(taskClient, "liar");
        var task = taskClient.getTask(token, LiarTask.class);
        log.info(task.toString());
        String question = "What is the capitol of France?";
        LiarAnswerResponse liarAnswerResponse = taskClient.postQuestionForm(token, question);
        String message = STR
            ."""
            Check if below answer is a response to this specific question. Respond only with one word "yes" or "no".
            question:\{question}
            answer:\{liarAnswerResponse.answer}
            """;
        ChatResponse call = openAiChatClient.call(new Prompt(message));
        String isItRealAnswer = call.getResults().getFirst().getOutput().getContent();
        ResponseEntity<AnswerResponse> answerResponseResponseEntity = taskClient.postAnswer(token, isItRealAnswer);
        log.info(answerResponseResponseEntity.toString());
    }

    private void bloggerTask(TaskClient taskClient, String openAiToken) {
        var token = getToken(taskClient, "blogger");
        var task = taskClient.getTask(token, BloggerTask.class);
        OpenAiService openAiService = new OpenAiService(openAiToken);
        List<String> chapters = new ArrayList<>();
        task.blog.forEach(chapter -> {
            var systemTemplate = STR
                ."""
                Jako bloger napisz wpis o następującym temacie. Max 4-5 zdań.

                temat###\{
                chapter
                }###
                """;
            ChatCompletionRequest completionRequest = ChatCompletionRequest.builder() // TODO gather all requests and response in next messages
                .model("gpt-4")
                .messages(List.of(new ChatMessage("user", systemTemplate)))
                .build();
            ChatCompletionResult completion = openAiService.createChatCompletion(completionRequest);
            chapters.add(completion.getChoices().stream().findFirst().map(c -> c.getMessage().getContent()).orElseThrow());
            log.info(completion.toString());
        });
        ResponseEntity<AnswerResponse> answerResponseResponseEntity = taskClient.postAnswer(token, chapters);
        log.info(answerResponseResponseEntity.getBody().toString());
    }

    private void moderationsTask(TaskClient taskClient, String openAiToken) {
        var token = getToken(taskClient, "moderation");
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

    private static String getToken(TaskClient taskClient, String taskName) {
        var tokenResponse = taskClient.getToken(taskName);
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

    private void springAi(OpenAiChatClient openAiChatClient) {
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

    public record BloggerTask(
        Integer code,
        String msg,
        List<String> blog
    ) {
    }

    public record LiarTask(
        Integer code,
        String msg,
        String hint1,
        String hint2,
        String hint3
    ) {
    }

    public record LiarAnswerResponse(
        Integer code,
        String msg,
        String answer
    ) {
    }

    public record InpromptTask(
        Integer code,
        String msg,
        List<String> input,
        String question) {
    }


}
