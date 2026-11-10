package br.com.lume.api.shared.security;

import br.com.lume.api.shared.exception.UnauthorizedException;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

@Component
public class AuthenticatedUserProvider {

    public UUID getUsuarioIdAutenticado() {
        return findUsuarioIdAutenticado()
                .orElseThrow(() -> new UnauthorizedException("Usuário não está autenticado no contexto atual."));
    }

    public Optional<UUID> findUsuarioIdAutenticado() {
        return findUsuarioAutenticado().map(AuthenticatedUser::id);
    }

    public AuthenticatedUser getUsuarioAutenticado() {
        return findUsuarioAutenticado()
                .orElseThrow(() -> new UnauthorizedException("Usuário não está autenticado no contexto atual."));
    }

    public boolean possuiRole(String role) {
        if (role == null || role.isBlank()) {
            return false;
        }
        String normalizedRole = role.startsWith("ROLE_") ? role : "ROLE_" + role;
        return findUsuarioAutenticado()
                .map(user -> user.roles().stream()
                        .anyMatch(r -> r.equals(role) || r.equals(normalizedRole)))
                .orElse(false);
    }

    private Optional<AuthenticatedUser> findUsuarioAutenticado() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return Optional.empty();
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof AuthenticatedUser authenticatedUser) {
            return Optional.of(authenticatedUser);
        }

        if (principal instanceof Jwt jwt) {
            String subject = jwt.getSubject();
            if (subject == null || subject.isBlank()) {
                return Optional.empty();
            }
            try {
                UUID id = UUID.fromString(subject);
                String email = jwt.getClaimAsString("email");
                List<String> roles = extractRoles(authentication.getAuthorities(), jwt);
                return Optional.of(new AuthenticatedUser(id, email, roles));
            } catch (IllegalArgumentException ex) {
                return Optional.empty();
            }
        }

        if (principal instanceof String str) {
            try {
                UUID id = UUID.fromString(str);
                List<String> roles = authentication.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority)
                        .toList();
                return Optional.of(new AuthenticatedUser(id, null, roles));
            } catch (IllegalArgumentException ex) {
                return Optional.empty();
            }
        }

        return Optional.empty();
    }

    private List<String> extractRoles(Collection<? extends GrantedAuthority> authorities, Jwt jwt) {
        if (authorities != null && !authorities.isEmpty()) {
            return authorities.stream()
                    .map(GrantedAuthority::getAuthority)
                    .toList();
        }
        List<String> claimRoles = jwt.getClaimAsStringList("roles");
        return claimRoles != null ? claimRoles : List.of();
    }
}