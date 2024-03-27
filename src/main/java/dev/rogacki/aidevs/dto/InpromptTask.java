package dev.rogacki.aidevs.dto;

import java.util.List;

public record InpromptTask(
    Integer code,
    String msg,
    List<String> input,
    String question
) {
}
