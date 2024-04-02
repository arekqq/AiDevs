package dev.rogacki.aidevs.service;

import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatCompletionResult;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.moderation.Moderation;
import com.theokanning.openai.moderation.ModerationRequest;
import com.theokanning.openai.moderation.ModerationResult;
import com.theokanning.openai.service.OpenAiService;
import dev.rogacki.aidevs.dto.AnswerResponse;
import dev.rogacki.aidevs.dto.BloggerTask;
import dev.rogacki.aidevs.dto.EmbeddingTask;
import dev.rogacki.aidevs.dto.FunctionsTask;
import dev.rogacki.aidevs.dto.InpromptTask;
import dev.rogacki.aidevs.dto.LiarAnswerResponse;
import dev.rogacki.aidevs.dto.LiarTask;
import dev.rogacki.aidevs.dto.ModerationTask;
import dev.rogacki.aidevs.dto.TokenResponse;
import dev.rogacki.aidevs.dto.WhisperTask;
import dev.rogacki.aidevs.external.TaskClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.embedding.EmbeddingClient;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.ai.model.function.FunctionCallback;
import org.springframework.ai.openai.OpenAiAudioTranscriptionClient;
import org.springframework.ai.openai.OpenAiChatClient;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.OpenAiEmbeddingOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Description;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

