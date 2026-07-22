package dev.vuemind.api.store.model;

/** Único usuário "cadastrado" — o mesmo `demo@vuemind.dev` / `demo123` do mock MSW do Vue. */
public record MockUser(String id, String name, String email, String password) {
}
