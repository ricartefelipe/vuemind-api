package dev.vuemind.api.beneficiaries.dto;

import dev.vuemind.api.store.model.PixKeyType;

public record BeneficiaryDto(String id, String name, String pixKey, PixKeyType pixKeyType) {
}
