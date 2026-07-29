package dev.vuemind.api.beneficiaries;

import dev.vuemind.api.beneficiaries.dto.BeneficiaryDto;
import dev.vuemind.api.beneficiaries.dto.CreateBeneficiaryRequest;
import dev.vuemind.api.common.ApiException;
import dev.vuemind.api.store.InMemoryStore;
import dev.vuemind.api.store.model.Beneficiary;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class BeneficiaryService {

    private final InMemoryStore store;

    public BeneficiaryService(InMemoryStore store) {
        this.store = store;
    }

    public List<BeneficiaryDto> list() {
        return store.beneficiaries().stream()
                .map(this::toDto)
                .toList();
    }

    public BeneficiaryDto create(CreateBeneficiaryRequest request) {
        if (isBlank(request.name()) || isBlank(request.pixKey())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_BENEFICIARY", "Nome e chave PIX são obrigatórios.");
        }

        Beneficiary beneficiary = new Beneficiary(UUID.randomUUID().toString(), request.name(), request.pixKey());
        store.beneficiaries().add(beneficiary);
        return toDto(beneficiary);
    }

    public void delete(String id) {
        boolean removed = store.beneficiaries().removeIf(beneficiary -> beneficiary.id().equals(id));
        if (!removed) {
            throw new ApiException(HttpStatus.NOT_FOUND, "BENEFICIARY_NOT_FOUND", "Favorecido não encontrado.");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private BeneficiaryDto toDto(Beneficiary beneficiary) {
        return new BeneficiaryDto(beneficiary.id(), beneficiary.name(), beneficiary.pixKey());
    }
}
