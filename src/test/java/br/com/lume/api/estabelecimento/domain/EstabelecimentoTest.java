package br.com.lume.api.estabelecimento.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.lume.api.shared.exception.BusinessRuleException;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.util.ReflectionTestUtils;

@DisplayName("Estabelecimento - Testes Unitários de Domínio")
class EstabelecimentoTest {

    private static final OffsetDateTime AGORA = OffsetDateTime.of(2026, 5, 10, 14, 30, 0, 0, ZoneOffset.UTC);
    private static final UUID CRIADO_POR = UUID.fromString("11111111-2222-3333-4444-555555555555");
    private static final String CNPJ_VALIDO = "11222333000181";
    private static final double LATITUDE_VALIDA = -23.55052;
    private static final double LONGITUDE_VALIDA = -46.633308;

    @Nested
    @DisplayName("criar(...)")
    class Criar {

        @Test
        @DisplayName("Deve criar estabelecimento ativo com scoreReputacao=5.00, verificado=false, SRID 4326 e trim nos campos")
        void deveCriarEstabelecimentoValidoComCnpjENumero() {
            Estabelecimento est = Estabelecimento.criar(
                    1,
                    "  Livraria e Café Aurora  ",
                    "  " + CNPJ_VALIDO + "  ",
                    "  Rua das Flores  ",
                    "  450B  ",
                    "  Centro  ",
                    "  São Paulo  ",
                    "  SP  ",
                    LATITUDE_VALIDA,
                    LONGITUDE_VALIDA,
                    CRIADO_POR,
                    AGORA
            );

            assertThat(est.getId()).isNotNull();
            assertThat(est.getCategoriaId()).isEqualTo(1);
            assertThat(est.getNome()).isEqualTo("Livraria e Café Aurora");
            assertThat(est.getCnpj()).isEqualTo(CNPJ_VALIDO);
            assertThat(est.getLogradouro()).isEqualTo("Rua das Flores");
            assertThat(est.getNumero()).isEqualTo("450B");
            assertThat(est.getBairro()).isEqualTo("Centro");
            assertThat(est.getCidade()).isEqualTo("São Paulo");
            assertThat(est.getEstado()).isEqualTo("SP");
            assertThat(est.getLocalizacao()).isNotNull();
            assertThat(est.getLocalizacao().getSRID()).isEqualTo(4326);
            assertThat(est.getLatitude()).isEqualTo(LATITUDE_VALIDA);
            assertThat(est.getLongitude()).isEqualTo(LONGITUDE_VALIDA);
            assertThat(est.getScoreReputacao()).isEqualByComparingTo(new BigDecimal("5.00"));
            assertThat(est.isVerificado()).isFalse();
            assertThat(est.isAtivo()).isTrue();
            assertThat(est.getCriadoEm()).isEqualTo(AGORA);
            assertThat(est.getCriadoPor()).isEqualTo(CRIADO_POR);
            assertThat(est.getAtualizadoEm()).isNull();
            assertThat(est.getAtualizadoPor()).isNull();
        }

        @Test
        @DisplayName("Deve criar estabelecimento com CNPJ alfanumérico e normalizá-lo para uppercase")
        void deveCriarEstabelecimentoComCnpjAlfanumerico() {
            Estabelecimento est = Estabelecimento.criar(
                    1,
                    "Livraria Aurora",
                    " 12abc34501de35 ",
                    "Rua das Flores",
                    "450",
                    "Centro",
                    "São Paulo",
                    "SP",
                    LATITUDE_VALIDA,
                    LONGITUDE_VALIDA,
                    CRIADO_POR,
                    AGORA
            );

            assertThat(est.getCnpj()).isEqualTo("12ABC34501DE35");
        }

        @Test
        @DisplayName("Deve criar estabelecimento com CNPJ e número nulos quando forem nulos ou em branco")
        void deveCriarEstabelecimentoComCnpjENumeroOpcionaisNulos() {
            Estabelecimento est = Estabelecimento.criar(
                    1,
                    "Bar do Zé",
                    "   ",
                    "Rua Augusta",
                    "   ",
                    "Consolação",
                    "São Paulo",
                    "SP",
                    LATITUDE_VALIDA,
                    LONGITUDE_VALIDA,
                    CRIADO_POR,
                    AGORA
            );

            assertThat(est.getCnpj()).isNull();
            assertThat(est.getNumero()).isNull();
        }

