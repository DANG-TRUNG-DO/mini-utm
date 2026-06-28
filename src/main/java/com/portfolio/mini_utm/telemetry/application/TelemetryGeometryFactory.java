package com.portfolio.mini_utm.telemetry.application;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.stereotype.Component;

import com.portfolio.mini_utm.telemetry.api.dto.IngestTelemetryRequest;

@Component
public class TelemetryGeometryFactory {

	private static final GeometryFactory GEOMETRY_FACTORY =
			new GeometryFactory(new PrecisionModel(), 4326);

	public Point createPosition(IngestTelemetryRequest request) {
		if (!Double.isFinite(request.longitude()) || !Double.isFinite(request.latitude())) {
			throw new InvalidTelemetryException("Telemetry coordinates must be finite numbers");
		}
		return GEOMETRY_FACTORY.createPoint(new Coordinate(
				request.longitude(),
				request.latitude(),
				request.altitudeM().doubleValue()));
	}
}
