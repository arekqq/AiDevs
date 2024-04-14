package dev.rogacki.aidevs.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.rogacki.aidevs.dto.AnswerResponse;
import dev.rogacki.aidevs.external.TaskClient;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.openai.OpenAiChatClient;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Map;

@Slf4j
@Service
public class ToolsTaskService extends Task {


    public ToolsTaskService(TaskClient taskClient,
                            OpenAiChatClient springClient) {
        super(taskClient, springClient);
    }

    @SneakyThrows
    @Override
    public void run() {
        var toolsTask = getTask("tools", ToolsTask.class);
        log.info(toolsTask.toString());
        String response = springClient.call(STR."""
            Basing on below message prepare JSON object to add it to proper tool (Calendar or ToDo).
            In case it's for Calendar add field "date" with proper date in format YYYY-MM-DD, in case it's for ToDo don't add `date` field
            To calculate date today is \{LocalDate.now()}
            Return only JSON object
            Example:
            {"tool":"ToDo","desc":"Kup mleko" }
            \{toolsTask.question()}
            """);
        var map = new ObjectMapper().readValue(response, new TypeReference<Map<String, String>>() {
        });
        ResponseEntity<AnswerResponse> answerResponseResponseEntity = postAnswer(map);
        log.info(response);
    }

    private record ToolsTask(
        int code,
        String msg,
        String hint,
        @JsonProperty("example for ToDo") String exampleForToDo,
        @JsonProperty("example for Calendar") String exampleForCalendar,
        String question
    ) {
    }
}
