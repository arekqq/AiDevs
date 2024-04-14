package dev.rogacki.aidevs.service;

import dev.rogacki.aidevs.dto.AnswerResponse;
import dev.rogacki.aidevs.external.TaskClient;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.ChatResponse;
import org.springframework.ai.chat.messages.Media;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatClient;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class GnomeTaskService extends Task {


    public GnomeTaskService(TaskClient taskClient,
                            OpenAiChatClient springClient) {
        super(taskClient, springClient);
    }

    @SneakyThrows
    @Override
    public void run() {
        var gnomeTask = getTask("gnome", GnomeTask.class);
        Resource resource = Optional.ofNullable(
            RestClient.create(gnomeTask.url().toString())
                .get()
                .retrieve()
                .body(Resource.class)).orElseThrow();
        SystemMessage systemMessage = new SystemMessage("""
            Rozpoznaj kolor czapki gnoma na obrazku.
            Jako wejście dostaniesz obrazek z gnomem
            """);
        UserMessage userMessage = new UserMessage("", List.of(new Media(MimeTypeUtils.IMAGE_PNG, resource)));
        // springAi doesn't allow for using images as input- yet
        Prompt prompt = new Prompt(List.of(systemMessage, userMessage));
        ChatResponse call = springClient.call(prompt);
        String content = call.getResult().getOutput().getContent();
        ResponseEntity<AnswerResponse> answerResponseResponseEntity = postAnswer(content);
        log.info(answerResponseResponseEntity.getStatusCode().toString());
    }

    private record GnomeTask(
        int code,
        String msg,
        String hint,
        URI url
    ) {
    }
}
