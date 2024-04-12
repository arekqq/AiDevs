package dev.rogacki.aidevs.service;

import dev.rogacki.aidevs.dto.AnswerResponse;
import dev.rogacki.aidevs.external.TaskClient;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingClient;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.ai.openai.OpenAiChatClient;
import org.springframework.ai.openai.OpenAiEmbeddingOptions;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
public class SearchTaskService extends Task {

    private final EmbeddingClient embeddingClient;
    private final VectorStore vectorStore;

    public SearchTaskService(TaskClient taskClient,
                             OpenAiChatClient springClient,
                             EmbeddingClient embeddingClient,
                             VectorStore vectorStore) {
        super(taskClient, springClient);
        this.embeddingClient = embeddingClient;
        this.vectorStore = vectorStore;
    }

    @SneakyThrows
    @Override
    public void run() {
        var searchTask = getTask("search", SearchTask.class);
        log.info(searchTask.toString());
        String sourceUrl = SplitterUtils.extractLinkFromLast(searchTask.msg());
        var links = Optional.ofNullable(RestClient.create(sourceUrl)
            .get()
            .retrieve()
            .body(new ParameterizedTypeReference<List<Link>>() {
            })).orElseThrow();
        List<String> linksInfo = links.stream()
            .map(link -> link.title() + link.info())
            .toList();
        EmbeddingRequest embeddingRequest = new EmbeddingRequest(linksInfo, OpenAiEmbeddingOptions.builder().build());
        EmbeddingResponse embeddingResponse = embeddingClient.call(embeddingRequest);
        var documents = embeddingResponse.getResults().stream()
            .map(embedding -> {
                var link = links.get(embedding.getIndex());
                Map<String, Object> metadata = Map.of(
                    "link", link.url().toString(),
                    "info", link.info(),
                    "date", link.date().toString());
                var document =  new Document(link.title(), metadata);
                document.setEmbedding(embedding.getOutput());
                return document;
            })
            .toList();

        vectorStore.accept(documents);
        List<Document> searchResult = vectorStore.similaritySearch(searchTask.question());

        Object responseLink = searchResult.stream().findFirst().map(document -> document.getMetadata().get("link")).orElseThrow();

        ResponseEntity<AnswerResponse> answerResponseResponseEntity = postAnswer(responseLink);
        log.info(answerResponseResponseEntity.getStatusCode().toString());
    }

    private record Link(
        String title,
        URI url,
        String info,
        LocalDate date
    ) {
    }

    private record SearchTask(
        int code,
        String msg,
        String question
    ) {
    }
}
