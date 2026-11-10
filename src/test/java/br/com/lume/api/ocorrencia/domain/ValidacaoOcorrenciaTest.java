package br.com.lume.api.ocorrencia.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.lume.api.shared.exception.BusinessRuleException;
import br.com.lume.api.shared.exception.ConflictException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("ValidacaoOcorrencia - Testes de Domínio")
class ValidacaoOcorrenciaTest {

    private static final OffsetDateTime AGORA = OffsetDateTime.of(2026, 5, 10, 14, 0, 0, 0, ZoneOffset.UTC);
    private static final UUID AUTOR_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID VALIDADOR_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

    private Ocorrencia criarOcorrenciaPublicada() {
        return Ocorrencia.registrar(
                null,
                -23.561414,
                -46.655881,
                "Avenida Paulista",
                "1000",
                "Bela Vista",
                "São Paulo",
                "SP",
                LocalDate.of(2026, 5, 9),
                FaixaHorario.NOITE,
                1,
                null,
                TipoAmbiente.VIA_PUBLICA,
                AUTOR_ID,
                AGORA
        );
    }

    @Nested
    @DisplayName("criar")
    class Criar {

        @Test
        @DisplayName("Deve criar validação comunitária com sucesso quando validador for diferente do autor e ocorrência estiver publicada")
        void deveCriarValidacaoComSucessoQuandoDadosValidos() {
            // Arrange
            Ocorrencia ocorrencia = criarOcorrenciaPublicada();

            // Act
            ValidacaoOcorrencia validacao = ValidacaoOcorrencia.criar(
                    ocorrencia,
                    VALIDADOR_ID,
                    TipoValidacao.PRESENCIEI,
                    AGORA
            );

            // Assert
            assertThat(validacao.getId()).isNotNull();
            assertThat(validacao.getOcorrenciaId()).isEqualTo(ocorrencia.getId());
            assertThat(validacao.getUsuarioId()).isEqualTo(VALIDADOR_ID);
            assertThat(validacao.getTipoValidacao()).isEqualTo(TipoValidacao.PRESENCIEI);
            assertThat(validacao.isAtivo()).isTrue();
            assertThat(validacao.getCriadoEm()).isEqualTo(AGORA);
            assertThat(validacao.getCriadoPor()).isEqualTo(VALIDADOR_ID);
            assertThat(validacao.getAtualizadoEm()).isNull();
            assertThat(validacao.getAtualizadoPor()).isNull();
        }

        @Test
        @DisplayName("Deve inativar validação ativa com sucesso e atualizar campos de auditoria")
        void deveInativarValidacaoComSucesso() {
            // Arrange
            Ocorrencia ocorrencia = criarOcorrenciaPublicada();
            ValidacaoOcorrencia validacao = ValidacaoOcorrencia.criar(ocorrencia, VALIDADOR_ID, TipoValidacao.PRESENCIEI, AGORA);
            OffsetDateTime dataInativacao = AGORA.plusMinutes(10);

            // Act
            validacao.inativar(VALIDADOR_ID, dataInativacao);

            // Assert
            assertThat(validacao.isAtivo()).isFalse();
            assertThat(validacao.getAtualizadoPor()).isEqualTo(VALIDADOR_ID);
            assertThat(validacao.getAtualizadoEm()).isEqualTo(dataInativacao);
        }

        @Test
        @DisplayName("Deve lançar BusinessRuleException ao tentar inativar validação já inativa")
        void deveLancarExcecaoAoInativarValidacaoJaInativa() {
            // Arrange
            Ocorrencia ocorrencia = criarOcorrenciaPublicada();
            ValidacaoOcorrencia validacao = ValidacaoOcorrencia.criar(ocorrencia, VALIDADOR_ID, TipoValidacao.PRESENCIEI, AGORA);
            validacao.inativar(VALIDADOR_ID, AGORA.plusMinutes(5));

            // Act & Assert
            assertThatThrownBy(() -> validacao.inativar(VALIDADOR_ID, AGORA.plusMinutes(10)))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessage("Validação comunitária já está inativa.");
        }

        @Test
        @DisplayName("Deve lançar BusinessRuleException ao inativar com usuário ou data nula")
        void deveLancarExcecaoAoInativarComDadosInvalidos() {
            // Arrange
            Ocorrencia ocorrencia = criarOcorrenciaPublicada();
            ValidacaoOcorrencia validacao = ValidacaoOcorrencia.criar(ocorrencia, VALIDADOR_ID, TipoValidacao.PRESENCIEI, AGORA);

            // Act & Assert
            assertThatThrownBy(() -> validacao.inativar(null, AGORA))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessage("Identificador do usuário atualizador é obrigatório.");

            assertThatThrownBy(() -> validacao.inativar(VALIDADOR_ID, null))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessage("Referência temporal é obrigatória.");
        }

        @Test
        @DisplayName("Deve reativar validação com sucesso atualizando tipo e campos de auditoria")
        void deveReativarValidacaoComSucesso() {
            // Arrange
            Ocorrencia ocorrencia = criarOcorrenciaPublicada();
            ValidacaoOcorrencia validacao = ValidacaoOcorrencia.criar(ocorrencia, VALIDADOR_ID, TipoValidacao.PRESENCIEI, AGORA);
            validacao.inativar(VALIDADOR_ID, AGORA.plusMinutes(5));
            OffsetDateTime dataReativacao = AGORA.plusMinutes(15);

            // Act
            validacao.reativar(TipoValidacao.CONHECO_RISCO, VALIDADOR_ID, dataReativacao);

            // Assert
            assertThat(validacao.isAtivo()).isTrue();
            assertThat(validacao.getTipoValidacao()).isEqualTo(TipoValidacao.CONHECO_RISCO);
            assertThat(validacao.getAtualizadoPor()).isEqualTo(VALIDADOR_ID);
            assertThat(validacao.getAtualizadoEm()).isEqualTo(dataReativacao);
        }

