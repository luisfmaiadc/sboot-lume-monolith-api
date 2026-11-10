package br.com.lume.api.ocorrencia.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.lume.api.catalogo.dto.MarcadorVulnerabilidadeDto;
import br.com.lume.api.catalogo.dto.TipoViolenciaDto;
import br.com.lume.api.model.FaixaHorario;
import br.com.lume.api.model.MarcadorVulnerabilidadeResponse;
import br.com.lume.api.model.MinhaOcorrenciaResponse;
import br.com.lume.api.model.OcorrenciaDetalheResponse;
import br.com.lume.api.model.OcorrenciaResumoResponse;
import br.com.lume.api.model.PontoMapaOcorrenciaResponse;
import br.com.lume.api.model.TipoAmbiente;
import br.com.lume.api.model.TipoViolenciaResponse;
import br.com.lume.api.ocorrencia.domain.Ocorrencia;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

@DisplayName("OcorrenciaMapper - Testes Unitários")
class OcorrenciaMapperTest {

    private static final OffsetDateTime AGORA = OffsetDateTime.of(2026, 5, 10, 14, 0, 0, 0, ZoneOffset.UTC);
    private static final LocalDate DATA_OCORRENCIA = LocalDate.of(2026, 5, 9);
    private static final ZoneId ZONA_NEGOCIO = ZoneId.of("America/Sao_Paulo");
    private static final UUID USUARIO_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID ESTABELECIMENTO_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

