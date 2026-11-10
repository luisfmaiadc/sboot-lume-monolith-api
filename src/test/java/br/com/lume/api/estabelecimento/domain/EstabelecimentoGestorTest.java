package br.com.lume.api.estabelecimento.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.lume.api.shared.exception.BusinessRuleException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("EstabelecimentoGestor e EstabelecimentoGestorId - Testes Unitários de Domínio")
class EstabelecimentoGestorTest {

    private static final OffsetDateTime AGORA = OffsetDateTime.of(2026, 5, 10, 14, 30, 0, 0, ZoneOffset.UTC);
    private static final UUID ESTABELECIMENTO_ID = UUID.fromString("11111111-2222-3333-4444-555555555555");
    private static final UUID USUARIO_ID = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee");

    @Nested
    @DisplayName("vincular(...) e desativar()")
    class VincularEDesativar {

        @Test
        @DisplayName("Deve vincular gestor ativo com cargo normalizado e permitir desativação")
        void deveVincularEDesativarComSucesso() {
            EstabelecimentoGestor gestor = EstabelecimentoGestor.vincular(
                    ESTABELECIMENTO_ID,
                    USUARIO_ID,
                    "  GESTOR_RESPONSAVEL  ",
                    USUARIO_ID,
                    AGORA
            );

            assertThat(gestor.getId()).isEqualTo(new EstabelecimentoGestorId(ESTABELECIMENTO_ID, USUARIO_ID));
            assertThat(gestor.getEstabelecimentoId()).isEqualTo(ESTABELECIMENTO_ID);
            assertThat(gestor.getUsuarioId()).isEqualTo(USUARIO_ID);
            assertThat(gestor.getCargo()).isEqualTo("GESTOR_RESPONSAVEL");
            assertThat(gestor.isAtivo()).isTrue();
            assertThat(gestor.getVinculadoEm()).isEqualTo(AGORA);
            assertThat(gestor.getVinculadoPor()).isEqualTo(USUARIO_ID);

            gestor.desativar();
            assertThat(gestor.isAtivo()).isFalse();
        }

        @Test
        @DisplayName("Deve lançar BusinessRuleException quando estabelecimentoId, usuarioId ou agora forem nulos")
        void deveRejeitarParametrosObrigatoriosNulos() {
            assertThatThrownBy(() -> EstabelecimentoGestor.vincular(
                    null, USUARIO_ID, "GESTOR", USUARIO_ID, AGORA
            ))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessage("Identificador do estabelecimento é obrigatório para vínculo de gestor.");

            assertThatThrownBy(() -> EstabelecimentoGestor.vincular(
                    ESTABELECIMENTO_ID, null, "GESTOR", USUARIO_ID, AGORA
            ))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessage("Identificador do usuário é obrigatório para vínculo de gestor.");

            assertThatThrownBy(() -> EstabelecimentoGestor.vincular(
                    ESTABELECIMENTO_ID, USUARIO_ID, "GESTOR", USUARIO_ID, null
            ))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessage("Referência temporal é obrigatória para vínculo de gestor.");
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   ", "\t"})
        @DisplayName("Deve lançar BusinessRuleException quando cargo for nulo ou em branco")
        void deveRejeitarCargoNuloOuEmBranco(String cargoInvalido) {
            assertThatThrownBy(() -> EstabelecimentoGestor.vincular(
                    ESTABELECIMENTO_ID, USUARIO_ID, cargoInvalido, USUARIO_ID, AGORA
            ))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessage("Cargo do gestor é obrigatório.");
        }

        @Test
        @DisplayName("Deve lançar BusinessRuleException quando cargo exceder 50 caracteres")
        void deveRejeitarCargoMaiorQue50Caracteres() {
            String cargoLongo = "C".repeat(51);

            assertThatThrownBy(() -> EstabelecimentoGestor.vincular(
                    ESTABELECIMENTO_ID, USUARIO_ID, cargoLongo, USUARIO_ID, AGORA
            ))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessage("Cargo do gestor deve ter no máximo 50 caracteres.");
        }
    }

    @Nested
    @DisplayName("equals e hashCode")
    class EqualsAndHashCode {

        @Test
        @DisplayName("Deve comparar EstabelecimentoGestor e EstabelecimentoGestorId pela chave composta")
        void deveCompararPelaChaveComposta() {
            EstabelecimentoGestor g1 = EstabelecimentoGestor.vincular(
                    ESTABELECIMENTO_ID, USUARIO_ID, "GESTOR", USUARIO_ID, AGORA
            );
            EstabelecimentoGestor g2 = EstabelecimentoGestor.vincular(
                    ESTABELECIMENTO_ID, USUARIO_ID, "SOCIO", UUID.randomUUID(), AGORA.plusHours(1)
            );
            EstabelecimentoGestor gDiferente = EstabelecimentoGestor.vincular(
                    UUID.randomUUID(), USUARIO_ID, "GESTOR", USUARIO_ID, AGORA
            );

            assertThat(g1)
                    .isEqualTo(g1)
                    .isEqualTo(g2)
                    .hasSameHashCodeAs(g2)
                    .isNotEqualTo(gDiferente)
                    .isNotEqualTo(null)
                    .isNotEqualTo("outro");

            EstabelecimentoGestorId id1 = new EstabelecimentoGestorId(ESTABELECIMENTO_ID, USUARIO_ID);
            EstabelecimentoGestorId id2 = new EstabelecimentoGestorId(ESTABELECIMENTO_ID, USUARIO_ID);
            EstabelecimentoGestorId idDiferente = new EstabelecimentoGestorId(ESTABELECIMENTO_ID, UUID.randomUUID());

            assertThat(id1)
                    .isEqualTo(id2)
                    .hasSameHashCodeAs(id2)
                    .isNotEqualTo(idDiferente);
        }
    }
}
