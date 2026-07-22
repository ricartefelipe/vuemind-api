package dev.vuemind.api.transfers;

import dev.vuemind.api.transfers.dto.CreatePixRequest;
import dev.vuemind.api.transfers.dto.TransferDto;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/transfers")
public class TransfersController {

    private final PixService pixService;

    public TransfersController(PixService pixService) {
        this.pixService = pixService;
    }

    @PostMapping("/pix")
    @ResponseStatus(HttpStatus.CREATED)
    public TransferDto executePix(
            @RequestBody CreatePixRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        // Sem chave enviada pelo client, geramos uma aleatória: efetivamente
        // "sem deduplicação" para essa chamada, igual ao comportamento do mock.
        String key = (idempotencyKey != null && !idempotencyKey.isBlank())
                ? idempotencyKey
                : UUID.randomUUID().toString();
        return pixService.executePix(request, key);
    }
}