    private OcorrenciaMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = Mappers.getMapper(OcorrenciaMapper.class);
    }

    private Ocorrencia criarOcorrencia(UUID estId, Integer marcadorId) {
        return Ocorrencia.registrar(
                estId,
                -23.550520,
                -46.633308,
                "Praça da Sé",
                "100",
                "Sé",
                "São Paulo",
                "SP",
                DATA_OCORRENCIA,
                br.com.lume.api.ocorrencia.domain.FaixaHorario.NOITE,
                1,
                marcadorId,
                estId != null ? br.com.lume.api.ocorrencia.domain.TipoAmbiente.ESTABELECIMENTO : br.com.lume.api.ocorrencia.domain.TipoAmbiente.VIA_PUBLICA,
                USUARIO_ID,
                AGORA
        );
    }

    private TipoViolenciaDto criarTipoViolenciaDto() {
        return new TipoViolenciaDto(1, "ASSEDIO_VERBAL", "Assédio Verbal", 3, 1, true);
    }

    private MarcadorVulnerabilidadeDto criarMarcadorDto() {
        return new MarcadorVulnerabilidadeDto(2, "POUCA_ILUMINACAO", "Pouca Iluminação", "Local escuro", true);
    }

    @Test
    @DisplayName("Deve mapear Ocorrência completa para OcorrenciaDetalheResponse")
    void deveMapearParaDetalheResponseCompleto() {
        Ocorrencia ocorrencia = criarOcorrencia(ESTABELECIMENTO_ID, 2);
        TipoViolenciaDto tipoViolencia = criarTipoViolenciaDto();
        MarcadorVulnerabilidadeDto marcador = criarMarcadorDto();

        OcorrenciaDetalheResponse response = mapper.toDetalheResponse(
                ocorrencia,
                tipoViolencia,
                marcador,
                "Restaurante Central",
                ZONA_NEGOCIO
        );

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(ocorrencia.getId());
        assertThat(response.getEstabelecimentoId()).isEqualTo(ESTABELECIMENTO_ID);
        assertThat(response.getEstabelecimentoNome()).isEqualTo("Restaurante Central");
        assertThat(response.getLatitude()).isEqualTo(-23.550520);
        assertThat(response.getLongitude()).isEqualTo(-46.633308);
        assertThat(response.getLogradouro()).isEqualTo("Praça da Sé");
        assertThat(response.getNumero()).isEqualTo("100");
        assertThat(response.getBairro()).isEqualTo("Sé");
        assertThat(response.getCidade()).isEqualTo("São Paulo");
        assertThat(response.getEstado()).isEqualTo("SP");
        assertThat(response.getDataOcorrencia()).isEqualTo(DATA_OCORRENCIA);
        assertThat(response.getFaixaHorario()).isEqualTo(FaixaHorario.NOITE);
        assertThat(response.getTipoAmbiente()).isEqualTo(TipoAmbiente.ESTABELECIMENTO);
        assertThat(response.getTotalValidacoes()).isZero();
        assertThat(response.getDataRegistro()).isEqualTo(LocalDate.of(2026, 5, 10));

        assertThat(response.getTipoViolencia()).isNotNull();
        assertThat(response.getTipoViolencia().getId()).isEqualTo(1);
        assertThat(response.getTipoViolencia().getCodigo()).isEqualTo("ASSEDIO_VERBAL");
        assertThat(response.getTipoViolencia().getNome()).isEqualTo("Assédio Verbal");
        assertThat(response.getTipoViolencia().getPesoGravidade()).isEqualTo(3);
        assertThat(response.getTipoViolencia().getOrdemExibicao()).isEqualTo(1);

        assertThat(response.getMarcadorVulnerabilidade()).isNotNull();
        assertThat(response.getMarcadorVulnerabilidade().getId()).isEqualTo(2);
        assertThat(response.getMarcadorVulnerabilidade().getCodigo()).isEqualTo("POUCA_ILUMINACAO");
        assertThat(response.getMarcadorVulnerabilidade().getNome()).isEqualTo("Pouca Iluminação");
        assertThat(response.getMarcadorVulnerabilidade().getDescricao()).isEqualTo("Local escuro");
    }

    @Test
    @DisplayName("Deve mapear OcorrenciaDetalheResponse com marcador e estabelecimento nulos, endereço suprimido e coordenadas/datas anonimizadas")
    void deveMapearParaDetalheResponseSemMarcadorEEstabelecimento() {
        Ocorrencia ocorrencia = criarOcorrencia(null, null);
        TipoViolenciaDto tipoViolencia = criarTipoViolenciaDto();

        OcorrenciaDetalheResponse response = mapper.toDetalheResponse(ocorrencia, tipoViolencia, null, null, ZONA_NEGOCIO);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(ocorrencia.getId());
        assertThat(response.getEstabelecimentoId()).isNull();
        assertThat(response.getEstabelecimentoNome()).isNull();
        assertThat(response.getMarcadorVulnerabilidade()).isNull();
        assertThat(response.getTipoAmbiente()).isEqualTo(TipoAmbiente.VIA_PUBLICA);
        // Anonimizadas para ~110m (3 casas)
        assertThat(response.getLatitude()).isEqualTo(-23.551);
        assertThat(response.getLongitude()).isEqualTo(-46.633);
        // Endereço específico suprimido em via pública
        assertThat(response.getLogradouro()).isNull();
        assertThat(response.getNumero()).isNull();
        assertThat(response.getBairro()).isNull();
        assertThat(response.getCidade()).isEqualTo("São Paulo");
        assertThat(response.getEstado()).isEqualTo("SP");
        // Datas atenuadas para o dia 1 do mês
        assertThat(response.getDataOcorrencia()).isEqualTo(LocalDate.of(2026, 5, 1));
        assertThat(response.getDataRegistro()).isEqualTo(LocalDate.of(2026, 5, 1));
    }

    @Test
    @DisplayName("Deve retornar null ao mapear OcorrenciaDetalheResponse com todos os argumentos nulos")
    void deveRetornarNullParaDetalheResponseNulo() {
        assertThat(mapper.toDetalheResponse(null, null, null, null, null)).isNull();
    }

    @Test
    @DisplayName("Deve mapear TipoViolenciaDto para TipoViolenciaResponse")
    void deveMapearTipoViolenciaResponse() {
        TipoViolenciaDto dto = criarTipoViolenciaDto();
        TipoViolenciaResponse response = mapper.toTipoViolenciaResponse(dto);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1);
        assertThat(response.getCodigo()).isEqualTo("ASSEDIO_VERBAL");
        assertThat(response.getNome()).isEqualTo("Assédio Verbal");
        assertThat(response.getPesoGravidade()).isEqualTo(3);
        assertThat(response.getOrdemExibicao()).isEqualTo(1);
    }

    @Test
    @DisplayName("Deve retornar null ao mapear TipoViolenciaDto nulo")
    void deveRetornarNullParaTipoViolenciaNulo() {
        assertThat(mapper.toTipoViolenciaResponse(null)).isNull();
    }

    @Test
    @DisplayName("Deve mapear MarcadorVulnerabilidadeDto para MarcadorVulnerabilidadeResponse")
    void deveMapearMarcadorResponse() {
        MarcadorVulnerabilidadeDto dto = criarMarcadorDto();
        MarcadorVulnerabilidadeResponse response = mapper.toMarcadorResponse(dto);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(2);
        assertThat(response.getCodigo()).isEqualTo("POUCA_ILUMINACAO");
        assertThat(response.getNome()).isEqualTo("Pouca Iluminação");
        assertThat(response.getDescricao()).isEqualTo("Local escuro");
    }

    @Test
    @DisplayName("Deve retornar null ao mapear MarcadorVulnerabilidadeDto nulo")
    void deveRetornarNullParaMarcadorNulo() {
        assertThat(mapper.toMarcadorResponse(null)).isNull();
    }

    @Test
    @DisplayName("Deve mapear para MinhaOcorrenciaResponse")
    void deveMapearParaMinhaOcorrenciaResponse() {
        Ocorrencia ocorrencia = criarOcorrencia(ESTABELECIMENTO_ID, null);

        MinhaOcorrenciaResponse response = mapper.toMinhaOcorrenciaResponse(
                ocorrencia,
                "Assédio Verbal",
                "Restaurante Central",
                true
        );

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(ocorrencia.getId());
        assertThat(response.getLogradouro()).isEqualTo("Praça da Sé");
        assertThat(response.getBairro()).isEqualTo("Sé");
        assertThat(response.getCidade()).isEqualTo("São Paulo");
        assertThat(response.getDataOcorrencia()).isEqualTo(DATA_OCORRENCIA);
        assertThat(response.getFaixaHorario()).isEqualTo(FaixaHorario.NOITE);
        assertThat(response.getTipoViolenciaNome()).isEqualTo("Assédio Verbal");
        assertThat(response.getEstabelecimentoNome()).isEqualTo("Restaurante Central");
        assertThat(response.getStatus()).isEqualTo("PUBLICADA");
        assertThat(response.getPermiteExclusao()).isTrue();
        assertThat(response.getTotalValidacoes()).isZero();
    }

    @Test
    @DisplayName("Deve mapear lista para MinhaOcorrenciaResponseList enriquecida")
    void deveMapearParaMinhaOcorrenciaResponseList() {
        Ocorrencia o1 = criarOcorrencia(ESTABELECIMENTO_ID, null);
        Ocorrencia o2 = criarOcorrencia(null, null);

        Map<Integer, String> nomesViolencia = Map.of(1, "Assédio Verbal");
        Map<UUID, String> nomesEstabelecimentos = Map.of(ESTABELECIMENTO_ID, "Restaurante Central");

        List<MinhaOcorrenciaResponse> list = mapper.toMinhaOcorrenciaResponseList(
                List.of(o1, o2),
                nomesViolencia,
                nomesEstabelecimentos,
                AGORA
        );

        assertThat(list).hasSize(2);
        assertThat(list.get(0).getEstabelecimentoNome()).isEqualTo("Restaurante Central");
        assertThat(list.get(0).getTipoViolenciaNome()).isEqualTo("Assédio Verbal");
        assertThat(list.get(0).getPermiteExclusao()).isTrue();

        assertThat(list.get(1).getEstabelecimentoNome()).isNull();
        assertThat(list.get(1).getTipoViolenciaNome()).isEqualTo("Assédio Verbal");
        assertThat(list.get(1).getPermiteExclusao()).isTrue();
    }

    @Test
    @DisplayName("Deve retornar null ao mapear lista nula de MinhaOcorrencia")
    void deveRetornarNullParaListaNulaMinhaOcorrencia() {
        assertThat(mapper.toMinhaOcorrenciaResponseList(null, Map.of(), Map.of(), AGORA)).isNull();
    }

    @Test
    @DisplayName("Deve mapear para OcorrenciaResumoResponse")
    void deveMapearParaOcorrenciaResumoResponse() {
        Ocorrencia ocorrencia = criarOcorrencia(ESTABELECIMENTO_ID, 2);
        TipoViolenciaDto tipoViolencia = criarTipoViolenciaDto();
        MarcadorVulnerabilidadeDto marcador = criarMarcadorDto();

        OcorrenciaResumoResponse response = mapper.toResumoResponse(ocorrencia, tipoViolencia, marcador);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(ocorrencia.getId());
        assertThat(response.getLogradouro()).isEqualTo("Praça da Sé");
        assertThat(response.getBairro()).isEqualTo("Sé");
        assertThat(response.getCidade()).isEqualTo("São Paulo");
        assertThat(response.getDataOcorrencia()).isEqualTo(DATA_OCORRENCIA);
        assertThat(response.getFaixaHorario()).isEqualTo(FaixaHorario.NOITE);
        assertThat(response.getTipoViolenciaNome()).isEqualTo("Assédio Verbal");
        assertThat(response.getPesoGravidade()).isEqualTo(3);
        assertThat(response.getMarcadorNome()).isEqualTo("Pouca Iluminação");
        assertThat(response.getTotalValidacoes()).isZero();
    }

    @Test
    @DisplayName("Deve mapear para OcorrenciaResumoResponse com defaults quando tipo e marcador são nulos")
    void deveMapearParaOcorrenciaResumoResponseComDefaults() {
        Ocorrencia ocorrencia = criarOcorrencia(null, null);

        OcorrenciaResumoResponse response = mapper.toResumoResponse(ocorrencia, null, null);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(ocorrencia.getId());
        assertThat(response.getTipoViolenciaNome()).isEqualTo("");
        assertThat(response.getPesoGravidade()).isEqualTo(1);
        assertThat(response.getMarcadorNome()).isNull();
    }

    @Test
    @DisplayName("Deve mapear lista de OcorrenciaResumoResponseList")
    void deveMapearParaResumoResponseList() {
        Ocorrencia o1 = criarOcorrencia(ESTABELECIMENTO_ID, 2);
        Map<Integer, TipoViolenciaDto> tipos = Map.of(1, criarTipoViolenciaDto());
        Map<Integer, MarcadorVulnerabilidadeDto> marcadores = Map.of(2, criarMarcadorDto());

        List<OcorrenciaResumoResponse> list = mapper.toResumoResponseList(List.of(o1), tipos, marcadores);

        assertThat(list).hasSize(1);
        assertThat(list.get(0).getTipoViolenciaNome()).isEqualTo("Assédio Verbal");
        assertThat(list.get(0).getMarcadorNome()).isEqualTo("Pouca Iluminação");
    }

    @Test
    @DisplayName("Deve retornar null ao mapear lista nula de resumo")
    void deveRetornarNullParaListaNulaResumo() {
        assertThat(mapper.toResumoResponseList(null, Map.of(), Map.of())).isNull();
    }

    @Test
    @DisplayName("Deve mapear para PontoMapaOcorrenciaResponse com coordenadas e data atenuadas quando sem estabelecimento (LGPD)")
    void deveMapearParaPontoMapaResponse() {
        Ocorrencia ocorrencia = criarOcorrencia(null, null);
        TipoViolenciaDto tipoViolencia = criarTipoViolenciaDto();

        PontoMapaOcorrenciaResponse response = mapper.toPontoMapaResponse(ocorrencia, tipoViolencia);

        assertThat(response).isNotNull();
        assertThat(response.getLatitude()).isEqualTo(-23.551);
        assertThat(response.getLongitude()).isEqualTo(-46.633);
        assertThat(response.getTipoViolenciaCodigo()).isEqualTo("ASSEDIO_VERBAL");
        assertThat(response.getTipoViolenciaNome()).isEqualTo("Assédio Verbal");
        assertThat(response.getPesoGravidade()).isEqualTo(3);
        assertThat(response.getFaixaHorario()).isEqualTo(FaixaHorario.NOITE);
        assertThat(response.getDataOcorrencia()).isEqualTo(LocalDate.of(2026, 5, 1));
        assertThat(response.getTotalValidacoes()).isZero();
    }

    @Test
    @DisplayName("Deve preservar data exata e coordenadas no PontoMapaOcorrenciaResponse quando vinculada a estabelecimento")
    void devePreservarDataECoordenadasNoMapaQuandoEstabelecimento() {
        Ocorrencia ocorrencia = criarOcorrencia(ESTABELECIMENTO_ID, null);
        TipoViolenciaDto tipoViolencia = criarTipoViolenciaDto();

        PontoMapaOcorrenciaResponse response = mapper.toPontoMapaResponse(ocorrencia, tipoViolencia);

        assertThat(response).isNotNull();
        assertThat(response.getLatitude()).isEqualTo(-23.550520);
        assertThat(response.getLongitude()).isEqualTo(-46.633308);
        assertThat(response.getDataOcorrencia()).isEqualTo(DATA_OCORRENCIA);
    }

    @Test
    @DisplayName("Deve mapear para PontoMapaOcorrenciaResponse com defaults quando tipo de violência é nulo")
    void deveMapearParaPontoMapaResponseComDefaults() {
        Ocorrencia ocorrencia = criarOcorrencia(null, null);

        PontoMapaOcorrenciaResponse response = mapper.toPontoMapaResponse(ocorrencia, null);

        assertThat(response).isNotNull();
        assertThat(response.getTipoViolenciaCodigo()).isEqualTo("OUTRO");
        assertThat(response.getTipoViolenciaNome()).isEqualTo("Outro Tipo de Violência");
        assertThat(response.getPesoGravidade()).isEqualTo(1);
    }

    @Test
    @DisplayName("Deve mapear lista de PontoMapaOcorrenciaResponseList")
    void deveMapearParaPontoMapaResponseList() {
        Ocorrencia o1 = criarOcorrencia(null, null);
        Map<Integer, TipoViolenciaDto> tipos = Map.of(1, criarTipoViolenciaDto());

        List<PontoMapaOcorrenciaResponse> list = mapper.toPontoMapaResponseList(List.of(o1), tipos);

        assertThat(list).hasSize(1);
        assertThat(list.get(0).getTipoViolenciaCodigo()).isEqualTo("ASSEDIO_VERBAL");
        assertThat(list.get(0).getTipoViolenciaNome()).isEqualTo("Assédio Verbal");
    }

    @Test
    @DisplayName("Deve retornar null ao mapear lista nula de ponto de mapa")
    void deveRetornarNullParaListaNulaPontoMapa() {
        assertThat(mapper.toPontoMapaResponseList(null, Map.of())).isNull();
    }
}
