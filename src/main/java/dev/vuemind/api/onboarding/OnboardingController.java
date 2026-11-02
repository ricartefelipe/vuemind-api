package dev.vuemind.api.onboarding;

import dev.vuemind.api.onboarding.dto.OnboardingStateResponse;
import dev.vuemind.api.onboarding.dto.OnboardingStepDto;
import dev.vuemind.api.store.InMemoryStore;
import dev.vuemind.api.store.model.OnboardingStepCode;
import java.util.Arrays;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/me")
public class OnboardingController {

    private final InMemoryStore store;

    public OnboardingController(InMemoryStore store) {
        this.store = store;
    }

    @GetMapping("/onboarding")
    public OnboardingStateResponse getOnboarding() {
        List<OnboardingStepDto> steps = Arrays.stream(OnboardingStepCode.values())
                .map(code -> new OnboardingStepDto(code.name(), store.completedSteps().contains(code)))
                .toList();
        return new OnboardingStateResponse(steps, steps.stream().allMatch(OnboardingStepDto::done));
    }
}
