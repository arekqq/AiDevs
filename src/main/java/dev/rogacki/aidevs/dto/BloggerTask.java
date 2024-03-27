package dev.rogacki.aidevs.dto;

import java.util.List;

public record BloggerTask(
    Integer code,
    String msg,
    List<String> blog
) {
}
