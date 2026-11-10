package br.com.lume.api.shared.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.lume.api.shared.exception.UnauthorizedException;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

@DisplayName("AuthenticatedUserProvider - Testes Unitários")
class AuthenticatedUserProviderTest {

    private static final UUID USUARIO_ID = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
    private static final String EMAIL = "usuario@lume.com.br";

    private AuthenticatedUserProvider provider;

    @BeforeEach
    void setUp() {
        provider = new AuthenticatedUserProvider();
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Nested
    @DisplayName("Extração de AuthenticatedUser e ID do Contexto de Segurança")
    class ExtracaoUsuarioAutenticado {

        @Test
        @DisplayName("Deve extrair AuthenticatedUser quando principal já for instância de AuthenticatedUser")
        void deveExtrairQuandoPrincipalForAuthenticatedUser() {
            AuthenticatedUser expectedUser = new AuthenticatedUser(USUARIO_ID, EMAIL, List.of("ROLE_CIDADAO"));
            UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                    expectedUser, null, List.of(new SimpleGrantedAuthority("ROLE_CIDADAO"))
            );
            SecurityContextHolder.getContext().setAuthentication(auth);

            assertThat(provider.getUsuarioAutenticado()).isEqualTo(expectedUser);
            assertThat(provider.getUsuarioIdAutenticado()).isEqualTo(USUARIO_ID);
            assertThat(provider.findUsuarioIdAutenticado()).contains(USUARIO_ID);
        }

        @Test
        @DisplayName("Deve garantir lista imutável vazia quando AuthenticatedUser for instanciado com roles nulas")
        void deveGarantirRolesVaziaQuandoAuthenticatedUserReceberNull() {
            AuthenticatedUser user = new AuthenticatedUser(USUARIO_ID, EMAIL, null);

            assertThat(user.roles()).isEmpty();
        }

        @Test
        @DisplayName("Deve extrair AuthenticatedUser quando principal for Jwt com authorities no Authentication")
        void deveExtrairQuandoPrincipalForJwtComAuthorities() {
            Jwt jwt = Jwt.withTokenValue("token-valor")
                    .header("alg", "HS256")
                    .subject(USUARIO_ID.toString())
                    .claim("email", EMAIL)
                    .issuedAt(Instant.parse("2026-05-10T14:00:00Z"))
                    .expiresAt(Instant.parse("2026-05-10T14:15:00Z"))
                    .build();
            JwtAuthenticationToken auth = new JwtAuthenticationToken(
                    jwt, List.of(new SimpleGrantedAuthority("ROLE_GESTOR"))
            );
            SecurityContextHolder.getContext().setAuthentication(auth);

            AuthenticatedUser user = provider.getUsuarioAutenticado();

            assertThat(user.id()).isEqualTo(USUARIO_ID);
            assertThat(user.email()).isEqualTo(EMAIL);
            assertThat(user.roles()).containsExactly("ROLE_GESTOR");
        }

        @Test
        @DisplayName("Deve extrair roles da claim 'roles' do Jwt quando authorities da Authentication estiverem vazias")
        void deveExtrairRolesDaClaimDoJwtQuandoAuthoritiesVazias() {
            Jwt jwt = Jwt.withTokenValue("token-valor")
                    .header("alg", "HS256")
                    .subject(USUARIO_ID.toString())
                    .claim("email", EMAIL)
                    .claim("roles", List.of("ROLE_ADMIN"))
                    .build();
            JwtAuthenticationToken auth = new JwtAuthenticationToken(jwt, Collections.emptyList());
            SecurityContextHolder.getContext().setAuthentication(auth);

            AuthenticatedUser user = provider.getUsuarioAutenticado();

            assertThat(user.id()).isEqualTo(USUARIO_ID);
            assertThat(user.roles()).containsExactly("ROLE_ADMIN");
        }

        @Test
        @DisplayName("Deve retornar lista de roles vazia quando Jwt não tiver authorities nem claim 'roles'")
        void deveRetornarRolesVaziaQuandoJwtSemAuthoritiesESemClaimRoles() {
            Jwt jwt = Jwt.withTokenValue("token-valor")
                    .header("alg", "HS256")
                    .subject(USUARIO_ID.toString())
                    .claim("email", EMAIL)
                    .build();
            JwtAuthenticationToken auth = new JwtAuthenticationToken(jwt, Collections.emptyList());
            SecurityContextHolder.getContext().setAuthentication(auth);

            AuthenticatedUser user = provider.getUsuarioAutenticado();

            assertThat(user.id()).isEqualTo(USUARIO_ID);
            assertThat(user.roles()).isEmpty();
        }

        @Test
        @DisplayName("Deve extrair AuthenticatedUser quando principal for String contendo UUID válido")
        void deveExtrairQuandoPrincipalForStringUuidValido() {
            UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                    USUARIO_ID.toString(), null, List.of(new SimpleGrantedAuthority("ROLE_CIDADAO"))
            );
            SecurityContextHolder.getContext().setAuthentication(auth);

            AuthenticatedUser user = provider.getUsuarioAutenticado();

            assertThat(user.id()).isEqualTo(USUARIO_ID);
            assertThat(user.email()).isNull();
            assertThat(user.roles()).containsExactly("ROLE_CIDADAO");
        }
    }

    @Nested
    @DisplayName("Cenários de Usuário Não Autenticado ou Principal Inválido")
    class CenariosNaoAutenticado {

        @Test
        @DisplayName("Deve lançar UnauthorizedException quando SecurityContext não possuir Authentication")
        void deveLancarExcecaoQuandoAuthenticationNula() {
            assertThat(provider.findUsuarioIdAutenticado()).isEmpty();
            assertThatThrownBy(() -> provider.getUsuarioIdAutenticado())
                    .isInstanceOf(UnauthorizedException.class)
                    .hasMessage("Usuário não está autenticado no contexto atual.");
            assertThatThrownBy(() -> provider.getUsuarioAutenticado())
                    .isInstanceOf(UnauthorizedException.class)
                    .hasMessage("Usuário não está autenticado no contexto atual.");
        }

        @Test
        @DisplayName("Deve retornar vazio quando Authentication não estiver autenticada")
        void deveRetornarVazioQuandoAuthenticationNaoAutenticada() {
            UsernamePasswordAuthenticationToken authNaoAutenticada =
                    new UsernamePasswordAuthenticationToken(USUARIO_ID.toString(), "senha");
            SecurityContextHolder.getContext().setAuthentication(authNaoAutenticada);

            assertThat(provider.findUsuarioIdAutenticado()).isEmpty();
        }

        @Test
        @DisplayName("Deve retornar vazio quando Authentication for AnonymousAuthenticationToken")
        void deveRetornarVazioQuandoAnonymousAuthenticationToken() {
            AnonymousAuthenticationToken anonymousAuth = new AnonymousAuthenticationToken(
                    "key", "anonymousUser", List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))
            );
            SecurityContextHolder.getContext().setAuthentication(anonymousAuth);

            assertThat(provider.findUsuarioIdAutenticado()).isEmpty();
        }

        @Test
        @DisplayName("Deve retornar vazio quando Jwt possuir subject inválido que não é UUID")
        void deveRetornarVazioQuandoJwtComSubjectInvalido() {
            Jwt jwtInvalido = Jwt.withTokenValue("token-valor")
                    .header("alg", "HS256")
                    .subject("nao-e-um-uuid")
                    .build();
            JwtAuthenticationToken auth = new JwtAuthenticationToken(
                    jwtInvalido, List.of(new SimpleGrantedAuthority("ROLE_CIDADAO"))
            );
            SecurityContextHolder.getContext().setAuthentication(auth);

            assertThat(provider.findUsuarioIdAutenticado()).isEmpty();
        }

        @Test
        @DisplayName("Deve retornar vazio quando principal for String que não é UUID ou tipo desconhecido")
        void deveRetornarVazioQuandoPrincipalStringInvalidaOuTipoDesconhecido() {
            UsernamePasswordAuthenticationToken authStringInvalida = new UsernamePasswordAuthenticationToken(
                    "usuario-sem-uuid", null, List.of(new SimpleGrantedAuthority("ROLE_CIDADAO"))
            );
            SecurityContextHolder.getContext().setAuthentication(authStringInvalida);
            assertThat(provider.findUsuarioIdAutenticado()).isEmpty();

            UsernamePasswordAuthenticationToken authObjetoDesconhecido = new UsernamePasswordAuthenticationToken(
                    new Object(), null, List.of(new SimpleGrantedAuthority("ROLE_CIDADAO"))
            );
            SecurityContextHolder.getContext().setAuthentication(authObjetoDesconhecido);
            Optional<UUID> resultado = provider.findUsuarioIdAutenticado();
            assertThat(resultado).isEmpty();
        }
    }

    @Nested
    @DisplayName("possuiRole(String)")
    class PossuiRole {

        @Test
        @DisplayName("Deve verificar role com ou sem prefixo ROLE_ para usuário autenticado")
        void deveVerificarRoleComOuSemPrefixo() {
            AuthenticatedUser user = new AuthenticatedUser(
                    USUARIO_ID, EMAIL, List.of("ROLE_GESTOR", "CIDADAO")
            );
            UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                    user, null, List.of()
            );
            SecurityContextHolder.getContext().setAuthentication(auth);

            assertThat(provider.possuiRole("GESTOR")).isTrue();
            assertThat(provider.possuiRole("ROLE_GESTOR")).isTrue();
            assertThat(provider.possuiRole("CIDADAO")).isTrue();
            assertThat(provider.possuiRole("ADMIN")).isFalse();
            assertThat(provider.possuiRole("ROLE_ADMIN")).isFalse();
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   ", "\t"})
        @DisplayName("Deve retornar false quando role informada for nula ou em branco")
        void deveRetornarFalseQuandoRoleNulaOuEmBranco(String roleInvalida) {
            AuthenticatedUser user = new AuthenticatedUser(USUARIO_ID, EMAIL, List.of("ROLE_ADMIN"));
            SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken(user, null, List.of())
            );

            assertThat(provider.possuiRole(roleInvalida)).isFalse();
        }

        @Test
        @DisplayName("Deve retornar false quando não houver usuário autenticado no contexto")
        void deveRetornarFalseQuandoNaoAutenticado() {
            assertThat(provider.possuiRole("CIDADAO")).isFalse();
        }
    }
}
