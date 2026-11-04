package dev.vuemind.api.beneficiaries;

import dev.vuemind.api.beneficiaries.dto.BeneficiaryDto;
import dev.vuemind.api.beneficiaries.dto.CreateBeneficiaryRequest;
import dev.vuemind.api.common.ApiException;
import dev.vuemind.api.store.InMemoryStore;
import dev.vuemind.api.store.model.Beneficiary;
import dev.vuemind.api.store.model.OnboardingStepCode;
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
        if (isBlank(request.name()) || isBlank(request.pixKey()) || request.pixKeyType() == null) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST, "INVALID_BENEFICIARY", "Nome, tipo e chave PIX são obrigatórios.");
        }
        String pixKey = request.pixKey().trim();
        PixKeyValidator.assertValid(request.pixKeyType(), pixKey);

        Beneficiary beneficiary = new Beneficiary(
                UUID.randomUUID().toString(), request.name().trim(), pixKey, request.pixKeyType());
        store.beneficiaries().add(beneficiary);
        store.completedSteps().add(OnboardingStepCode.FIRST_BENEFICIARY);
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
        return new BeneficiaryDto(beneficiary.id(), beneficiary.name(), beneficiary.pixKey(), beneficiary.pixKeyType());
    }
}
