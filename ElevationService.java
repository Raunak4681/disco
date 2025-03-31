package com.radar.service;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;
import java.util.List;
import java.util.ArrayList;
import java.util.stream.IntStream;

@Service
public class ElevationService {
    private final GeometryFactory geometryFactory;
    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public ElevationService(JdbcTemplate jdbcTemplate) {
        this.geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);
        this.jdbcTemplate = jdbcTemplate;
    }

    public double getElevation(double longitude, double latitude) {
        String sql = """
            SELECT ST_Value(rast, ST_SetSRID(ST_MakePoint(?, ?), 4326)) as elevation
            FROM elevation_data
            WHERE ST_Intersects(rast, ST_SetSRID(ST_MakePoint(?, ?), 4326))
            LIMIT 1
        """;
        
        return jdbcTemplate.queryForObject(sql, Double.class, longitude, latitude, longitude, latitude);
    }

    public boolean hasLineOfSight(double fromLon, double fromLat, double fromElevation,
                                 double toLon, double toLat, double toElevation) {
        // Create a line between the two points
        Point fromPoint = createPoint(fromLon, fromLat);
        Point toPoint = createPoint(toLon, toLat);
        LineString line = geometryFactory.createLineString(new Coordinate[]{
            fromPoint.getCoordinate(),
            toPoint.getCoordinate()
        });

        // Query to check terrain elevation along the line
        String sql = """
            WITH points AS (
                SELECT (ST_DumpPoints(ST_Densify(ST_Transform(?, 3857), 100))).geom as point
            ),
            elevations AS (
                SELECT ST_Z(point) as elevation,
                       ST_X(ST_Transform(point, 4326)) as lon,
                       ST_Y(ST_Transform(point, 4326)) as lat
                FROM points
            )
            SELECT elevation, lon, lat
            FROM elevations
            ORDER BY ST_Distance(point, ST_StartPoint(?));
        """;

        List<TerrainPoint> terrainPoints = jdbcTemplate.query(sql,
            (rs, rowNum) -> new TerrainPoint(
                rs.getDouble("lon"),
                rs.getDouble("lat"),
                rs.getDouble("elevation")
            ),
            line, line
        );

        // Calculate line of sight considering Earth's curvature
        double distance = calculateDistance(fromLon, fromLat, toLon, toLat);
        double earthCurvature = calculateEarthCurvature(distance);

        // Check if any terrain point blocks the line of sight
        for (TerrainPoint point : terrainPoints) {
            double distanceFromStart = calculateDistance(fromLon, fromLat, point.lon, point.lat);
            double ratio = distanceFromStart / distance;
            double lineHeight = fromElevation + (toElevation - fromElevation) * ratio;
            double adjustedHeight = lineHeight - earthCurvature * ratio * (1 - ratio);

            if (point.elevation > adjustedHeight) {
                return false; // Terrain blocks line of sight
            }
        }

        return true;
    }

    private double calculateEarthCurvature(double distance) {
        final double EARTH_RADIUS = 6371000; // Earth's radius in meters
        return (distance * distance) / (2 * EARTH_RADIUS);
    }

    public Point createPoint(double longitude, double latitude) {
        return geometryFactory.createPoint(new Coordinate(longitude, latitude));
    }

    private double calculateDistance(double lon1, double lat1, double lon2, double lat2) {
        // Haversine formula for calculating great-circle distance
        final int R = 6371; // Earth's radius in kilometers

        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return R * c;
    }

    public List<Double> getElevationProfile(double startLon, double startLat, double endLon, double endLat, int samples) {
        List<Double> profile = new ArrayList<>();
        double dLon = (endLon - startLon) / (samples - 1);
        double dLat = (endLat - startLat) / (samples - 1);

        for (int i = 0; i < samples; i++) {
            double lon = startLon + (dLon * i);
            double lat = startLat + (dLat * i);
            profile.add(getElevation(lon, lat));
        }

        return profile;
    }

    public boolean isLineOfSightBlocked(double startLon, double startLat, double endLon, double endLat, double radarHeight, int samples) {
        List<Double> profile = getElevationProfile(startLon, startLat, endLon, endLat, samples);
        double distance = calculateDistance(startLon, startLat, endLon, endLat);
        double segmentLength = distance / (samples - 1);

        for (int i = 0; i < samples; i++) {
            double ratio = (double) i / (samples - 1);
            double expectedHeight = radarHeight * (1 - ratio); // Linear height decrease
            double terrainHeight = profile.get(i);

            if (terrainHeight > expectedHeight) {
                return true; // Line of sight is blocked
            }
        }

        return false; // Line of sight is clear
    }

    private static class TerrainPoint {
        final double lon;
        final double lat;
        final double elevation;

        TerrainPoint(double lon, double lat, double elevation) {
            this.lon = lon;
            this.lat = lat;
            this.elevation = elevation;
        }
    }
}