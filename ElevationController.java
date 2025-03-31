package com.radar.controller;

import com.radar.service.ElevationService;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import java.util.List;

@RestController
@RequestMapping("/api/elevation")
public class ElevationController {
    private final ElevationService elevationService;

    public ElevationController(ElevationService elevationService) {
        this.elevationService = elevationService;
    }

    @GetMapping
    public ResponseEntity<Double> getElevation(
            @RequestParam("lon") double longitude,
            @RequestParam("lat") double latitude) {
        return ResponseEntity.ok(elevationService.getElevation(longitude, latitude));
    }

    @GetMapping("/profile")
    public ResponseEntity<List<Double>> getElevationProfile(
            @RequestParam("startLon") double startLon,
            @RequestParam("startLat") double startLat,
            @RequestParam("endLon") double endLon,
            @RequestParam("endLat") double endLat,
            @RequestParam(value = "samples", defaultValue = "10") int samples) {
        return ResponseEntity.ok(elevationService.getElevationProfile(startLon, startLat, endLon, endLat, samples));
    }

    @GetMapping("/line-of-sight")
    public ResponseEntity<Boolean> checkLineOfSight(
            @RequestParam("startLon") double startLon,
            @RequestParam("startLat") double startLat,
            @RequestParam("endLon") double endLon,
            @RequestParam("endLat") double endLat,
            @RequestParam("radarHeight") double radarHeight,
            @RequestParam(value = "samples", defaultValue = "10") int samples) {
        return ResponseEntity.ok(elevationService.isLineOfSightBlocked(
                startLon, startLat, endLon, endLat, radarHeight, samples));
    }
}