        @Test
        @DisplayName("Deve lançar BusinessRuleException quando categoriaId for nula ou não positiva")
        void deveRejeitarCategoriaIdInvalida() {
            assertThatThrownBy(() -> criarCom(null, "Nome Válido", null, "Rua A", "10", "Bairro", "Cidade", "SP", AGORA))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessage("Categoria do estabelecimento é obrigatória e deve ser positiva.");

            assertThatThrownBy(() -> criarCom(0, "Nome Válido", null, "Rua A", "10", "Bairro", "Cidade", "SP", AGORA))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessage("Categoria do estabelecimento é obrigatória e deve ser positiva.");

            assertThatThrownBy(() -> criarCom(-1, "Nome Válido", null, "Rua A", "10", "Bairro", "Cidade", "SP", AGORA))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessage("Categoria do estabelecimento é obrigatória e deve ser positiva.");
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   ", "\t"})
        @DisplayName("Deve lançar BusinessRuleException quando nome for nulo ou em branco")
        void deveRejeitarNomeNuloOuEmBranco(String nomeInvalido) {
            assertThatThrownBy(() -> criarCom(1, nomeInvalido, null, "Rua A", "10", "Bairro", "Cidade", "SP", AGORA))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessage("Nome do estabelecimento é obrigatório.");
        }

        @Test
        @DisplayName("Deve lançar BusinessRuleException quando nome tiver menos de 3 ou mais de 150 caracteres")
        void deveRejeitarNomeForaDosLimites() {
            assertThatThrownBy(() -> criarCom(1, "Ab", null, "Rua A", "10", "Bairro", "Cidade", "SP", AGORA))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessage("Nome do estabelecimento deve ter entre 3 e 150 caracteres.");

            assertThatThrownBy(() -> criarCom(1, "N".repeat(151), null, "Rua A", "10", "Bairro", "Cidade", "SP", AGORA))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessage("Nome do estabelecimento deve ter entre 3 e 150 caracteres.");
        }

        @Test
        @DisplayName("Deve lançar BusinessRuleException quando CNPJ informado for inválido")
        void deveRejeitarCnpjInvalido() {
            assertThatThrownBy(() -> criarCom(1, "Bar do Zé", "11111111111111", "Rua A", "10", "Bairro", "Cidade", "SP", AGORA))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessage("CNPJ informado é inválido.");
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   ", "\t"})
        @DisplayName("Deve lançar BusinessRuleException quando logradouro for nulo ou em branco")
        void deveRejeitarLogradouroNuloOuEmBranco(String logradouroInvalido) {
            assertThatThrownBy(() -> criarCom(1, "Bar do Zé", null, logradouroInvalido, "10", "Bairro", "Cidade", "SP", AGORA))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessage("Logradouro do estabelecimento é obrigatório.");
        }

        @Test
        @DisplayName("Deve lançar BusinessRuleException quando logradouro tiver menos de 2 ou mais de 150 caracteres")
        void deveRejeitarLogradouroForaDosLimites() {
            assertThatThrownBy(() -> criarCom(1, "Bar do Zé", null, "R", "10", "Bairro", "Cidade", "SP", AGORA))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessage("Logradouro do estabelecimento deve ter entre 2 e 150 caracteres.");

            assertThatThrownBy(() -> criarCom(1, "Bar do Zé", null, "L".repeat(151), "10", "Bairro", "Cidade", "SP", AGORA))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessage("Logradouro do estabelecimento deve ter entre 2 e 150 caracteres.");
        }

        @Test
        @DisplayName("Deve lançar BusinessRuleException quando número exceder 20 caracteres")
        void deveRejeitarNumeroMaiorQue20Caracteres() {
            assertThatThrownBy(() -> criarCom(1, "Bar do Zé", null, "Rua A", "1".repeat(21), "Bairro", "Cidade", "SP", AGORA))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessage("Número do estabelecimento deve ter no máximo 20 caracteres.");
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   ", "\t"})
        @DisplayName("Deve lançar BusinessRuleException quando bairro for nulo ou em branco")
        void deveRejeitarBairroNuloOuEmBranco(String bairroInvalido) {
            assertThatThrownBy(() -> criarCom(1, "Bar do Zé", null, "Rua A", "10", bairroInvalido, "Cidade", "SP", AGORA))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessage("Bairro do estabelecimento é obrigatório.");
        }

        @Test
        @DisplayName("Deve lançar BusinessRuleException quando bairro exceder 100 caracteres")
        void deveRejeitarBairroMaiorQue100Caracteres() {
            assertThatThrownBy(() -> criarCom(1, "Bar do Zé", null, "Rua A", "10", "B".repeat(101), "Cidade", "SP", AGORA))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessage("Bairro do estabelecimento deve ter entre 1 e 100 caracteres.");
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   ", "\t"})
        @DisplayName("Deve lançar BusinessRuleException quando cidade for nula ou em branco")
        void deveRejeitarCidadeNulaOuEmBranco(String cidadeInvalida) {
            assertThatThrownBy(() -> criarCom(1, "Bar do Zé", null, "Rua A", "10", "Bairro", cidadeInvalida, "SP", AGORA))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessage("Cidade do estabelecimento é obrigatória.");
        }

        @Test
        @DisplayName("Deve lançar BusinessRuleException quando cidade exceder 100 caracteres")
        void deveRejeitarCidadeMaiorQue100Caracteres() {
            assertThatThrownBy(() -> criarCom(1, "Bar do Zé", null, "Rua A", "10", "Bairro", "C".repeat(101), "SP", AGORA))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessage("Cidade do estabelecimento deve ter entre 1 e 100 caracteres.");
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   ", "\t"})
        @DisplayName("Deve lançar BusinessRuleException quando estado (UF) for nulo ou em branco")
        void deveRejeitarEstadoNuloOuEmBranco(String estadoInvalido) {
            assertThatThrownBy(() -> criarCom(1, "Bar do Zé", null, "Rua A", "10", "Bairro", "Cidade", estadoInvalido, AGORA))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessage("Estado (UF) do estabelecimento é obrigatório.");
        }

        @ParameterizedTest
        @ValueSource(strings = {"sp", "S", "SAO", "S1", "12"})
        @DisplayName("Deve lançar BusinessRuleException quando estado (UF) não tiver exatamente 2 letras maiúsculas")
        void deveRejeitarEstadoForaDoPadraoUf(String estadoInvalido) {
            assertThatThrownBy(() -> criarCom(1, "Bar do Zé", null, "Rua A", "10", "Bairro", "Cidade", estadoInvalido, AGORA))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessage("Estado (UF) deve conter exatamente 2 letras maiúsculas.");
        }

        @Test
        @DisplayName("Deve lançar BusinessRuleException quando coordenadas ou agora forem inválidos")
        void deveRejeitarCoordenadasOuAgoraInvalidos() {
            assertThatThrownBy(() -> Estabelecimento.criar(
                    1, "Bar do Zé", null, "Rua A", "10", "Bairro", "Cidade", "SP", 95.0, LONGITUDE_VALIDA, CRIADO_POR, AGORA
            ))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessage("Latitude inválida: o valor deve estar entre -90 e 90 graus.");

            assertThatThrownBy(() -> criarCom(1, "Bar do Zé", null, "Rua A", "10", "Bairro", "Cidade", "SP", null))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessage("Referência temporal é obrigatória.");
        }
    }

    @Nested
    @DisplayName("atualizarDadosCadastrais, atualizarScoreReputacao e marcarComoVerificado")
    class MutacoesDominio {

        @Test
        @DisplayName("Deve atualizar dados cadastrais e manter ou alterar flag ativo")
        void deveAtualizarDadosCadastraisComSucesso() {
            Estabelecimento est = criarEstabelecimentoPadrao();
            UUID gestorId = UUID.randomUUID();
            OffsetDateTime momentoAtualizacao = AGORA.plusDays(1);

            est.atualizarDadosCadastrais(
                    "  Novo Nome  ",
                    2,
                    CNPJ_VALIDO,
                    "  Av Paulista  ",
                    "  1000  ",
                    "  Bela Vista  ",
                    "  Rio de Janeiro  ",
                    "RJ",
                    false,
                    gestorId,
                    momentoAtualizacao
            );

            assertThat(est.getNome()).isEqualTo("Novo Nome");
            assertThat(est.getCategoriaId()).isEqualTo(2);
            assertThat(est.getCnpj()).isEqualTo(CNPJ_VALIDO);
            assertThat(est.getLogradouro()).isEqualTo("Av Paulista");
            assertThat(est.getNumero()).isEqualTo("1000");
            assertThat(est.getBairro()).isEqualTo("Bela Vista");
            assertThat(est.getCidade()).isEqualTo("Rio de Janeiro");
            assertThat(est.getEstado()).isEqualTo("RJ");
            assertThat(est.isAtivo()).isFalse();
            assertThat(est.getAtualizadoPor()).isEqualTo(gestorId);
            assertThat(est.getAtualizadoEm()).isEqualTo(momentoAtualizacao);

            // Se ativo for null, mantém o estado atual
            est.atualizarDadosCadastrais(
                    "Novo Nome", 2, null, "Av Paulista", null, "Bela Vista", "Rio de Janeiro", "RJ",
                    null, gestorId, momentoAtualizacao.plusMinutes(5)
            );
            assertThat(est.isAtivo()).isFalse();
        }

        @Test
        @DisplayName("Deve atualizar scoreReputacao aplicando arredondamento HALF_UP e aceitar fronteiras 0.00 e 5.00")
        void deveAtualizarScoreReputacaoComArredondamentoHalfUp() {
            Estabelecimento est = criarEstabelecimentoPadrao();
            OffsetDateTime momento = AGORA.plusHours(1);

            est.atualizarScoreReputacao(new BigDecimal("4.355"), momento);
            assertThat(est.getScoreReputacao()).isEqualByComparingTo(new BigDecimal("4.36"));
            assertThat(est.getAtualizadoEm()).isEqualTo(momento);

            est.atualizarScoreReputacao(new BigDecimal("0"), momento);
            assertThat(est.getScoreReputacao()).isEqualByComparingTo(new BigDecimal("0.00"));

            est.atualizarScoreReputacao(new BigDecimal("5"), momento);
            assertThat(est.getScoreReputacao()).isEqualByComparingTo(new BigDecimal("5.00"));
        }

        @Test
        @DisplayName("Deve rejeitar scoreReputacao nulo, menor que 0.00 ou maior que 5.00")
        void deveRejeitarScoreReputacaoInvalido() {
            Estabelecimento est = criarEstabelecimentoPadrao();

            assertThatThrownBy(() -> est.atualizarScoreReputacao(null, AGORA))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessage("Novo score de reputação é obrigatório.");

            assertThatThrownBy(() -> est.atualizarScoreReputacao(new BigDecimal("-0.01"), AGORA))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessage("Score de reputação deve estar entre 0.00 e 5.00.");

            assertThatThrownBy(() -> est.atualizarScoreReputacao(new BigDecimal("5.01"), AGORA))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessage("Score de reputação deve estar entre 0.00 e 5.00.");
        }

        @Test
        @DisplayName("Deve marcar estabelecimento como verificado e atualizar auditoria")
        void deveMarcarComoVerificado() {
            Estabelecimento est = criarEstabelecimentoPadrao();
            UUID gestorId = UUID.randomUUID();
            OffsetDateTime momento = AGORA.plusMinutes(30);

            est.marcarComoVerificado(gestorId, momento);

            assertThat(est.isVerificado()).isTrue();
            assertThat(est.getAtualizadoPor()).isEqualTo(gestorId);
            assertThat(est.getAtualizadoEm()).isEqualTo(momento);
        }
    }

    @Nested
    @DisplayName("equals e hashCode")
    class EqualsAndHashCode {

        @Test
        @DisplayName("Deve comparar Estabelecimento por id")
        void deveCompararPorId() {
            Estabelecimento e1 = criarEstabelecimentoPadrao();
            Estabelecimento e2 = criarEstabelecimentoPadrao();
            ReflectionTestUtils.setField(e2, "id", e1.getId());
            Estabelecimento eDiferente = criarEstabelecimentoPadrao();

            assertThat(e1)
                    .isEqualTo(e1)
                    .isEqualTo(e2)
                    .hasSameHashCodeAs(e2)
                    .isNotEqualTo(eDiferente)
                    .isNotEqualTo(null)
                    .isNotEqualTo("outro");
        }
    }

    private Estabelecimento criarEstabelecimentoPadrao() {
        return criarCom(1, "Bar do Zé", CNPJ_VALIDO, "Rua Augusta", "1200", "Consolação", "São Paulo", "SP", AGORA);
    }

    private Estabelecimento criarCom(
            Integer categoriaId,
            String nome,
            String cnpj,
            String logradouro,
            String numero,
            String bairro,
            String cidade,
            String estado,
            OffsetDateTime agora
    ) {
        return Estabelecimento.criar(
                categoriaId,
                nome,
                cnpj,
                logradouro,
                numero,
                bairro,
                cidade,
                estado,
                LATITUDE_VALIDA,
                LONGITUDE_VALIDA,
                CRIADO_POR,
                agora
        );
    }
}
