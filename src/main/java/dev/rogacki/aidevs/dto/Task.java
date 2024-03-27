package dev.rogacki.aidevs.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public abstract class Task {
    private final Integer code;
    private final String msg;
}
