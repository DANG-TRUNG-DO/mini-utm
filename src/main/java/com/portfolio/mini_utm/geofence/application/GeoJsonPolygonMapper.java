package com.portfolio.mini_utm.geofence.application;

import java.util.ArrayList;
import java.util.List;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.geom.PrecisionModel;
import org.locationtech.jts.operation.valid.IsValidOp;
import org.springframework.stereotype.Component;

import com.portfolio.mini_utm.geofence.api.dto.GeoJsonPolygon;

@Component
public class GeoJsonPolygonMapper {

	private static final int WGS84_SRID = 4326;
	private static final GeometryFactory GEOMETRY_FACTORY =
			new GeometryFactory(new PrecisionModel(), WGS84_SRID);

	public Polygon toPolygon(GeoJsonPolygon geoJson) {
		if (!"Polygon".equals(geoJson.type())) {
			throw new InvalidGeofenceException("Boundary type must be Polygon");
		}
		if (geoJson.coordinates() == null || geoJson.coordinates().isEmpty()) {
			throw new InvalidGeofenceException("Polygon must contain an exterior ring");
		}

		LinearRing shell = toLinearRing(geoJson.coordinates().get(0), "exterior");
		LinearRing[] holes = new LinearRing[Math.max(0, geoJson.coordinates().size() - 1)];
		for (int index = 1; index < geoJson.coordinates().size(); index++) {
			holes[index - 1] = toLinearRing(geoJson.coordinates().get(index), "interior");
		}

		Polygon polygon = GEOMETRY_FACTORY.createPolygon(shell, holes);
		IsValidOp validity = new IsValidOp(polygon);
		if (!validity.isValid()) {
			throw new InvalidGeofenceException("Invalid polygon: " + validity.getValidationError().getMessage());
		}
		if (polygon.isEmpty() || polygon.getArea() == 0) {
			throw new InvalidGeofenceException("Polygon area must be greater than zero");
		}
		return polygon;
	}

	public GeoJsonPolygon toGeoJson(Polygon polygon) {
		List<List<List<Double>>> rings = new ArrayList<>();
		rings.add(toCoordinates(polygon.getExteriorRing().getCoordinates()));
		for (int index = 0; index < polygon.getNumInteriorRing(); index++) {
			rings.add(toCoordinates(polygon.getInteriorRingN(index).getCoordinates()));
		}
		return new GeoJsonPolygon("Polygon", rings);
	}

	private LinearRing toLinearRing(List<List<Double>> positions, String ringType) {
		if (positions == null || positions.size() < 4) {
			throw new InvalidGeofenceException("Each %s ring must contain at least four positions".formatted(ringType));
		}

		Coordinate[] coordinates = new Coordinate[positions.size()];
		for (int index = 0; index < positions.size(); index++) {
			coordinates[index] = toCoordinate(positions.get(index));
		}
		if (!coordinates[0].equals2D(coordinates[coordinates.length - 1])) {
			throw new InvalidGeofenceException("Each polygon ring must be closed");
		}
		return GEOMETRY_FACTORY.createLinearRing(coordinates);
	}

	private Coordinate toCoordinate(List<Double> position) {
		if (position == null || position.size() != 2 || position.get(0) == null || position.get(1) == null) {
			throw new InvalidGeofenceException("Each position must contain [longitude, latitude]");
		}
		double longitude = position.get(0);
		double latitude = position.get(1);
		if (!Double.isFinite(longitude) || longitude < -180 || longitude > 180) {
			throw new InvalidGeofenceException("Longitude must be between -180 and 180");
		}
		if (!Double.isFinite(latitude) || latitude < -90 || latitude > 90) {
			throw new InvalidGeofenceException("Latitude must be between -90 and 90");
		}
		return new Coordinate(longitude, latitude);
	}

	private List<List<Double>> toCoordinates(Coordinate[] coordinates) {
		List<List<Double>> positions = new ArrayList<>(coordinates.length);
		for (Coordinate coordinate : coordinates) {
			positions.add(List.of(coordinate.getX(), coordinate.getY()));
		}
		return positions;
	}
}