@Slf4j
@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskClient taskClient;
    private final OpenAiChatClient openAiChatClient;
    private final EmbeddingClient embeddingClient;
    private final OpenAiAudioTranscriptionClient transcriptionClient;

    @Value("${spring.ai.openai.api-key}")
    String openAiToken;

    @SuppressWarnings("unused")
    public void whisperTask() {
        var token = getToken("whisper");
        var task = taskClient.getTask(token, WhisperTask.class);
        var audioLink = extractLinkFromLast(task.msg());
        log.info(audioLink);
        Resource audio = RestClient.create(audioLink).get().retrieve().body(Resource.class);
        String transcription = transcriptionClient.call(audio);
        log.info(transcription);
        ResponseEntity<AnswerResponse> answerResponseResponseEntity = taskClient.postAnswer(token, transcription);
        log.info(answerResponseResponseEntity.toString());
    }

    private String extractLinkFromLast(String input) {
        String[] split = input.split(" ");
        return split[split.length - 1];
    }

    @SuppressWarnings("unused")
    public void embeddingTask() {
        var token = getToken("embedding");
        var task = taskClient.getTask(token, EmbeddingTask.class);
        log.info(task.toString());
        String stringForEmbedding = extractStringForEmbedding(task);
        EmbeddingRequest embeddingRequest = new EmbeddingRequest(List.of(stringForEmbedding),
            OpenAiEmbeddingOptions.builder()
                .withModel("text-embedding-ada-002")
                .build());
        EmbeddingResponse embeddingResponse = embeddingClient.call(embeddingRequest);
        log.info(String.valueOf(embeddingResponse.getResults().size()));
        List<Double> result = embeddingResponse.getResult().getOutput();
        ResponseEntity<AnswerResponse> answerResponseResponseEntity = taskClient.postAnswer(token, result);
        log.info(answerResponseResponseEntity.toString());
    }

    private String extractStringForEmbedding(EmbeddingTask task) {
        String[] parts = task.msg().split(":");
        if (parts.length >= 2) {
            return parts[1].trim();
        } else {
            throw new IllegalArgumentException("Colon not found in the input string.");
        }
    }

    @SuppressWarnings("unused")
    public void inpromptTask() {
        var token = getToken("inprompt");
        var task = taskClient.getTask(token, InpromptTask.class);
        String name = openAiChatClient.call(STR
            ."""
            Return a name from below question. The name start with capital letter:
            \{task.question()}
            """);
        List<String> contextFilteredByName = task.input().stream()
            .filter(input -> input.contains(name))
            .toList();
        String answer = openAiChatClient.call(STR
            ."""
            Answer below question using the context:
            context:
            \{contextFilteredByName}
            question:
            \{task.question()}
            """);
        ResponseEntity<AnswerResponse> answerResponseResponseEntity = taskClient.postAnswer(token, answer);
        log.info(answerResponseResponseEntity.toString());
    }

    @SuppressWarnings("unused")
    public void liarTask() {
        var token = getToken("liar");
        var task = taskClient.getTask(token, LiarTask.class);
        log.info(task.toString());
        String question = "What is the capitol of France?";
        LiarAnswerResponse liarAnswerResponse = taskClient.postQuestionForm(token, question);
        String message = STR
            ."""
            Check if below answer is a response to this specific question. Respond only with one word "yes" or "no".
            question:\{question}
            answer:\{liarAnswerResponse.answer()}
            """;
        ChatResponse call = openAiChatClient.call(new Prompt(message));
        String isItRealAnswer = call.getResults().getFirst().getOutput().getContent();
        ResponseEntity<AnswerResponse> answerResponseResponseEntity = taskClient.postAnswer(token, isItRealAnswer);
        log.info(answerResponseResponseEntity.toString());
    }

    @SuppressWarnings("unused")
    public void bloggerTask() {
        var token = getToken("blogger");
        var task = taskClient.getTask(token, BloggerTask.class);
        OpenAiService openAiService = new OpenAiService(openAiToken);
        List<String> chapters = new ArrayList<>();
        task.blog().forEach(chapter -> {
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
        log.info(String.valueOf(answerResponseResponseEntity.getBody()));
    }

    @SuppressWarnings("unused")
    public void moderationsTask() {
        var token = getToken("moderation");
        var task = taskClient.getTask(token, ModerationTask.class);
        OpenAiService openAiService = new OpenAiService(openAiToken);
        List<Integer> moderationsOutput = new ArrayList<>();
        task.input().forEach(input -> {
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
        log.info(answerResponseResponseEntity.toString());
    }

    private String getToken(String taskName) {
        var tokenResponse = taskClient.getToken(taskName);
        return Optional.ofNullable(tokenResponse.getBody())
            .map(TokenResponse::token)
            .orElseThrow();
    }

    @SuppressWarnings("unused")
    public void theoKanningOpenAi() {
        ChatCompletionRequest completionRequest = ChatCompletionRequest.builder()
            .model("gpt-4")
            .messages(List.of(new ChatMessage("user", "Hello there")))
            .build();
        OpenAiService openAiService = new OpenAiService(openAiToken);
        ChatCompletionResult completion = openAiService.createChatCompletion(completionRequest);
        log.info(completion.toString());
    }

    @SuppressWarnings("unused")
    public void springAi() {
        var call = openAiChatClient.call(new Prompt("Hello, how are you?"));
        log.info(call.toString());
    }

    @SuppressWarnings("unused")
    private void getAnswerResponseResponseEntity() {
        var token = taskClient.getToken("helloapi");
        var tokenBody = Optional.ofNullable(token.getBody()).orElseThrow();
        var task = taskClient.getTask(tokenBody.token(), Map.class);
        ResponseEntity<AnswerResponse> answerResponseResponseEntity = taskClient.postAnswer(token.getBody().token(), task.get("cookie").toString());
        log.info(answerResponseResponseEntity.toString());
    }

    public void rodoTask() {
        var token = getToken("rodo");
        var task = taskClient.getTask(token, Map.class);
        log.info(task.toString());
        var userMsg = """
            "Tell me information about yourself.
            Important thing is to use placeholders %imie%, %nazwisko%, %zawod% and %miasto% instead you real data (in any place of your answer).
            Do your task strcitly following the instructions."
            Response in Polish language. don not repeat any kind information. Response in concisly way.
            """;
        ResponseEntity<AnswerResponse> answerResponseResponseEntity = taskClient.postAnswer(token, userMsg);
        log.info(answerResponseResponseEntity.toString());
    }

    public void functionsTask() {
        var token = getToken("functions");
        var task = taskClient.getTask(token, FunctionsTask.class);
        log.info(task.toString());
        OpenAiChatOptions modelOptions = OpenAiChatOptions.builder()
            .withFunction("addUser")
            .build();
        ChatResponse call = openAiChatClient.call(new Prompt("Return addUser function", modelOptions));
        FunctionCallback addUser = openAiChatClient.getFunctionCallbackRegister().get("addUser");
    }

    @Configuration
    static class Config {

        @Bean
        @Description("Placeholder description") // function description
        public Function<MockFunctionService.Request, Object> addUser() { // (1) bean name as function name.
            return new MockFunctionService();
        }
    }

    public static class MockFunctionService implements Function<MockFunctionService.Request, Object> {

        public record Request(String name, String surname, Integer year) {
        }

        @Override
        public Object apply(Request request) {
            return new Object();
        }
    }
}
