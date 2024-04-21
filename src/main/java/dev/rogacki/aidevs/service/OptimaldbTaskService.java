package dev.rogacki.aidevs.service;

import dev.rogacki.aidevs.dto.AnswerResponse;
import dev.rogacki.aidevs.external.TaskClient;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatClient;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class OptimaldbTaskService extends Task {

    private static final String PROMPT = """
        Transform string array into bullet point list that contains SAME AMOUNT OF ELEMENTS.
        Focus on facts. Make each bullet point more concise than array element, so it carries the same information.
        Answer ULTRA briefly.
        Omit the name of person. In case there is the name, replace it with "he" or "she", "his" or "hers".
          ###Example Input: ["Jennifer Lopez to inspiracja fitnessowa dla Ani, zwłaszcza jeśli chodzi o taniec i ruch sceniczny.", "W wolnych chwilach Ania prowadzi kanał na YouTube, gdzie dzieli się poradami z zakresu beauty."]
          ###Example Output:
          fitness inspiration is Jeniffer Lopez, has YT beuaty channel
      
        """;
    public static final SystemMessage SYSTEM_MESSAGE = new SystemMessage(PROMPT);
    public static final int SUBLIST_SIZE = 8;

    public OptimaldbTaskService(TaskClient taskClient,
                                OpenAiChatClient springClient) {
        super(taskClient, springClient);
    }

    @SneakyThrows
    @Override
    public void run() {
        String taskName = "optimaldb";
        var optimaldbTask = getTask(taskName, OptimalDbTask.class);
        Map<String, List<String>> personsDb = RestClient.create(optimaldbTask.database().toString())
            .get()
            .retrieve()
            .body(new ParameterizedTypeReference<>() {
            });
        Map<String, String> optimizedDb = new HashMap<>();
        personsDb.forEach((k, v) -> {
            List<String> optimizedEntries = new ArrayList<>();
            for (int i = 0; i < v.size(); i += SUBLIST_SIZE) {
                int subListToIndex = Math.min(i + SUBLIST_SIZE, v.size());
                log.info("Optimizing {} to {} entries from {}", i, subListToIndex, v.size());
                String optimizedEntry = optimizeData(v.subList(i, subListToIndex));
                optimizedEntries.add(optimizedEntry);
            }
//            String s = optimizeData(optimizedEntries);
//            log.info(s);
            optimizedDb.put(k, optimizedEntries.toString());
        });
        ResponseEntity<AnswerResponse> answerResponseResponseEntity = postAnswer(optimizedDb.toString());
        log.info(answerResponseResponseEntity.getStatusCode().toString());
    }

    private String optimizeData(List<String> input) {
        var userMessage = new UserMessage(input.toString());
        return springClient.call(new Prompt(List.of(SYSTEM_MESSAGE, userMessage))).getResult().getOutput().getContent();
    }

    private record Render(
        String requestId,
        URI href
    ) {
    }

    private record OptimalDbTask(
        int code,
        String msg,
        URI database,
        String hint
    ) {
    }
}
