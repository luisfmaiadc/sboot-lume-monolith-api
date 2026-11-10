package br.com.lume.api.identidade.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.lume.api.identidade.domain.Perfil;
import br.com.lume.api.identidade.domain.Usuario;
import br.com.lume.api.model.UsuarioDetalheResponse;
import br.com.lume.api.model.UsuarioResumoResponse;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

@DisplayName("UsuarioMapper - Testes Unitários")
class UsuarioMapperTest {

    private static final OffsetDateTime AGORA = OffsetDateTime.of(2026, 5, 10, 14, 30, 0, 0, ZoneOffset.UTC);
    private static final String CPF_HASH = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855";

    private UsuarioMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = Mappers.getMapper(UsuarioMapper.class);
    }

    @Nested
    @DisplayName("Mapeamento de Enums")
    class MapeamentoEnums {

        @Test
        @DisplayName("Deve mapear Genero bidirecionalmente incluindo nulo")
        void deveMapearGeneroBidirecionalmente() {
            assertThat(mapper.toDomain(br.com.lume.api.model.Genero.MULHER_CIS))
                    .isEqualTo(br.com.lume.api.identidade.domain.Genero.MULHER_CIS);
            assertThat(mapper.toApi(br.com.lume.api.identidade.domain.Genero.MULHER_CIS))
                    .isEqualTo(br.com.lume.api.model.Genero.MULHER_CIS);
            assertThat(mapper.toDomain((br.com.lume.api.model.Genero) null)).isNull();
            assertThat(mapper.toApi((br.com.lume.api.identidade.domain.Genero) null)).isNull();
        }

        @Test
        @DisplayName("Deve mapear OrientacaoSexual bidirecionalmente incluindo ASSEXUAL")
        void deveMapearOrientacaoSexualBidirecionalmente() {
            assertThat(mapper.toDomain(br.com.lume.api.model.OrientacaoSexual.ASSEXUAL))
                    .isEqualTo(br.com.lume.api.identidade.domain.OrientacaoSexual.ASSEXUAL);
            assertThat(mapper.toApi(br.com.lume.api.identidade.domain.OrientacaoSexual.ASSEXUAL))
                    .isEqualTo(br.com.lume.api.model.OrientacaoSexual.ASSEXUAL);
            assertThat(mapper.toDomain((br.com.lume.api.model.OrientacaoSexual) null)).isNull();
            assertThat(mapper.toApi((br.com.lume.api.identidade.domain.OrientacaoSexual) null)).isNull();
        }

        @Test
        @DisplayName("Deve mapear RacaEtnia bidirecionalmente")
        void deveMapearRacaEtniaBidirecionalmente() {
            assertThat(mapper.toDomain(br.com.lume.api.model.RacaEtnia.PARDA))
                    .isEqualTo(br.com.lume.api.identidade.domain.RacaEtnia.PARDA);
            assertThat(mapper.toApi(br.com.lume.api.identidade.domain.RacaEtnia.PARDA))
                    .isEqualTo(br.com.lume.api.model.RacaEtnia.PARDA);
            assertThat(mapper.toDomain((br.com.lume.api.model.RacaEtnia) null)).isNull();
            assertThat(mapper.toApi((br.com.lume.api.identidade.domain.RacaEtnia) null)).isNull();
        }
    }

    @Nested
    @DisplayName("Mapeamento de DTOs")
    class MapeamentoDtos {

        @Test
        @DisplayName("Deve mapear Usuario para UsuarioResumoResponse com email e perfis")
        void deveMapearParaUsuarioResumoResponse() {
            // Arrange
            Usuario usuario = Usuario.criar(
                    "Maria da Silva",
                    "52998224725",
                    CPF_HASH,
                    "11987654321",
                    br.com.lume.api.identidade.domain.Genero.MULHER_CIS,
                    br.com.lume.api.identidade.domain.RacaEtnia.PARDA,
                    br.com.lume.api.identidade.domain.OrientacaoSexual.ASSEXUAL,
                    AGORA
            );
            usuario.adicionarPerfil(Perfil.criar(1, "ROLE_USUARIO", "Cidadão"), AGORA, usuario.getId());

            // Act
            UsuarioResumoResponse response = mapper.toResumoResponse(usuario, "maria@exemplo.com");

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.getId()).isEqualTo(usuario.getId());
            assertThat(response.getNomeCompleto()).isEqualTo("Maria da Silva");
            assertThat(response.getEmail()).isEqualTo("maria@exemplo.com");
            assertThat(response.getPerfis()).containsExactly("ROLE_USUARIO");
        }

        @Test
        @DisplayName("Deve mapear Usuario para UsuarioDetalheResponse com todos os campos")
        void deveMapearParaUsuarioDetalheResponse() {
            // Arrange
            Usuario usuario = Usuario.criar(
                    "Maria da Silva",
                    "52998224725",
                    CPF_HASH,
                    "11987654321",
                    br.com.lume.api.identidade.domain.Genero.MULHER_CIS,
                    br.com.lume.api.identidade.domain.RacaEtnia.PARDA,
                    br.com.lume.api.identidade.domain.OrientacaoSexual.ASSEXUAL,
                    AGORA
            );
            usuario.adicionarPerfil(Perfil.criar(1, "ROLE_USUARIO", "Cidadão"), AGORA, usuario.getId());

            // Act
            UsuarioDetalheResponse response = mapper.toDetalheResponse(usuario, "maria@exemplo.com");

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.getId()).isEqualTo(usuario.getId());
            assertThat(response.getNomeCompleto()).isEqualTo("Maria da Silva");
            assertThat(response.getCpf()).isEqualTo("52998224725");
            assertThat(response.getTelefone()).isEqualTo("11987654321");
            assertThat(response.getEmail()).isEqualTo("maria@exemplo.com");
            assertThat(response.getGenero()).isEqualTo(br.com.lume.api.model.Genero.MULHER_CIS);
            assertThat(response.getRacaEtnia()).isEqualTo(br.com.lume.api.model.RacaEtnia.PARDA);
            assertThat(response.getOrientacaoSexual()).isEqualTo(br.com.lume.api.model.OrientacaoSexual.ASSEXUAL);
            assertThat(response.getPerfis()).containsExactly("ROLE_USUARIO");
            assertThat(response.getCriadoEm()).isEqualTo(AGORA);
        }
    }
}
