package br.com.lume.api.shared.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.lume.api.shared.exception.BusinessRuleException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.locationtech.jts.geom.Point;

@DisplayName("GeoUtils - Testes Unitários")
class GeoUtilsTest {

    @Nested
    @DisplayName("criarPonto(double, double)")
    class CriarPonto {

        @Test
        @DisplayName("Deve criar Point com SRID 4326 e coordenadas X=longitude e Y=latitude")
        void deveCriarPontoComSrid4326ECoordenadasCorretas() {
            double latitude = -23.55052;
            double longitude = -46.633308;

            Point point = GeoUtils.criarPonto(latitude, longitude);

            assertThat(point).isNotNull();
            assertThat(point.getSRID()).isEqualTo(GeoUtils.SRID_WGS84);
            assertThat(point.getX()).isEqualTo(longitude);
            assertThat(point.getY()).isEqualTo(latitude);
        }

        @ParameterizedTest
        @CsvSource({
                "-90.0, -180.0",
                "90.0, 180.0",
                "-90.0, 180.0",
                "90.0, -180.0",
                "0.0, 0.0"
        })
        @DisplayName("Deve aceitar limites extremos válidos de latitude (-90/90) e longitude (-180/180)")
        void deveAceitarLimitesExtremosValidos(double latitude, double longitude) {
            Point point = GeoUtils.criarPonto(latitude, longitude);

            assertThat(point.getSRID()).isEqualTo(4326);
            assertThat(GeoUtils.extrairLatitude(point)).isEqualTo(latitude);
            assertThat(GeoUtils.extrairLongitude(point)).isEqualTo(longitude);
        }

        @ParameterizedTest
        @ValueSource(doubles = {
                -90.0001,
                90.0001,
                -100.0,
                100.0,
                Double.NaN,
                Double.POSITIVE_INFINITY,
                Double.NEGATIVE_INFINITY
        })
        @DisplayName("Deve lançar BusinessRuleException quando latitude estiver fora de faixa, NaN ou Infinity")
        void deveLancarBusinessRuleExceptionQuandoLatitudeInvalida(double latitudeInvalida) {
            assertThatThrownBy(() -> GeoUtils.criarPonto(latitudeInvalida, -46.633308))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessage("Latitude inválida: o valor deve estar entre -90 e 90 graus.");
        }

        @ParameterizedTest
        @ValueSource(doubles = {
                -180.0001,
                180.0001,
                -200.0,
                200.0,
                Double.NaN,
                Double.POSITIVE_INFINITY,
                Double.NEGATIVE_INFINITY
        })
        @DisplayName("Deve lançar BusinessRuleException quando longitude estiver fora de faixa, NaN ou Infinity")
        void deveLancarBusinessRuleExceptionQuandoLongitudeInvalida(double longitudeInvalida) {
            assertThatThrownBy(() -> GeoUtils.criarPonto(-23.55052, longitudeInvalida))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessage("Longitude inválida: o valor deve estar entre -180 e 180 graus.");
        }
    }

    @Nested
    @DisplayName("extrairLatitude(Point) e extrairLongitude(Point)")
    class ExtrairCoordenadas {

        @Test
        @DisplayName("Deve extrair latitude e longitude corretamente de um Point válido")
        void deveExtrairLatitudeELongitudeCorretamente() {
            Point point = GeoUtils.criarPonto(-22.906847, -43.172896);

            assertThat(GeoUtils.extrairLatitude(point)).isEqualTo(-22.906847);
            assertThat(GeoUtils.extrairLongitude(point)).isEqualTo(-43.172896);
        }

        @Test
        @DisplayName("Deve lançar IllegalArgumentException ao extrair latitude de Point nulo")
        void deveLancarIllegalArgumentExceptionQuandoExtrairLatitudeDePontoNulo() {
            assertThatThrownBy(() -> GeoUtils.extrairLatitude(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Ponto geoespacial não pode ser nulo.");
        }

        @Test
        @DisplayName("Deve lançar IllegalArgumentException ao extrair longitude de Point nulo")
        void deveLancarIllegalArgumentExceptionQuandoExtrairLongitudeDePontoNulo() {
            assertThatThrownBy(() -> GeoUtils.extrairLongitude(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Ponto geoespacial não pode ser nulo.");
        }
    }
}
