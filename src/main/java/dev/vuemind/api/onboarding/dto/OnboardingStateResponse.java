package dev.vuemind.api.onboarding.dto;

import java.util.List;

public record OnboardingStateResponse(List<OnboardingStepDto> steps, boolean completed) {
}
