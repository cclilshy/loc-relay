package com.cclilshy.tayd.common.log;

import java.util.ArrayList;
import java.util.List;

public final class BoundedLogBuffer {
    private BoundedLogBuffer() {
    }

    public static String append(String existing, String line, int maxLines) {
        String trimmed = line == null ? "" : line.trim();
        if (trimmed.isEmpty()) {
            return existing == null ? "" : existing;
        }
        List<String> lines = new ArrayList<>();
        if (existing != null && !existing.trim().isEmpty()) {
            for (String item : existing.split("\\n")) {
                if (!item.trim().isEmpty()) {
                    lines.add(item);
                }
            }
        }
        lines.add(trimmed);
        int from = Math.max(0, lines.size() - Math.max(1, maxLines));
        return String.join("\n", lines.subList(from, lines.size()));
    }
}
