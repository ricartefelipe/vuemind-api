package dev.vuemind.api.auth;

import dev.vuemind.api.auth.dto.LoginRequest;
import dev.vuemind.api.auth.dto.LoginResponse;
import dev.vuemind.api.auth.dto.UserDto;
import dev.vuemind.api.common.ApiException;
import dev.vuemind.api.security.MockBearerTokenFilter;
import dev.vuemind.api.store.InMemoryStore;
import dev.vuemind.api.store.model.MockUser;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final InMemoryStore store;

    public AuthService(InMemoryStore store) {
        this.store = store;
    }

    public LoginResponse login(LoginRequest request) {
        MockUser user = store.user();

        boolean credenciaisValidas = user.email().equals(request.email())
                && user.password().equals(request.password());

        if (!credenciaisValidas) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "Email ou senha inválidos.");
        }

        UserDto userDto = new UserDto(user.id(), user.name(), user.email());
        return new LoginResponse(MockBearerTokenFilter.MOCK_TOKEN, userDto);
    }
}
