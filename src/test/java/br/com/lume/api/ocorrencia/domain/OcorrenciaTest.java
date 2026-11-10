package br.com.lume.api.ocorrencia.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.lume.api.shared.exception.BusinessRuleException;
import br.com.lume.api.shared.exception.ForbiddenOperationException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("Ocorrencia - Testes de Domínio")
class OcorrenciaTest {

    private static final OffsetDateTime AGORA = OffsetDateTime.of(2026, 5, 10, 14, 0, 0, 0, ZoneOffset.UTC);
    private static final LocalDate DATA_OCORRENCIA = LocalDate.of(2026, 5, 9);
    private static final UUID AUTOR_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID MODERADOR_ID = UUID.fromString("99999999-9999-9999-9999-999999999999");
    private static final UUID ADMIN_ID = UUID.fromString("88888888-8888-8888-8888-888888888888");
    private static final UUID ESTABELECIMENTO_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

    private Ocorrencia criarOcorrenciaPadrao() {
        return Ocorrencia.registrar(
                ESTABELECIMENTO_ID,
                -23.561414,
                -46.655881,
                "  Avenida Paulista  ",
                "  1578  ",
                "  Bela Vista  ",
                "  São Paulo  ",
                "  SP  ",
                DATA_OCORRENCIA,
                FaixaHorario.NOITE,
                1,
                2,
                TipoAmbiente.ESTABELECIMENTO,
                AUTOR_ID,
                AGORA
        );
    }

    @Nested
    @DisplayName("registrar")
    class Registrar {

        @Test
        @DisplayName("Deve registrar ocorrência publicada com campos higienizados, ponto SRID 4326 e totalValidacoes zero quando dados válidos")
        void deveRegistrarOcorrenciaComSucessoQuandoDadosValidos() {
            // Arrange & Act
            Ocorrencia ocorrencia = criarOcorrenciaPadrao();

            // Assert
            assertThat(ocorrencia.getId()).isNotNull();
            assertThat(ocorrencia.getEstabelecimentoId()).isEqualTo(ESTABELECIMENTO_ID);
            assertThat(ocorrencia.getLocalizacao()).isNotNull();
            assertThat(ocorrencia.getLocalizacao().getSRID()).isEqualTo(4326);
            assertThat(ocorrencia.getLatitude()).isEqualTo(-23.561414);
            assertThat(ocorrencia.getLongitude()).isEqualTo(-46.655881);
            assertThat(ocorrencia.getLogradouro()).isEqualTo("Avenida Paulista");
            assertThat(ocorrencia.getNumero()).isEqualTo("1578");
            assertThat(ocorrencia.getBairro()).isEqualTo("Bela Vista");
            assertThat(ocorrencia.getCidade()).isEqualTo("São Paulo");
            assertThat(ocorrencia.getEstado()).isEqualTo("SP");
            assertThat(ocorrencia.getDataOcorrencia()).isEqualTo(DATA_OCORRENCIA);
            assertThat(ocorrencia.getFaixaHorario()).isEqualTo(FaixaHorario.NOITE);
            assertThat(ocorrencia.getTipoViolenciaId()).isEqualTo(1);
            assertThat(ocorrencia.getMarcadorVulnerabilidadeId()).isEqualTo(2);
            assertThat(ocorrencia.getTipoAmbiente()).isEqualTo(TipoAmbiente.ESTABELECIMENTO);
            assertThat(ocorrencia.getTotalValidacoes()).isZero();
            assertThat(ocorrencia.getStatus()).isEqualTo(StatusOcorrencia.PUBLICADA);
            assertThat(ocorrencia.isPublicada()).isTrue();
            assertThat(ocorrencia.getCriadoEm()).isEqualTo(AGORA);
            assertThat(ocorrencia.getCriadoPor()).isEqualTo(AUTOR_ID);
            assertThat(ocorrencia.getAtualizadoEm()).isNull();
            assertThat(ocorrencia.getAtualizadoPor()).isNull();
        }

