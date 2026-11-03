package dev.vuemind.api.transfers;

import dev.vuemind.api.transfers.dto.CreatePixRequest;
import dev.vuemind.api.transfers.dto.TransferDto;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestHeader(value = "X-Correlation-Id", required = false) String correlationId) {
        return pixService.executePix(request, orNew(idempotencyKey), orNew(correlationId));
    }

    @GetMapping("/{id}")
    public TransferDto findById(@PathVariable String id) {
        return pixService.findById(id);
    }

    private static String orNew(String value) {
        return (value != null && !value.isBlank()) ? value : UUID.randomUUID().toString();
    }
}