        @Test
        @DisplayName("Deve lançar BusinessRuleException ao reativar validação informando parâmetros inválidos")
        void deveLancarExcecaoAoReativarComParametrosInvalidos() {
            // Arrange
            Ocorrencia ocorrencia = criarOcorrenciaPublicada();
            ValidacaoOcorrencia validacao = ValidacaoOcorrencia.criar(ocorrencia, VALIDADOR_ID, TipoValidacao.PRESENCIEI, AGORA);
            validacao.inativar(VALIDADOR_ID, AGORA.plusMinutes(5));

            // Act & Assert
            assertThatThrownBy(() -> validacao.reativar(null, VALIDADOR_ID, AGORA))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessage("Tipo de validação comunitária é obrigatório.");

            assertThatThrownBy(() -> validacao.reativar(TipoValidacao.CONHECO_RISCO, null, AGORA))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessage("Identificador do usuário atualizador é obrigatório.");

            assertThatThrownBy(() -> validacao.reativar(TipoValidacao.CONHECO_RISCO, VALIDADOR_ID, null))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessage("Referência temporal é obrigatória.");
        }

        @Test
        @DisplayName("Deve lançar ConflictException quando autor tentar validar a própria ocorrência")
        void deveLancarConflictExceptionQuandoAutorTentarValidarPropriaOcorrencia() {
            // Arrange
            Ocorrencia ocorrencia = criarOcorrenciaPublicada();

            // Act & Assert
            assertThatThrownBy(() -> ValidacaoOcorrencia.criar(
                    ocorrencia,
                    AUTOR_ID,
                    TipoValidacao.CONHECO_RISCO,
                    AGORA
            ))
                    .isInstanceOf(ConflictException.class)
                    .hasMessage("Operação negada: o autor não pode validar sua própria ocorrência.");
        }

        @Test
        @DisplayName("Deve lançar BusinessRuleException quando ocorrência estiver removida")
        void deveLancarBusinessRuleExceptionQuandoOcorrenciaEstiverRemovida() {
            // Arrange
            Ocorrencia ocorrencia = criarOcorrenciaPublicada();
            ocorrencia.excluirLogicamente(AUTOR_ID, AGORA.plusHours(1));

            // Act & Assert
            assertThatThrownBy(() -> ValidacaoOcorrencia.criar(
                    ocorrencia,
                    VALIDADOR_ID,
                    TipoValidacao.PRESENCIEI,
                    AGORA.plusHours(2)
            ))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessage("Não é possível validar uma ocorrência removida.");
        }

        @Test
        @DisplayName("Deve lançar BusinessRuleException quando ocorrer tentativa de validação após 30 dias da criação")
        void deveLancarBusinessRuleExceptionQuandoPrazoDe30DiasExpirado() {
            // Arrange
            Ocorrencia ocorrencia = criarOcorrenciaPublicada();
            OffsetDateTime apos30Dias = AGORA.plusDays(30).plusSeconds(1);

            // Act & Assert
            assertThatThrownBy(() -> ValidacaoOcorrencia.criar(
                    ocorrencia,
                    VALIDADOR_ID,
                    TipoValidacao.PRESENCIEI,
                    apos30Dias
            ))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessage("Ocorrências só podem receber validações comunitárias em até 30 dias após seu registro.");
        }

        @Test
        @DisplayName("Deve lançar BusinessRuleException quando parâmetros obrigatórios forem nulos")
        void deveLancarBusinessRuleExceptionQuandoParametrosObrigatoriosNulos() {
            // Arrange
            Ocorrencia ocorrencia = criarOcorrenciaPublicada();

            // Act & Assert
            assertThatThrownBy(() -> ValidacaoOcorrencia.criar(null, VALIDADOR_ID, TipoValidacao.PRESENCIEI, AGORA))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessage("Ocorrência a ser validada é obrigatória.");

            assertThatThrownBy(() -> ValidacaoOcorrencia.criar(ocorrencia, null, TipoValidacao.PRESENCIEI, AGORA))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessage("Identificador do usuário validador é obrigatório.");

            assertThatThrownBy(() -> ValidacaoOcorrencia.criar(ocorrencia, VALIDADOR_ID, null, AGORA))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessage("Tipo de validação comunitária é obrigatório.");

            assertThatThrownBy(() -> ValidacaoOcorrencia.criar(ocorrencia, VALIDADOR_ID, TipoValidacao.PRESENCIEI, null))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessage("Referência temporal é obrigatória.");
        }

        @Test
        @DisplayName("Deve comparar igualdade de ValidacaoOcorrencia pelo id")
        void deveCompararIgualdadePeloId() {
            // Arrange
            Ocorrencia ocorrencia = criarOcorrenciaPublicada();
            ValidacaoOcorrencia v1 = ValidacaoOcorrencia.criar(ocorrencia, VALIDADOR_ID, TipoValidacao.PRESENCIEI, AGORA);
            ValidacaoOcorrencia v2 = ValidacaoOcorrencia.criar(ocorrencia, VALIDADOR_ID, TipoValidacao.PRESENCIEI, AGORA);

            // Act & Assert
            assertThat(v1).isEqualTo(v1);
            assertThat(v1).isNotEqualTo(v2);
            assertThat(v1).isNotEqualTo(null);
            assertThat(v1.hashCode()).isEqualTo(v1.getId().hashCode());
        }
    }
}