        @Test
        @DisplayName("Deve registrar ocorrência em via pública sem campos opcionais quando nulos ou em branco")
        void deveRegistrarOcorrenciaSemCamposOpcionaisQuandoNulosOuEmBranco() {
            // Arrange & Act
            Ocorrencia ocorrencia = Ocorrencia.registrar(
                    null,
                    -23.561414,
                    -46.655881,
                    "Rua Augusta",
                    "   ",
                    "Consolação",
                    "São Paulo",
                    "SP",
                    DATA_OCORRENCIA,
                    FaixaHorario.MADRUGADA,
                    1,
                    null,
                    TipoAmbiente.VIA_PUBLICA,
                    AUTOR_ID,
                    AGORA
            );

            // Assert
            assertThat(ocorrencia.getEstabelecimentoId()).isNull();
            assertThat(ocorrencia.getNumero()).isNull();
            assertThat(ocorrencia.getMarcadorVulnerabilidadeId()).isNull();
        }

        @Test
        @DisplayName("Deve lançar BusinessRuleException quando referência temporal agora for nula")
        void deveLancarBusinessRuleExceptionQuandoAgoraNulo() {
            assertThatThrownBy(() -> Ocorrencia.registrar(
                    null, -23.5, -46.6, "Rua A", "10", "Bairro", "Cidade", "SP",
                    DATA_OCORRENCIA, FaixaHorario.NOITE, 1, null, TipoAmbiente.VIA_PUBLICA, AUTOR_ID, null
            ))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessage("Referência temporal é obrigatória.");
        }

