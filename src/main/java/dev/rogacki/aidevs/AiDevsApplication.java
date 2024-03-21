package dev.rogacki.aidevs;

import dev.rogacki.aidevs.external.TaskClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.util.Optional;

@Slf4j
@SpringBootApplication
public class AiDevsApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiDevsApplication.class, args);
    }

    @Bean
    ApplicationRunner applicationRunner(TaskClient taskClient) {
        return _ -> {
            var token = taskClient.getToken("helloapi");
            var tokenBody = Optional.ofNullable(token.getBody()).orElseThrow();
            var task = taskClient.getTask(tokenBody.token());
            var answerResponse = taskClient.postAnswer(token.getBody().token(), task.get("cookie").toString());
            log.info(answerResponse.toString());
        };
    }


}
