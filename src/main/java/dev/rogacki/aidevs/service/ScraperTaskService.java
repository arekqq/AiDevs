package dev.rogacki.aidevs.service;

import dev.rogacki.aidevs.dto.AnswerResponse;
import dev.rogacki.aidevs.external.TaskClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.openai.OpenAiChatClient;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Slf4j
@Service
public class ScraperTaskService extends Task {

    public ScraperTaskService(TaskClient taskClient, OpenAiChatClient springClient) {
        super(taskClient, springClient);
    }

    @Override
    public void run() {
        var task = this.getTask("scraper", ScraperTask.class);
        log.info(task.toString());
        String article = retrieveBody(task);
        log.info(article);
//        springClient.call("Translate it to polish: " + task.question());
        String prompt = STR
            ."""
            \{task.msg()}
            \{article}
            \{task.question()}
            """;
        String answer = springClient.call(prompt);
        log.info(answer);
        ResponseEntity<AnswerResponse> answerResponseResponseEntity = postAnswer(answer);
        log.info(answerResponseResponseEntity.toString());
    }

    private String retrieveBody(ScraperTask task) {
        String body = null;
        do {
            try {
                body = getInput(task.input());
            } catch (Exception e) {
                log.error("Error scraping", e);
            }
        } while (body == null);
        return body;
    }


    private static String getInput(String input) {
        var requestFactory = new JdkClientHttpRequestFactory();
        requestFactory.setReadTimeout(5000);
        return RestClient.builder()
            .requestFactory(requestFactory)
            .baseUrl(input)
            .defaultHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/58.0.3029.110 Safari/537.3")
            .build()
            .get()
            .retrieve()
            .body(String.class);
    }

    private record ScraperTask(
        String code,
        String msg,
        String input,
        String question
    ) {
    }
}
