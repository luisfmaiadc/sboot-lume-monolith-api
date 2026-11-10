package br.com.lume.api.shared.util;

import br.com.lume.api.shared.exception.BusinessRuleException;

import java.math.BigDecimal;
import java.math.RoundingMode;

import lombok.experimental.UtilityClass;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;

@UtilityClass
public final class GeoUtils {

    public static final int SRID_WGS84 = 4326;
    public static final int CASAS_DECIMAIS_ANONIMIZACAO = 3;
    private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory(new PrecisionModel(), SRID_WGS84);

    public static Point criarPonto(double latitude, double longitude) {
        if (Double.isNaN(latitude) || Double.isInfinite(latitude) || latitude < -90.0 || latitude > 90.0) {
            throw new BusinessRuleException("Latitude inválida: o valor deve estar entre -90 e 90 graus.");
        }
        if (Double.isNaN(longitude) || Double.isInfinite(longitude) || longitude < -180.0 || longitude > 180.0) {
            throw new BusinessRuleException("Longitude inválida: o valor deve estar entre -180 e 180 graus.");
        }

        Point point = GEOMETRY_FACTORY.createPoint(new Coordinate(longitude, latitude));
        point.setSRID(SRID_WGS84);
        return point;
    }

    public static double extrairLatitude(Point point) {
        if (point == null) {
            throw new IllegalArgumentException("Ponto geoespacial não pode ser nulo.");
        }
        return point.getY();
    }

    public static double extrairLongitude(Point point) {
        if (point == null) {
            throw new IllegalArgumentException("Ponto geoespacial não pode ser nulo.");
        }
        return point.getX();
    }

    public static double arredondarParaAnonimizacao(double valor) {
        return BigDecimal.valueOf(valor)
                .setScale(CASAS_DECIMAIS_ANONIMIZACAO, RoundingMode.HALF_UP)
                .doubleValue();
    }
}