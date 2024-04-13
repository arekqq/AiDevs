package dev.rogacki.aidevs.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import dev.rogacki.aidevs.dto.AnswerResponse;
import dev.rogacki.aidevs.external.TaskClient;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatClient;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Description;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.function.Function;

@Slf4j
@Service
public class KnowledgeTaskService extends Task {


    public KnowledgeTaskService(TaskClient taskClient,
                                OpenAiChatClient springClient) {
        super(taskClient, springClient);
    }

    @SneakyThrows
    @Override
    public void run() {
        var knowledgeTask = getTask("knowledge", KnowledgeTask.class);
        log.info(knowledgeTask.toString());
        OpenAiChatOptions modelOptions = OpenAiChatOptions.builder()
            .withFunction("getCurrency")
            .withFunction("getCountryPopulation")
            .build();
        String message = STR."""
            Answer ultra briefly the following question: \{knowledgeTask.question()}
            If it's possible answer only with one number or one word- without any formatting. In case it's 65 thousand - answer 65000, in case it's 4 and a half - answer 4.5```
            In case it's about currency - use the function getCurrency with currency code as an arguent.
            In case it's about country population - use the function getCountryPopulation with country name as an argument.
            """;
        ChatResponse call = springClient.call(new Prompt(message, modelOptions));
        ResponseEntity<AnswerResponse> answerResponseResponseEntity = postAnswer(call.getResult().getOutput().getContent());
        log.info(answerResponseResponseEntity.getBody().toString());
    }

    @Configuration
    static class Config {

        @Bean
        @Description("Gets current currency exchange rate for a given currency code.")
        public Function<CurrencyFunctionService.Request, String> getCurrency() {
            return new CurrencyFunctionService();
        }

        @Bean
        @Description("Gets population data for a given country name")
        public Function<PopulationFunctionService.Request, String> getCountryPopulation() {
            return new PopulationFunctionService();
        }
    }

    public static class CurrencyFunctionService implements Function<CurrencyFunctionService.Request, String> {

        public record Request(String currencyCode) {
        }

        @Override
        public String apply(CurrencyFunctionService.Request request) {
            String body = RestClient.create(STR."http://api.nbp.pl/api/exchangerates/rates/A/\{request.currencyCode()}")
                .get()
                .retrieve()
                .body(String.class);
            return body;
        }
    }

    public static class PopulationFunctionService implements Function<PopulationFunctionService.Request, String> {

        public record Request(String countryName) {
        }

        @Override
        public String apply(PopulationFunctionService.Request request) {
            String body = RestClient.create(STR."https://restcountries.com/v3.1/name/\{request.countryName()}?fields=name,population")
                .get()
                .retrieve()
                .body(String.class);
            return body;
        }
    }

    private record KnowledgeTask(
        int code,
        String msg,
        String question,
        @JsonProperty("database #1") String database1,
        @JsonProperty("database #2") String database2
    ) {
    }
}
