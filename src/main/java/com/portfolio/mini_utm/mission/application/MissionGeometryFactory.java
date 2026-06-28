package com.portfolio.mini_utm.mission.application;

import java.util.List;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.stereotype.Component;

import com.portfolio.mini_utm.mission.api.dto.MissionWaypointRequest;

@Component
public class MissionGeometryFactory {

	private static final GeometryFactory GEOMETRY_FACTORY =
			new GeometryFactory(new PrecisionModel(), 4326);

	public LineString createPath(List<MissionWaypointRequest> waypoints) {
		Coordinate[] coordinates = waypoints.stream()
				.map(this::coordinate)
				.toArray(Coordinate[]::new);
		for (int index = 1; index < coordinates.length; index++) {
			if (coordinates[index - 1].equals3D(coordinates[index])) {
				throw new InvalidMissionException("Consecutive waypoints must not be identical");
			}
		}
		return GEOMETRY_FACTORY.createLineString(coordinates);
	}

	public Point createPoint(MissionWaypointRequest waypoint) {
		return GEOMETRY_FACTORY.createPoint(coordinate(waypoint));
	}

	private Coordinate coordinate(MissionWaypointRequest waypoint) {
		if (!Double.isFinite(waypoint.longitude()) || !Double.isFinite(waypoint.latitude())) {
			throw new InvalidMissionException("Waypoint coordinates must be finite numbers");
		}
		return new Coordinate(
				waypoint.longitude(),
				waypoint.latitude(),
				waypoint.altitudeM().doubleValue());
	}
}
