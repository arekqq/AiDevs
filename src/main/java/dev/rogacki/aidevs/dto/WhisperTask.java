package dev.rogacki.aidevs.dto;

import lombok.Getter;

@Getter
public class WhisperTask extends Task {
    private final String hint;

    public WhisperTask(Integer code, String msg, String hint) {
        super(code, msg);
        this.hint = hint;
    }
}
