package dev.vuemind.api.store.model;

import java.time.Instant;

public record Notification(String id, String title, String body, boolean read, Instant createdAt) {

    public Notification markedRead() {
        return new Notification(id, title, body, true, createdAt);
    }
}
