package dev.rogacki.aidevs.config.web.client;


import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.util.stream.Collectors;

@Component
@Slf4j
public class LoggingInterceptor implements ClientHttpRequestInterceptor {

    @NotNull
    @Override
    public ClientHttpResponse intercept(HttpRequest request,
                                        @NotNull byte[] body,
                                        ClientHttpRequestExecution execution) throws IOException {
        log.info("\nRequest:\n{} n{}\n{}\nBody: {}",
            request.getMethod(), request.getURI(), request.getHeaders(), new String(body));
        ClientHttpResponse response = execution.execute(request, body);
        // TODO - log the body without consuming inpput stream
        log.info("\nResponse:\n{}\n{}\nBody:\n{}",
            response.getStatusCode(), response.getHeaders(), response.getBody());
        return response;
    }
}
