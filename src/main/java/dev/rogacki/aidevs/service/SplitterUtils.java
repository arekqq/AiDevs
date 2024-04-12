package dev.rogacki.aidevs.service;

import lombok.experimental.UtilityClass;

@UtilityClass
public class SplitterUtils {
    String extractLinkFromLast(String input) {
        String[] split = input.split(" ");
        return split[split.length - 1];
    }
}
