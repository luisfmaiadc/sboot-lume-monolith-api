package br.com.lume.api.shared.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "lume.geo")
public record GeoProperties(
        @NotNull @Valid DiagnosticoGeoProperties diagnostico,
        @NotNull @Valid MapaGeoProperties mapa,
        @NotNull @Valid EstabelecimentoGeoProperties estabelecimento
) {
    public record DiagnosticoGeoProperties(
            @Positive int raioMinimoMetros,
            @Positive int raioPadraoMetros,
            @Positive int raioMaximoMetros
    ) {}

    public record MapaGeoProperties(
            @Positive int raioMinimoMetros,
            @Positive int raioPadraoMetros,
            @Positive int raioMaximoMetros,
            @Positive int limiteMaximoPontos,
            @Positive int diasMaximo
    ) {}

    public record EstabelecimentoGeoProperties(
            @Positive int raioMinimoMetros,
            @Positive int raioPadraoMetros,
            @Positive int raioMaximoMetros
    ) {}
}
