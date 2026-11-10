package br.com.lume.api.estabelecimento.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.lume.api.catalogo.dto.CategoriaEstabelecimentoDto;
import br.com.lume.api.estabelecimento.domain.Estabelecimento;
import br.com.lume.api.estabelecimento.dto.EstabelecimentoResumoDto;
import br.com.lume.api.model.CategoriaEstabelecimentoResponse;
import br.com.lume.api.model.DistribuicaoTurnoResponse;
import br.com.lume.api.model.EstabelecimentoAutocompleteResponse;
import br.com.lume.api.model.EstabelecimentoDetalheResponse;
import br.com.lume.api.model.EstabelecimentoResumoResponse;
import br.com.lume.api.shared.port.EstatisticasOcorrenciaEstabelecimentoDto;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

@DisplayName("EstabelecimentoMapper - Testes Unitários")
class EstabelecimentoMapperTest {

    private static final OffsetDateTime AGORA = OffsetDateTime.of(2026, 5, 10, 14, 30, 0, 0, ZoneOffset.UTC);
    private static final UUID USUARIO_ID = UUID.fromString("11111111-2222-3333-4444-555555555555");
    private static final String CNPJ_VALIDO = "11222333000181";

    private EstabelecimentoMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = Mappers.getMapper(EstabelecimentoMapper.class);
    }

    @Test
    @DisplayName("Deve mapear Estabelecimento para EstabelecimentoAutocompleteResponse corretamente")
    void deveMapearParaAutocompleteResponse() {
        Estabelecimento estabelecimento = criarEstabelecimento();

        EstabelecimentoAutocompleteResponse response = mapper.toAutocompleteResponse(estabelecimento, "Bares e Restaurantes");

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(estabelecimento.getId());
        assertThat(response.getNome()).isEqualTo("Bar do Zé");
        assertThat(response.getCategoriaNome()).isEqualTo("Bares e Restaurantes");
        assertThat(response.getLogradouro()).isEqualTo("Rua Augusta");
        assertThat(response.getNumero()).isEqualTo("1200");
        assertThat(response.getBairro()).isEqualTo("Consolação");
        assertThat(response.getCidade()).isEqualTo("São Paulo");
        assertThat(response.getScoreReputacao()).isEqualTo(5.0);
        assertThat(response.getVerificado()).isFalse();
    }

    @Test
    @DisplayName("Deve mapear lista de estabelecimentos para autocomplete list")
    void deveMapearListaParaAutocompleteResponse() {
        Estabelecimento est1 = criarEstabelecimento();
        Map<Integer, String> nomesCategorias = Map.of(1, "Bares e Restaurantes");

        List<EstabelecimentoAutocompleteResponse> list = mapper.toAutocompleteResponseList(List.of(est1), nomesCategorias);

        assertThat(list).hasSize(1);
        assertThat(list.get(0).getId()).isEqualTo(est1.getId());
        assertThat(list.get(0).getCategoriaNome()).isEqualTo("Bares e Restaurantes");

        assertThat(mapper.toAutocompleteResponseList(null, nomesCategorias)).isNull();
        assertThat(mapper.toAutocompleteResponseList(List.of(est1), null).get(0).getCategoriaNome()).isEmpty();
    }

    @Test
    @DisplayName("Deve mapear Estabelecimento para EstabelecimentoResumoResponse corretamente")
    void deveMapearParaResumoResponse() {
        Estabelecimento estabelecimento = criarEstabelecimento();

        EstabelecimentoResumoResponse response = mapper.toResumoResponse(estabelecimento, "Bares e Restaurantes");

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(estabelecimento.getId());
        assertThat(response.getNome()).isEqualTo("Bar do Zé");
        assertThat(response.getCategoriaNome()).isEqualTo("Bares e Restaurantes");
        assertThat(response.getLogradouro()).isEqualTo("Rua Augusta");
        assertThat(response.getBairro()).isEqualTo("Consolação");
        assertThat(response.getCidade()).isEqualTo("São Paulo");
        assertThat(response.getScoreReputacao()).isEqualTo(5.0);
        assertThat(response.getVerificado()).isFalse();
    }

    @Test
    @DisplayName("Deve mapear lista de estabelecimentos para resumo response list")
    void deveMapearListaParaResumoResponse() {
        Estabelecimento est1 = criarEstabelecimento();
        Map<Integer, String> nomesCategorias = Map.of(1, "Bares e Restaurantes");

        List<EstabelecimentoResumoResponse> list = mapper.toResumoResponseList(List.of(est1), nomesCategorias);

        assertThat(list).hasSize(1);
        assertThat(list.get(0).getId()).isEqualTo(est1.getId());
        assertThat(list.get(0).getCategoriaNome()).isEqualTo("Bares e Restaurantes");

        assertThat(mapper.toResumoResponseList(null, nomesCategorias)).isNull();
        assertThat(mapper.toResumoResponseList(List.of(est1), null).get(0).getCategoriaNome()).isEmpty();
    }

    @Test
    @DisplayName("Deve mapear Estabelecimento para EstabelecimentoDetalheResponse completo com estatísticas")
    void deveMapearParaDetalheResponseComEstatisticas() {
        Estabelecimento estabelecimento = criarEstabelecimento();
        CategoriaEstabelecimentoDto categoria = new CategoriaEstabelecimentoDto(1, "BAR", "Bares e Restaurantes", true);
        EstatisticasOcorrenciaEstabelecimentoDto stats = new EstatisticasOcorrenciaEstabelecimentoDto(10, 2, 3, 1, 4);

        EstabelecimentoDetalheResponse response = mapper.toDetalheResponse(estabelecimento, categoria, stats, List.of());

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(estabelecimento.getId());
        assertThat(response.getNome()).isEqualTo("Bar do Zé");
        assertThat(response.getCnpj()).isEqualTo(CNPJ_VALIDO);
        assertThat(response.getLogradouro()).isEqualTo("Rua Augusta");
        assertThat(response.getNumero()).isEqualTo("1200");
        assertThat(response.getBairro()).isEqualTo("Consolação");
        assertThat(response.getCidade()).isEqualTo("São Paulo");
        assertThat(response.getEstado()).isEqualTo("SP");
        assertThat(response.getLatitude()).isEqualTo(-23.55052);
        assertThat(response.getLongitude()).isEqualTo(-46.633308);
        assertThat(response.getScoreReputacao()).isEqualTo(5.0);
        assertThat(response.getVerificado()).isFalse();
        assertThat(response.getAtivo()).isTrue();

        assertThat(response.getCategoria()).isNotNull();
        assertThat(response.getCategoria().getId()).isEqualTo(1);
        assertThat(response.getCategoria().getCodigo()).isEqualTo("BAR");
        assertThat(response.getCategoria().getNome()).isEqualTo("Bares e Restaurantes");

        assertThat(response.getTotalOcorrenciasHistoricas()).isEqualTo(10);
        assertThat(response.getDistribuicaoTurno()).isNotNull();
        assertThat(response.getDistribuicaoTurno().getMadrugada()).isEqualTo(2);
        assertThat(response.getDistribuicaoTurno().getManha()).isEqualTo(3);
        assertThat(response.getDistribuicaoTurno().getTarde()).isEqualTo(1);
        assertThat(response.getDistribuicaoTurno().getNoite()).isEqualTo(4);
    }

    @Test
    @DisplayName("Deve mapear para DetalheResponse com estatísticas nulas inicializando totais zerados")
    void deveMapearParaDetalheResponseComStatsNulas() {
        Estabelecimento estabelecimento = criarEstabelecimento();
        CategoriaEstabelecimentoDto categoria = new CategoriaEstabelecimentoDto(1, "BAR", "Bares e Restaurantes", true);

        EstabelecimentoDetalheResponse response = mapper.toDetalheResponse(estabelecimento, categoria, null, List.of());

        assertThat(response).isNotNull();
        assertThat(response.getTotalOcorrenciasHistoricas()).isZero();
        assertThat(response.getDistribuicaoTurno()).isNotNull();
        assertThat(response.getDistribuicaoTurno().getMadrugada()).isZero();
        assertThat(response.getDistribuicaoTurno().getManha()).isZero();
        assertThat(response.getDistribuicaoTurno().getTarde()).isZero();
        assertThat(response.getDistribuicaoTurno().getNoite()).isZero();
    }

    @Test
    @DisplayName("Deve mapear CategoriaEstabelecimentoDto para CategoriaEstabelecimentoResponse")
    void deveMapearCategoriaResponse() {
        CategoriaEstabelecimentoDto dto = new CategoriaEstabelecimentoDto(2, "FARMACIA", "Farmácias", true);

        CategoriaEstabelecimentoResponse response = mapper.toCategoriaResponse(dto);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(2);
        assertThat(response.getCodigo()).isEqualTo("FARMACIA");
        assertThat(response.getNome()).isEqualTo("Farmácias");

        assertThat(mapper.toCategoriaResponse(null)).isNull();
    }

    @Test
    @DisplayName("Deve mapear EstatisticasOcorrenciaEstabelecimentoDto para DistribuicaoTurnoResponse")
    void deveMapearDistribuicaoTurnoResponse() {
        EstatisticasOcorrenciaEstabelecimentoDto stats = new EstatisticasOcorrenciaEstabelecimentoDto(5, 1, 2, 1, 1);

        DistribuicaoTurnoResponse response = mapper.toDistribuicaoTurnoResponse(stats);

        assertThat(response).isNotNull();
        assertThat(response.getMadrugada()).isEqualTo(1);
        assertThat(response.getManha()).isEqualTo(2);
        assertThat(response.getTarde()).isEqualTo(1);
        assertThat(response.getNoite()).isEqualTo(1);

        DistribuicaoTurnoResponse nullStatsResponse = mapper.toDistribuicaoTurnoResponse((EstatisticasOcorrenciaEstabelecimentoDto) null);
        assertThat(nullStatsResponse).isNotNull();
        assertThat(nullStatsResponse.getMadrugada()).isZero();
        assertThat(nullStatsResponse.getManha()).isZero();
        assertThat(nullStatsResponse.getTarde()).isZero();
        assertThat(nullStatsResponse.getNoite()).isZero();
    }

    @Test
    @DisplayName("Deve mapear Estabelecimento para EstabelecimentoResumoDto e listarResumoDto")
    void deveMapearParaResumoDto() {
        Estabelecimento estabelecimento = criarEstabelecimento();

        EstabelecimentoResumoDto dto = mapper.toResumoDto(estabelecimento, "Bares e Restaurantes");

        assertThat(dto).isNotNull();
        assertThat(dto.id()).isEqualTo(estabelecimento.getId());
        assertThat(dto.categoriaId()).isEqualTo(1);
        assertThat(dto.categoriaNome()).isEqualTo("Bares e Restaurantes");
        assertThat(dto.nome()).isEqualTo("Bar do Zé");
        assertThat(dto.logradouro()).isEqualTo("Rua Augusta");
        assertThat(dto.numero()).isEqualTo("1200");
        assertThat(dto.bairro()).isEqualTo("Consolação");
        assertThat(dto.cidade()).isEqualTo("São Paulo");
        assertThat(dto.estado()).isEqualTo("SP");
        assertThat(dto.latitude()).isEqualTo(-23.55052);
        assertThat(dto.longitude()).isEqualTo(-46.633308);
        assertThat(dto.scoreReputacao()).isEqualByComparingTo(new BigDecimal("5.00"));
        assertThat(dto.verificado()).isFalse();
        assertThat(dto.ativo()).isTrue();

        List<EstabelecimentoResumoDto> list = mapper.toResumoDtoList(List.of(estabelecimento), Map.of(1, "Bares"));
        assertThat(list).hasSize(1);
        assertThat(list.get(0).categoriaNome()).isEqualTo("Bares");

        assertThat(mapper.toResumoDtoList(null, Map.of())).isNull();
        assertThat(mapper.toResumoDtoList(List.of(estabelecimento), null).get(0).categoriaNome()).isEmpty();
    }

    @Test
    @DisplayName("Deve retornar null quando entradas forem nulas")
    void deveRetornarNullParaEntradasNulas() {
        assertThat(mapper.toAutocompleteResponse(null, null)).isNull();
        assertThat(mapper.toResumoResponse(null, null)).isNull();
        assertThat(mapper.toDetalheResponse(null, null, null, null)).isNull();
        assertThat(mapper.toResumoDto(null, null)).isNull();
        assertThat(mapper.mapScore(null)).isNull();
    }

    private Estabelecimento criarEstabelecimento() {
        return Estabelecimento.criar(
                1,
                "Bar do Zé",
                CNPJ_VALIDO,
                "Rua Augusta",
                "1200",
                "Consolação",
                "São Paulo",
                "SP",
                -23.55052,
                -46.633308,
                USUARIO_ID,
                AGORA
        );
    }
}
