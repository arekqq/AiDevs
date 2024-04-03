package dev.rogacki.aidevs.service;

import dev.rogacki.aidevs.external.TaskClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.ChatResponse;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatClient;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class WhoamiTaskService extends Task {

    public static final int MAX_RETRIES = 10;

    public WhoamiTaskService(TaskClient taskClient, OpenAiChatClient springClient) {
        super(taskClient, springClient);
    }

    @Override
    public void run() {

        String systemMessame = """
            Spróbuj odgadnąć kim jestem, w każdej następnej odpowiedzi podam nową ciekawostkę o sobie.
            Odpowiadaj tylko imieniem i nazwiskiem.
            Jeśli jesteś pewien odpowiedzi, nie zmieniaj jej- odpowiadaj tym samym w nastepnych wiadomościach.
            Jeśli nie wiesz odpowiedz: ...
            """;
        List<Message> messages = new ArrayList<>(List.of(new SystemMessage(systemMessame)));

        var hints = new ArrayList<>();
        for (int i = 0; i < MAX_RETRIES; i++) {
            var task = this.getTask("whoami", WhoamiTask.class);
            String hint = task.hint();
            log.info(STR."Hint: \{hint}");
            hints.add(hint);
            messages.add(new UserMessage(hint));
            ChatResponse call = springClient.call(new Prompt(messages));
            AssistantMessage output = call.getResult().getOutput();
            log.info(STR."Output: \{output.toString()}");
            var result = postAnswer(output.getContent());
            if (result.getStatusCode() == HttpStatus.OK) {
                log.info("I'm done guessing who I am. Success!");
                break;
            }
            messages.add(output);
        }
        log.error("I'm done guessing who I am. No success.");
        log.info(hints.toString());
    }

    private record WhoamiTask(
        String code,
        String msg,
        String hint
    ) {
    }
}
