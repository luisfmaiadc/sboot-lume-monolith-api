package br.com.lume.api.shared.security;

import java.util.List;
import java.util.UUID;

public record AuthenticatedUser(
        UUID id,
        String email,
        List<String> roles
) {
    public AuthenticatedUser {
        roles = roles != null ? List.copyOf(roles) : List.of();
    }
}
