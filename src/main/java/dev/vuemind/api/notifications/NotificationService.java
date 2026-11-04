package dev.vuemind.api.notifications;

import dev.vuemind.api.common.ApiException;
import dev.vuemind.api.notifications.dto.NotificationDto;
import dev.vuemind.api.store.InMemoryStore;
import dev.vuemind.api.store.model.Notification;
import dev.vuemind.api.transfers.PixService;
import java.util.Comparator;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class NotificationService {

    private final InMemoryStore store;
    private final PixService pixService;

    public NotificationService(InMemoryStore store, PixService pixService) {
        this.store = store;
        this.pixService = pixService;
    }

    public List<NotificationDto> list() {
        pixService.processDueScheduledTransfers();
        return store.notifications().stream()
                .sorted(Comparator.comparing(Notification::createdAt).reversed())
                .map(item -> new NotificationDto(item.id(), item.title(), item.body(), item.read(), item.createdAt()))
                .toList();
    }

    public synchronized void markRead(String id) {
        List<Notification> notifications = store.notifications();
        for (int index = 0; index < notifications.size(); index++) {
            Notification notification = notifications.get(index);
            if (notification.id().equals(id)) {
                notifications.set(index, notification.markedRead());
                return;
            }
        }
        throw new ApiException(HttpStatus.NOT_FOUND, "NOTIFICATION_NOT_FOUND", "Notificação não encontrada.");
    }

    public synchronized void markAllRead() {
        store.notifications().replaceAll(Notification::markedRead);
    }
}