        @Test
        @DisplayName("Deve lançar BusinessRuleException quando criadoPor for nulo")
        void deveLancarBusinessRuleExceptionQuandoCriadoPorNulo() {
            assertThatThrownBy(() -> Ocorrencia.registrar(
                    null, -23.5, -46.6, "Rua A", "10", "Bairro", "Cidade", "SP",
                    DATA_OCORRENCIA, FaixaHorario.NOITE, 1, null, TipoAmbiente.VIA_PUBLICA, null, AGORA
            ))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessage("Autor da ocorrência é obrigatório.");
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   "})
        @DisplayName("Deve lançar BusinessRuleException quando logradouro for nulo ou vazio")
        void deveLancarBusinessRuleExceptionQuandoLogradouroVazio(String logradouroInvalido) {
            assertThatThrownBy(() -> Ocorrencia.registrar(
                    null, -23.5, -46.6, logradouroInvalido, "10", "Bairro", "Cidade", "SP",
                    DATA_OCORRENCIA, FaixaHorario.NOITE, 1, null, TipoAmbiente.VIA_PUBLICA, AUTOR_ID, AGORA
            ))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessage("Logradouro da ocorrência é obrigatório.");
        }

        @Test
        @DisplayName("Deve lançar BusinessRuleException quando logradouro tiver tamanho fora de 2 a 150 caracteres")
        void deveLancarBusinessRuleExceptionQuandoLogradouroTamanhoInvalido() {
            String logradouroCurto = "A";
            String logradouroLongo = "A".repeat(151);

            assertThatThrownBy(() -> Ocorrencia.registrar(
                    null, -23.5, -46.6, logradouroCurto, "10", "Bairro", "Cidade", "SP",
                    DATA_OCORRENCIA, FaixaHorario.NOITE, 1, null, TipoAmbiente.VIA_PUBLICA, AUTOR_ID, AGORA
            ))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessage("Logradouro da ocorrência deve ter entre 2 e 150 caracteres.");

            assertThatThrownBy(() -> Ocorrencia.registrar(
                    null, -23.5, -46.6, logradouroLongo, "10", "Bairro", "Cidade", "SP",
                    DATA_OCORRENCIA, FaixaHorario.NOITE, 1, null, TipoAmbiente.VIA_PUBLICA, AUTOR_ID, AGORA
            ))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessage("Logradouro da ocorrência deve ter entre 2 e 150 caracteres.");
        }

        @Test
        @DisplayName("Deve lançar BusinessRuleException quando número exceder 20 caracteres")
        void deveLancarBusinessRuleExceptionQuandoNumeroMuitoLongo() {
            String numeroLongo = "1".repeat(21);

            assertThatThrownBy(() -> Ocorrencia.registrar(
                    null, -23.5, -46.6, "Rua A", numeroLongo, "Bairro", "Cidade", "SP",
                    DATA_OCORRENCIA, FaixaHorario.NOITE, 1, null, TipoAmbiente.VIA_PUBLICA, AUTOR_ID, AGORA
            ))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessage("Número do endereço deve ter no máximo 20 caracteres.");
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   "})
        @DisplayName("Deve lançar BusinessRuleException quando bairro for nulo ou vazio")
        void deveLancarBusinessRuleExceptionQuandoBairroVazio(String bairroInvalido) {
            assertThatThrownBy(() -> Ocorrencia.registrar(
                    null, -23.5, -46.6, "Rua A", "10", bairroInvalido, "Cidade", "SP",
                    DATA_OCORRENCIA, FaixaHorario.NOITE, 1, null, TipoAmbiente.VIA_PUBLICA, AUTOR_ID, AGORA
            ))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessage("Bairro da ocorrência é obrigatório.");
        }

        @Test
        @DisplayName("Deve lançar BusinessRuleException quando bairro exceder 100 caracteres")
        void deveLancarBusinessRuleExceptionQuandoBairroMuitoLongo() {
            String bairroLongo = "B".repeat(101);

            assertThatThrownBy(() -> Ocorrencia.registrar(
                    null, -23.5, -46.6, "Rua A", "10", bairroLongo, "Cidade", "SP",
                    DATA_OCORRENCIA, FaixaHorario.NOITE, 1, null, TipoAmbiente.VIA_PUBLICA, AUTOR_ID, AGORA
            ))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessage("Bairro da ocorrência deve ter entre 1 e 100 caracteres.");
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   "})
        @DisplayName("Deve lançar BusinessRuleException quando cidade for nula ou vazia")
        void deveLancarBusinessRuleExceptionQuandoCidadeVazia(String cidadeInvalida) {
            assertThatThrownBy(() -> Ocorrencia.registrar(
                    null, -23.5, -46.6, "Rua A", "10", "Bairro", cidadeInvalida, "SP",
                    DATA_OCORRENCIA, FaixaHorario.NOITE, 1, null, TipoAmbiente.VIA_PUBLICA, AUTOR_ID, AGORA
            ))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessage("Cidade da ocorrência é obrigatória.");
        }

        @Test
        @DisplayName("Deve lançar BusinessRuleException quando cidade exceder 100 caracteres")
        void deveLancarBusinessRuleExceptionQuandoCidadeMuitoLonga() {
            String cidadeLonga = "C".repeat(101);

            assertThatThrownBy(() -> Ocorrencia.registrar(
                    null, -23.5, -46.6, "Rua A", "10", "Bairro", cidadeLonga, "SP",
                    DATA_OCORRENCIA, FaixaHorario.NOITE, 1, null, TipoAmbiente.VIA_PUBLICA, AUTOR_ID, AGORA
            ))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessage("Cidade da ocorrência deve ter entre 1 e 100 caracteres.");
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   "})
        @DisplayName("Deve lançar BusinessRuleException quando estado for nulo ou vazio")
        void deveLancarBusinessRuleExceptionQuandoEstadoVazio(String estadoInvalido) {
            assertThatThrownBy(() -> Ocorrencia.registrar(
                    null, -23.5, -46.6, "Rua A", "10", "Bairro", "Cidade", estadoInvalido,
                    DATA_OCORRENCIA, FaixaHorario.NOITE, 1, null, TipoAmbiente.VIA_PUBLICA, AUTOR_ID, AGORA
            ))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessage("Estado (UF) da ocorrência é obrigatório.");
        }

        @ParameterizedTest
        @ValueSource(strings = {"S", "SPA", "S1", "12", "sp"})
        @DisplayName("Deve lançar BusinessRuleException quando estado não tiver exatamente 2 letras maiúsculas")
        void deveLancarBusinessRuleExceptionQuandoEstadoFormatoInvalido(String estadoInvalido) {
            assertThatThrownBy(() -> Ocorrencia.registrar(
                    null, -23.5, -46.6, "Rua A", "10", "Bairro", "Cidade", estadoInvalido,
                    DATA_OCORRENCIA, FaixaHorario.NOITE, 1, null, TipoAmbiente.VIA_PUBLICA, AUTOR_ID, AGORA
            ))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessage("Estado (UF) deve conter exatamente 2 letras maiúsculas.");
        }

        @Test
        @DisplayName("Deve lançar BusinessRuleException quando data da ocorrência for nula ou futura no fuso horário de negócio (America/Sao_Paulo)")
        void deveLancarBusinessRuleExceptionQuandoDataOcorrenciaNulaOuFutura() {
            LocalDate dataFutura = AGORA.toLocalDate().plusDays(1);

            assertThatThrownBy(() -> Ocorrencia.registrar(
                    null, -23.5, -46.6, "Rua A", "10", "Bairro", "Cidade", "SP",
                    null, FaixaHorario.NOITE, 1, null, TipoAmbiente.VIA_PUBLICA, AUTOR_ID, AGORA
            ))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessage("Data da ocorrência é obrigatória.");

            assertThatThrownBy(() -> Ocorrencia.registrar(
                    null, -23.5, -46.6, "Rua A", "10", "Bairro", "Cidade", "SP",
                    dataFutura, FaixaHorario.NOITE, 1, null, TipoAmbiente.VIA_PUBLICA, AUTOR_ID, AGORA
            ))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessage("A data da ocorrência não pode ser futura.");

            // D3: 2026-05-11T01:30:00Z corresponde a 2026-05-10T22:30:00-03:00 em São Paulo.
            // Portanto, a data 2026-05-11 ainda é futura no fuso de negócio!
            OffsetDateTime noiteNoBrasilMadrugadaUtc = OffsetDateTime.of(2026, 5, 11, 1, 30, 0, 0, ZoneOffset.UTC);
            ZoneId zonaNegocio = ZoneId.of("America/Sao_Paulo");

            assertThatThrownBy(() -> Ocorrencia.registrar(
                    null, -23.5, -46.6, "Rua A", "10", "Bairro", "Cidade", "SP",
                    LocalDate.of(2026, 5, 11), FaixaHorario.NOITE, 1, null, TipoAmbiente.VIA_PUBLICA,
                    AUTOR_ID, noiteNoBrasilMadrugadaUtc, zonaNegocio
            ))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessage("A data da ocorrência não pode ser futura.");

            Ocorrencia validaNoBrasil = Ocorrencia.registrar(
                    null, -23.5, -46.6, "Rua A", "10", "Bairro", "Cidade", "SP",
                    LocalDate.of(2026, 5, 10), FaixaHorario.NOITE, 1, null, TipoAmbiente.VIA_PUBLICA,
                    AUTOR_ID, noiteNoBrasilMadrugadaUtc, zonaNegocio
            );
            assertThat(validaNoBrasil.getDataOcorrencia()).isEqualTo(LocalDate.of(2026, 5, 10));
        }

        @Test
        @DisplayName("Deve lançar BusinessRuleException quando faixa de horário, tipo de violência, marcador ou tipo de ambiente forem inválidos")
        void deveLancarBusinessRuleExceptionQuandoDemaisCamposObrigatoriosInvalidos() {
            assertThatThrownBy(() -> Ocorrencia.registrar(
                    null, -23.5, -46.6, "Rua A", "10", "Bairro", "Cidade", "SP",
                    DATA_OCORRENCIA, null, 1, null, TipoAmbiente.VIA_PUBLICA, AUTOR_ID, AGORA
            ))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessage("Faixa de horário da ocorrência é obrigatória.");

            assertThatThrownBy(() -> Ocorrencia.registrar(
                    null, -23.5, -46.6, "Rua A", "10", "Bairro", "Cidade", "SP",
                    DATA_OCORRENCIA, FaixaHorario.NOITE, 0, null, TipoAmbiente.VIA_PUBLICA, AUTOR_ID, AGORA
            ))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessage("Tipo de violência é obrigatório e deve ser positivo.");

            assertThatThrownBy(() -> Ocorrencia.registrar(
                    null, -23.5, -46.6, "Rua A", "10", "Bairro", "Cidade", "SP",
                    DATA_OCORRENCIA, FaixaHorario.NOITE, 1, 0, TipoAmbiente.VIA_PUBLICA, AUTOR_ID, AGORA
            ))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessage("Marcador de vulnerabilidade deve ser positivo quando informado.");

            assertThatThrownBy(() -> Ocorrencia.registrar(
                    null, -23.5, -46.6, "Rua A", "10", "Bairro", "Cidade", "SP",
                    DATA_OCORRENCIA, FaixaHorario.NOITE, 1, null, null, AUTOR_ID, AGORA
            ))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessage("Tipo de ambiente da ocorrência é obrigatório.");
        }
    }

    @Nested
    @DisplayName("permiteExclusao e excluirLogicamente")
    class ExclusaoLogica {

        @Test
        @DisplayName("Deve permitir e realizar exclusão lógica quando solicitada pelo autor em até 30 dias")
        void deveRealizarExclusaoLogicaQuandoSolicitadaPeloAutorNoPrazo() {
            Ocorrencia ocorrencia = criarOcorrenciaPadrao();
            OffsetDateTime noLimite30Dias = AGORA.plusDays(30);

            boolean permiteAntes = ocorrencia.permiteExclusao(noLimite30Dias);
            ocorrencia.excluirLogicamente(AUTOR_ID, noLimite30Dias);

            assertThat(permiteAntes).isTrue();
            assertThat(ocorrencia.getStatus()).isEqualTo(StatusOcorrencia.REMOVIDA);
            assertThat(ocorrencia.isPublicada()).isFalse();
            assertThat(ocorrencia.getAtualizadoPor()).isEqualTo(AUTOR_ID);
            assertThat(ocorrencia.getAtualizadoEm()).isEqualTo(noLimite30Dias);
            assertThat(ocorrencia.permiteExclusao(noLimite30Dias)).isFalse();
        }

        @Test
        @DisplayName("Deve retornar false em permiteExclusao quando agora for nulo ou após 30 dias")
        void deveRetornarFalseEmPermiteExclusaoQuandoAgoraNuloOuExpirado() {
            Ocorrencia ocorrencia = criarOcorrenciaPadrao();

            assertThat(ocorrencia.permiteExclusao(null)).isFalse();
            assertThat(ocorrencia.permiteExclusao(AGORA.plusDays(30).plusSeconds(1))).isFalse();
        }

        @Test
        @DisplayName("Deve lançar ForbiddenOperationException quando outro usuário tentar excluir a ocorrência")
        void deveLancarForbiddenOperationExceptionQuandoOutroUsuarioTentarExcluir() {
            Ocorrencia ocorrencia = criarOcorrenciaPadrao();
            UUID outroUsuario = UUID.fromString("99999999-9999-9999-9999-999999999999");

            assertThatThrownBy(() -> ocorrencia.excluirLogicamente(outroUsuario, AGORA.plusDays(1)))
                    .isInstanceOf(ForbiddenOperationException.class)
                    .hasMessage("Operação negada: apenas o autor original pode excluir a ocorrência.");
        }

        @Test
        @DisplayName("Deve lançar BusinessRuleException quando prazo de 30 dias tiver expirado")
        void deveLancarBusinessRuleExceptionQuandoPrazoDe30DiasExpirado() {
            Ocorrencia ocorrencia = criarOcorrenciaPadrao();
            OffsetDateTime apos30Dias = AGORA.plusDays(30).plusSeconds(1);

            assertThatThrownBy(() -> ocorrencia.excluirLogicamente(AUTOR_ID, apos30Dias))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessage("Prazo de exclusão expirado: a ocorrência só pode ser excluída em até 30 dias após sua publicação.");
        }

        @Test
        @DisplayName("Deve lançar BusinessRuleException quando ocorrência já estiver removida ou parâmetros forem nulos")
        void deveLancarBusinessRuleExceptionQuandoJaRemovidaOuParametrosNulos() {
            Ocorrencia ocorrencia = criarOcorrenciaPadrao();

            assertThatThrownBy(() -> ocorrencia.excluirLogicamente(null, AGORA))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessage("Identificador do usuário solicitante é obrigatório.");

            assertThatThrownBy(() -> ocorrencia.excluirLogicamente(AUTOR_ID, null))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessage("Referência temporal é obrigatória.");

            ocorrencia.excluirLogicamente(AUTOR_ID, AGORA.plusDays(1));

            assertThatThrownBy(() -> ocorrencia.excluirLogicamente(AUTOR_ID, AGORA.plusDays(2)))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessage("A ocorrência já foi removida anteriormente.");
        }
    }

    @Nested
    @DisplayName("Moderação: atualizarStatusPorModerador e atualizarStatusPorAdmin")
    class Moderacao {

        @Test
        @DisplayName("Moderador pode colocar em SOB_ANALISE e REMOVIDA, ou restaurar de SOB_ANALISE para PUBLICADA")
        void moderadorPodeAlterarStatus() {
            Ocorrencia ocorrencia = criarOcorrenciaPadrao();
            OffsetDateTime modTime = AGORA.plusHours(1);

            ocorrencia.atualizarStatusPorModerador(StatusOcorrencia.SOB_ANALISE, MODERADOR_ID, modTime);
            assertThat(ocorrencia.getStatus()).isEqualTo(StatusOcorrencia.SOB_ANALISE);
            assertThat(ocorrencia.getAtualizadoPor()).isEqualTo(MODERADOR_ID);
            assertThat(ocorrencia.getAtualizadoEm()).isEqualTo(modTime);

            // restaura para PUBLICADA
            ocorrencia.atualizarStatusPorModerador(StatusOcorrencia.PUBLICADA, MODERADOR_ID, modTime.plusHours(1));
            assertThat(ocorrencia.getStatus()).isEqualTo(StatusOcorrencia.PUBLICADA);

            // remove
            ocorrencia.atualizarStatusPorModerador(StatusOcorrencia.REMOVIDA, MODERADOR_ID, modTime.plusHours(2));
            assertThat(ocorrencia.getStatus()).isEqualTo(StatusOcorrencia.REMOVIDA);

            // tentar restaurar quando REMOVIDA deve lançar ForbiddenOperationException
            assertThatThrownBy(() -> ocorrencia.atualizarStatusPorModerador(StatusOcorrencia.PUBLICADA, MODERADOR_ID, modTime.plusHours(3)))
                    .isInstanceOf(ForbiddenOperationException.class)
                    .hasMessage("Ocorrência removida não pode ser restaurada por moderadores.");
        }

        @Test
        @DisplayName("Administrador pode restaurar mesmo que status seja REMOVIDA")
        void adminPodeRestaurarRemovida() {
            Ocorrencia ocorrencia = criarOcorrenciaPadrao();
            ocorrencia.excluirLogicamente(AUTOR_ID, AGORA.plusDays(1));
            assertThat(ocorrencia.getStatus()).isEqualTo(StatusOcorrencia.REMOVIDA);

            OffsetDateTime adminTime = AGORA.plusDays(2);
            ocorrencia.atualizarStatusPorAdmin(StatusOcorrencia.PUBLICADA, ADMIN_ID, adminTime);

            assertThat(ocorrencia.getStatus()).isEqualTo(StatusOcorrencia.PUBLICADA);
            assertThat(ocorrencia.getAtualizadoPor()).isEqualTo(ADMIN_ID);
            assertThat(ocorrencia.getAtualizadoEm()).isEqualTo(adminTime);
        }
    }

    @Nested
    @DisplayName("permiteValidacao e equals/hashCode")
    class ValidacaoEIgualdade {

        @Test
        @DisplayName("Deve permitir validação apenas em até 30 dias após a criação e enquanto publicada")
        void deveAvaliarJanelaDe30DiasParaValidacao() {
            Ocorrencia ocorrencia = criarOcorrenciaPadrao();

            assertThat(ocorrencia.permiteValidacao(AGORA)).isTrue();
            assertThat(ocorrencia.permiteValidacao(AGORA.plusDays(30))).isTrue();
            assertThat(ocorrencia.permiteValidacao(AGORA.plusDays(30).plusSeconds(1))).isFalse();
            assertThat(ocorrencia.permiteValidacao(null)).isFalse();

            ocorrencia.excluirLogicamente(AUTOR_ID, AGORA.plusDays(1));
            assertThat(ocorrencia.permiteValidacao(AGORA.plusDays(2))).isFalse();
        }

        @Test
        @DisplayName("Deve comparar igualdade de Ocorrencia pelo id")
        void deveCompararIgualdadePeloId() {
            Ocorrencia o1 = criarOcorrenciaPadrao();
            Ocorrencia o2 = criarOcorrenciaPadrao();

            assertThat(o1).isEqualTo(o1);
            assertThat(o1).isNotEqualTo(o2);
            assertThat(o1).isNotEqualTo(null);
            assertThat(o1.hashCode()).isEqualTo(o1.getId().hashCode());
        }
    }
}
