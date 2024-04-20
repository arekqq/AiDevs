package dev.rogacki.aidevs.service;

import dev.rogacki.aidevs.dto.AnswerResponse;
import dev.rogacki.aidevs.external.TaskClient;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.openai.OpenAiChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.util.Map;

@Slf4j
@Service
public class MemeTaskService extends Task {


    @Value("${render_forn.apiKey}")
    public String apiKey;

    public MemeTaskService(TaskClient taskClient,
                           OpenAiChatClient springClient) {
        super(taskClient, springClient);
    }

    @SneakyThrows
    @Override
    public void run() {
        var ownapiTask = getTask("meme", MemeTask.class);
        RestClient renderFormClient = RestClient.builder()
            .baseUrl("https://get.renderform.io/api/v2/render")
            .defaultHeader("X-API-KEY", apiKey)
            .build();
        Map<String, Object> body = Map.of(
            "template", "lean-ant-lions-eat-loudly-1765",
            "data", Map.of(
                "image.src", ownapiTask.image(),
                "title.text", ownapiTask.text()
            )

        );
        var response = renderFormClient
            .post()
            .body(body)
            .retrieve()
            .body(Render.class);
        ResponseEntity<AnswerResponse> answerResponseResponseEntity = postAnswer(response.href().toString());

        log.info(answerResponseResponseEntity.getBody().toString());
    }

    private record Render(
        String requestId,
        URI href
    ) {
    }

    private record MemeTask(
        int code,
        String msg,
        String service,
        String image,
        String text,
        String hint
    ) {
    }
}
