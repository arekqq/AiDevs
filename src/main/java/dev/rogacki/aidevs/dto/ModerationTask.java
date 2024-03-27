package dev.rogacki.aidevs.dto;

import java.util.List;

public record ModerationTask(
    Integer code,
    String msg,
    List<String> input
) {
}
