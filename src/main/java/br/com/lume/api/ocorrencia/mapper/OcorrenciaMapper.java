package br.com.lume.api.ocorrencia.mapper;

import br.com.lume.api.catalogo.dto.MarcadorVulnerabilidadeDto;
import br.com.lume.api.catalogo.dto.TipoViolenciaDto;
import br.com.lume.api.model.AtualizacaoStatusOcorrenciaRequest;
import br.com.lume.api.model.ContagemItemResponse;
import br.com.lume.api.model.DiagnosticoSegurancaResponse;
import br.com.lume.api.model.DistribuicaoTurnoResponse;
import br.com.lume.api.model.MarcadorVulnerabilidadeResponse;
import br.com.lume.api.model.MinhaOcorrenciaPageResult;
import br.com.lume.api.model.MinhaOcorrenciaResponse;
import br.com.lume.api.model.OcorrenciaDetalheResponse;
import br.com.lume.api.model.OcorrenciaResumoResponse;
import br.com.lume.api.model.PontoMapaOcorrenciaResponse;
import br.com.lume.api.model.RegistroOcorrenciaRequest;
import br.com.lume.api.model.TipoViolenciaResponse;
import br.com.lume.api.model.ValidacaoOcorrenciaRequest;
import br.com.lume.api.ocorrencia.command.AtualizarStatusOcorrenciaCommand;
import br.com.lume.api.ocorrencia.command.RegistrarOcorrenciaCommand;
import br.com.lume.api.ocorrencia.command.ValidarOcorrenciaCommand;
import br.com.lume.api.ocorrencia.domain.Ocorrencia;
import br.com.lume.api.ocorrencia.dto.DiagnosticoSegurancaDto;
import br.com.lume.api.ocorrencia.dto.MinhaOcorrenciaDto;
import br.com.lume.api.ocorrencia.dto.MinhaOcorrenciaPageDto;
import br.com.lume.api.ocorrencia.dto.OcorrenciaDetalheDto;
import br.com.lume.api.ocorrencia.dto.PontoMapaDto;
import br.com.lume.api.shared.port.ContagemItemDto;
import br.com.lume.api.shared.port.DistribuicaoTurnoDto;
import br.com.lume.api.shared.port.OcorrenciaResumoPortDto;
import br.com.lume.api.shared.util.GeoUtils;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface OcorrenciaMapper {

    br.com.lume.api.ocorrencia.domain.FaixaHorario toDomain(br.com.lume.api.model.FaixaHorario faixaHorario);
    br.com.lume.api.model.FaixaHorario toApi(br.com.lume.api.ocorrencia.domain.FaixaHorario faixaHorario);

    br.com.lume.api.ocorrencia.domain.TipoAmbiente toDomain(br.com.lume.api.model.TipoAmbiente tipoAmbiente);
    br.com.lume.api.model.TipoAmbiente toApi(br.com.lume.api.ocorrencia.domain.TipoAmbiente tipoAmbiente);

    br.com.lume.api.ocorrencia.domain.TipoValidacao toDomain(br.com.lume.api.model.TipoValidacao tipoValidacao);
    br.com.lume.api.model.TipoValidacao toApi(br.com.lume.api.ocorrencia.domain.TipoValidacao tipoValidacao);

    br.com.lume.api.ocorrencia.domain.NivelRisco toDomain(br.com.lume.api.model.NivelRisco nivelRisco);
    br.com.lume.api.model.NivelRisco toApi(br.com.lume.api.ocorrencia.domain.NivelRisco nivelRisco);

    default br.com.lume.api.ocorrencia.domain.StatusOcorrencia toDomain(AtualizacaoStatusOcorrenciaRequest.StatusEnum status) {
        if (status == null) {
            return null;
        }
        return br.com.lume.api.ocorrencia.domain.StatusOcorrencia.valueOf(status.name());
    }

    RegistrarOcorrenciaCommand toCommand(RegistroOcorrenciaRequest request);
    ValidarOcorrenciaCommand toCommand(ValidacaoOcorrenciaRequest request);

    default AtualizarStatusOcorrenciaCommand toCommand(AtualizacaoStatusOcorrenciaRequest request) {
        if (request == null) {
            return null;
        }
        return new AtualizarStatusOcorrenciaCommand(toDomain(request.getStatus()));
    }

    @Mapping(target = "id", source = "ocorrencia.id")
    @Mapping(target = "latitude", expression = "java(mapLatitude(ocorrencia))")
    @Mapping(target = "longitude", expression = "java(mapLongitude(ocorrencia))")
    @Mapping(target = "logradouro", expression = "java(mapLogradouro(ocorrencia))")
    @Mapping(target = "numero", expression = "java(mapNumero(ocorrencia))")
    @Mapping(target = "bairro", expression = "java(mapBairro(ocorrencia))")
    @Mapping(target = "dataOcorrencia", expression = "java(mapDataOcorrenciaMapa(ocorrencia))")
    @Mapping(target = "dataRegistro", expression = "java(mapDataRegistro(ocorrencia, zonaNegocio))")
    OcorrenciaDetalheResponse toDetalheResponse(
            Ocorrencia ocorrencia,
            TipoViolenciaDto tipoViolencia,
            MarcadorVulnerabilidadeDto marcadorVulnerabilidade,
            String estabelecimentoNome,
            ZoneId zonaNegocio
    );

    @Mapping(target = "id", source = "ocorrencia.id")
    @Mapping(target = "latitude", expression = "java(mapLatitude(ocorrencia))")
    @Mapping(target = "longitude", expression = "java(mapLongitude(ocorrencia))")
    @Mapping(target = "logradouro", expression = "java(mapLogradouro(ocorrencia))")
    @Mapping(target = "numero", expression = "java(mapNumero(ocorrencia))")
    @Mapping(target = "bairro", expression = "java(mapBairro(ocorrencia))")
    @Mapping(target = "dataOcorrencia", expression = "java(mapDataOcorrenciaMapa(ocorrencia))")
    @Mapping(target = "dataRegistro", expression = "java(mapDataRegistro(ocorrencia, zonaNegocio))")
    OcorrenciaDetalheDto toDetalheDto(
            Ocorrencia ocorrencia,
            TipoViolenciaDto tipoViolencia,
            MarcadorVulnerabilidadeDto marcadorVulnerabilidade,
            String estabelecimentoNome,
            ZoneId zonaNegocio
    );

    OcorrenciaDetalheResponse toDetalheResponse(OcorrenciaDetalheDto dto);

    TipoViolenciaResponse toTipoViolenciaResponse(TipoViolenciaDto dto);

    MarcadorVulnerabilidadeResponse toMarcadorResponse(MarcadorVulnerabilidadeDto dto);

    ContagemItemResponse toContagemItemResponse(ContagemItemDto dto);

    DistribuicaoTurnoResponse toDistribuicaoTurnoResponse(DistribuicaoTurnoDto dto);

    DiagnosticoSegurancaResponse toDiagnosticoResponse(DiagnosticoSegurancaDto dto);

    @Mapping(target = "status", expression = "java(ocorrencia != null && ocorrencia.getStatus() != null ? ocorrencia.getStatus().name() : null)")
    MinhaOcorrenciaResponse toMinhaOcorrenciaResponse(
            Ocorrencia ocorrencia,
            String tipoViolenciaNome,
            String estabelecimentoNome,
            boolean permiteExclusao
    );

    @Mapping(target = "status", expression = "java(ocorrencia != null && ocorrencia.getStatus() != null ? ocorrencia.getStatus().name() : null)")
    MinhaOcorrenciaDto toMinhaOcorrenciaDto(
            Ocorrencia ocorrencia,
            String tipoViolenciaNome,
            String estabelecimentoNome,
            boolean permiteExclusao
    );

    MinhaOcorrenciaResponse toMinhaOcorrenciaResponse(MinhaOcorrenciaDto dto);

    MinhaOcorrenciaPageResult toMinhaOcorrenciaPageResult(MinhaOcorrenciaPageDto pageDto);

    default List<MinhaOcorrenciaResponse> toMinhaOcorrenciaResponseList(
            List<Ocorrencia> ocorrencias,
            Map<Integer, String> nomesViolencia,
            Map<UUID, String> nomesEstabelecimentos,
            OffsetDateTime agora
    ) {
        if (ocorrencias == null) {
            return null;
        }
        List<MinhaOcorrenciaResponse> list = new ArrayList<>(ocorrencias.size());
        for (Ocorrencia o : ocorrencias) {
            String tvNome = nomesViolencia != null ? nomesViolencia.getOrDefault(o.getTipoViolenciaId(), "") : "";
            String estNome = (nomesEstabelecimentos != null && o.getEstabelecimentoId() != null)
                    ? nomesEstabelecimentos.get(o.getEstabelecimentoId()) : null;
            list.add(toMinhaOcorrenciaResponse(o, tvNome, estNome, o.permiteExclusao(agora)));
        }
        return list;
    }

    default List<MinhaOcorrenciaDto> toMinhaOcorrenciaDtoList(
            List<Ocorrencia> ocorrencias,
            Map<Integer, String> nomesViolencia,
            Map<UUID, String> nomesEstabelecimentos,
            OffsetDateTime agora
    ) {
        if (ocorrencias == null) {
            return null;
        }
        List<MinhaOcorrenciaDto> list = new ArrayList<>(ocorrencias.size());
        for (Ocorrencia o : ocorrencias) {
            String tvNome = nomesViolencia != null ? nomesViolencia.getOrDefault(o.getTipoViolenciaId(), "") : "";
            String estNome = (nomesEstabelecimentos != null && o.getEstabelecimentoId() != null)
                    ? nomesEstabelecimentos.get(o.getEstabelecimentoId()) : null;
            list.add(toMinhaOcorrenciaDto(o, tvNome, estNome, o.permiteExclusao(agora)));
        }
        return list;
    }

    @Mapping(target = "id", source = "ocorrencia.id")
    @Mapping(target = "tipoViolenciaNome", expression = "java(tipoViolencia != null && tipoViolencia.nome() != null ? tipoViolencia.nome() : \"\")")
    @Mapping(target = "pesoGravidade", expression = "java(tipoViolencia != null ? tipoViolencia.pesoGravidade() : 1)")
    @Mapping(target = "marcadorNome", expression = "java(marcador != null ? marcador.nome() : null)")
    OcorrenciaResumoResponse toResumoResponse(
            Ocorrencia ocorrencia,
            TipoViolenciaDto tipoViolencia,
            MarcadorVulnerabilidadeDto marcador
    );

    @Mapping(target = "id", source = "ocorrencia.id")
    @Mapping(target = "faixaHorario", expression = "java(ocorrencia != null && ocorrencia.getFaixaHorario() != null ? ocorrencia.getFaixaHorario().name() : null)")
    @Mapping(target = "tipoViolenciaNome", expression = "java(tipoViolencia != null && tipoViolencia.nome() != null ? tipoViolencia.nome() : \"\")")
    @Mapping(target = "pesoGravidade", expression = "java(tipoViolencia != null ? tipoViolencia.pesoGravidade() : 1)")
    @Mapping(target = "marcadorNome", expression = "java(marcador != null ? marcador.nome() : null)")
    OcorrenciaResumoPortDto toResumoPortDto(
            Ocorrencia ocorrencia,
            TipoViolenciaDto tipoViolencia,
            MarcadorVulnerabilidadeDto marcador
    );

    default List<OcorrenciaResumoResponse> toResumoResponseList(
            List<Ocorrencia> ocorrencias,
            Map<Integer, TipoViolenciaDto> tiposPorId,
            Map<Integer, MarcadorVulnerabilidadeDto> marcadoresPorId
    ) {
        if (ocorrencias == null) {
            return null;
        }
        List<OcorrenciaResumoResponse> list = new ArrayList<>(ocorrencias.size());
        for (Ocorrencia o : ocorrencias) {
            TipoViolenciaDto tv = tiposPorId != null ? tiposPorId.get(o.getTipoViolenciaId()) : null;
            MarcadorVulnerabilidadeDto m = (marcadoresPorId != null && o.getMarcadorVulnerabilidadeId() != null)
                    ? marcadoresPorId.get(o.getMarcadorVulnerabilidadeId()) : null;
            list.add(toResumoResponse(o, tv, m));
        }
        return list;
    }

    default List<OcorrenciaResumoPortDto> toResumoPortDtoList(
            List<Ocorrencia> ocorrencias,
            Map<Integer, TipoViolenciaDto> tiposPorId,
            Map<Integer, MarcadorVulnerabilidadeDto> marcadoresPorId
    ) {
        if (ocorrencias == null) {
            return null;
        }
        List<OcorrenciaResumoPortDto> list = new ArrayList<>(ocorrencias.size());
        for (Ocorrencia o : ocorrencias) {
            TipoViolenciaDto tv = tiposPorId != null ? tiposPorId.get(o.getTipoViolenciaId()) : null;
            MarcadorVulnerabilidadeDto m = (marcadoresPorId != null && o.getMarcadorVulnerabilidadeId() != null)
                    ? marcadoresPorId.get(o.getMarcadorVulnerabilidadeId()) : null;
            list.add(toResumoPortDto(o, tv, m));
        }
        return list;
    }

    @Mapping(target = "latitude", expression = "java(mapLatitude(ocorrencia))")
    @Mapping(target = "longitude", expression = "java(mapLongitude(ocorrencia))")
    @Mapping(target = "dataOcorrencia", expression = "java(mapDataOcorrenciaMapa(ocorrencia))")
    @Mapping(target = "tipoViolenciaCodigo", expression = "java(tipoViolencia != null && tipoViolencia.codigo() != null ? tipoViolencia.codigo() : \"OUTRO\")")
    @Mapping(target = "tipoViolenciaNome", expression = "java(tipoViolencia != null && tipoViolencia.nome() != null ? tipoViolencia.nome() : \"Outro Tipo de Viol\\u00EAncia\")")
    @Mapping(target = "pesoGravidade", expression = "java(tipoViolencia != null ? tipoViolencia.pesoGravidade() : 1)")
    PontoMapaOcorrenciaResponse toPontoMapaResponse(Ocorrencia ocorrencia, TipoViolenciaDto tipoViolencia);

    @Mapping(target = "latitude", expression = "java(mapLatitude(ocorrencia))")
    @Mapping(target = "longitude", expression = "java(mapLongitude(ocorrencia))")
    @Mapping(target = "dataOcorrencia", expression = "java(mapDataOcorrenciaMapa(ocorrencia))")
    @Mapping(target = "tipoViolenciaCodigo", expression = "java(tipoViolencia != null && tipoViolencia.codigo() != null ? tipoViolencia.codigo() : \"OUTRO\")")
    @Mapping(target = "tipoViolenciaNome", expression = "java(tipoViolencia != null && tipoViolencia.nome() != null ? tipoViolencia.nome() : \"Outro Tipo de Viol\\u00EAncia\")")
    @Mapping(target = "pesoGravidade", expression = "java(tipoViolencia != null ? tipoViolencia.pesoGravidade() : 1)")
    PontoMapaDto toPontoMapaDto(Ocorrencia ocorrencia, TipoViolenciaDto tipoViolencia);

    PontoMapaOcorrenciaResponse toPontoMapaResponse(PontoMapaDto dto);

    List<PontoMapaOcorrenciaResponse> toPontoMapaResponseListFromDto(List<PontoMapaDto> dtos);

    default List<PontoMapaOcorrenciaResponse> toPontoMapaResponseList(
            List<Ocorrencia> ocorrencias,
            Map<Integer, TipoViolenciaDto> tiposPorId
    ) {
        if (ocorrencias == null) {
            return null;
        }
        List<PontoMapaOcorrenciaResponse> list = new ArrayList<>(ocorrencias.size());
        for (Ocorrencia o : ocorrencias) {
            TipoViolenciaDto tv = tiposPorId != null ? tiposPorId.get(o.getTipoViolenciaId()) : null;
            list.add(toPontoMapaResponse(o, tv));
        }
        return list;
    }

    default List<PontoMapaDto> toPontoMapaDtoList(
            List<Ocorrencia> ocorrencias,
            Map<Integer, TipoViolenciaDto> tiposPorId
    ) {
        if (ocorrencias == null) {
            return null;
        }
        List<PontoMapaDto> list = new ArrayList<>(ocorrencias.size());
        for (Ocorrencia o : ocorrencias) {
            TipoViolenciaDto tv = tiposPorId != null ? tiposPorId.get(o.getTipoViolenciaId()) : null;
            list.add(toPontoMapaDto(o, tv));
        }
        return list;
    }

    default Double mapLatitude(Ocorrencia ocorrencia) {
        if (ocorrencia == null) {
            return null;
        }
        if (ocorrencia.getEstabelecimentoId() != null) {
            return ocorrencia.getLatitude();
        }
        return GeoUtils.arredondarParaAnonimizacao(ocorrencia.getLatitude());
    }

    default Double mapLongitude(Ocorrencia ocorrencia) {
        if (ocorrencia == null) {
            return null;
        }
        if (ocorrencia.getEstabelecimentoId() != null) {
            return ocorrencia.getLongitude();
        }
        return GeoUtils.arredondarParaAnonimizacao(ocorrencia.getLongitude());
    }

    default String mapLogradouro(Ocorrencia ocorrencia) {
        if (ocorrencia == null || ocorrencia.getEstabelecimentoId() == null) {
            return null;
        }
        return ocorrencia.getLogradouro();
    }

    default String mapNumero(Ocorrencia ocorrencia) {
        if (ocorrencia == null || ocorrencia.getEstabelecimentoId() == null) {
            return null;
        }
        return ocorrencia.getNumero();
    }

    default String mapBairro(Ocorrencia ocorrencia) {
        if (ocorrencia == null || ocorrencia.getEstabelecimentoId() == null) {
            return null;
        }
        return ocorrencia.getBairro();
    }

    default LocalDate mapDataOcorrenciaMapa(Ocorrencia ocorrencia) {
        if (ocorrencia == null || ocorrencia.getDataOcorrencia() == null) {
            return null;
        }
        if (ocorrencia.getEstabelecimentoId() != null) {
            return ocorrencia.getDataOcorrencia();
        }
        return ocorrencia.getDataOcorrencia().withDayOfMonth(1);
    }

    default LocalDate mapDataRegistro(Ocorrencia ocorrencia, ZoneId zonaNegocio) {
        if (ocorrencia == null || ocorrencia.getCriadoEm() == null) {
            return null;
        }
        ZoneId zone = zonaNegocio != null ? zonaNegocio : ZoneId.of("America/Sao_Paulo");
        LocalDate dataLocal = ocorrencia.getCriadoEm().atZoneSameInstant(zone).toLocalDate();
        if (ocorrencia.getEstabelecimentoId() != null) {
            return dataLocal;
        }
        return dataLocal.withDayOfMonth(1);
    }
}
