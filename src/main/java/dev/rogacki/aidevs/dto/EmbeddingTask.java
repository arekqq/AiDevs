package dev.rogacki.aidevs.dto;

import lombok.Getter;

@Getter
public class EmbeddingTask extends Task {
    private final String hint1;
    private final String hint2;
    private final String hint3;

    public EmbeddingTask(Integer code, String msg, String hint1, String hint2, String hint3) {
        super(code, msg);
        this.hint1 = hint1;
        this.hint2 = hint2;
        this.hint3 = hint3;
    }
}
