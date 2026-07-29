package dev.vuemind.api.wallet.reactive;

import dev.vuemind.api.wallet.WalletService;
import dev.vuemind.api.wallet.dto.BalanceResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1/reactive/wallet")
public class ReactiveWalletController {

    private final WalletService walletService;

    public ReactiveWalletController(WalletService walletService) {
        this.walletService = walletService;
    }

    @GetMapping("/balance")
    public Mono<BalanceResponse> getBalance() {
        return Mono.fromSupplier(walletService::getBalance);
    }
}
