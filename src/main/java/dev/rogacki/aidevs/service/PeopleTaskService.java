package dev.rogacki.aidevs.service;

import dev.rogacki.aidevs.dto.AnswerResponse;
import dev.rogacki.aidevs.external.TaskClient;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.openai.OpenAiChatClient;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class PeopleTaskService extends Task {


    public PeopleTaskService(TaskClient taskClient,
                             OpenAiChatClient springClient) {
        super(taskClient, springClient);
    }

    @SneakyThrows
    @Override
    public void run() {
        var peopleTask = getTask("people", PeopleTask.class);
        log.info(peopleTask.question());
        String nameAndSurname = springClient.call(STR."""
            Z poniższego pytania zwróć imię i nazwisko w mianowniku.
            Np: Jaka planeta podoba się Janowi Kowalskiemu?
            Jan Kowalski
            \{peopleTask.question()}
            """);
        log.info(nameAndSurname);
        String[] splitted = nameAndSurname.split(" ");

        var data = Optional.ofNullable(RestClient.create(peopleTask.data().toString())
            .get()
            .retrieve()
            .body(new ParameterizedTypeReference<List<PeopleData>>() {
            })).orElseThrow();
        PeopleData peopleData = data.stream()
            .filter(p -> p.imie().equals(splitted[0]) && p.nazwisko().equals(splitted[1]))
            .findFirst()
            .orElseThrow();
        String response = springClient.call(STR."""
            \{peopleData.toString()}
            Odpowiedz na pytanie: \{peopleTask.question()}
            """);
        ResponseEntity<AnswerResponse> answerResponseResponseEntity = postAnswer(response);
        log.info(answerResponseResponseEntity.getStatusCode().toString());
    }

    private record PeopleData(
        String imie,
        String nazwisko,
        int wiek,
        String o_mnie,
        String ulubiona_postac_z_kapitana_bomby,
        String ulubiony_serial,
        String ulubiony_film,
        String ulubiony_kolor
    ) {
    }

    private record PeopleTask(
        int code,
        String msg,
        URI data,
        String question,
        String hint1,
        String hint2
    ) {
    }
}
