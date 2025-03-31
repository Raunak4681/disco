package com.radar.model;

import javax.persistence.*;
import org.locationtech.jts.geom.Geometry;

@Entity
@Table(name = "elevation_data")
public class ElevationData {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(columnDefinition = "raster")
    private Object rast;

    @Column(name = "filename")
    private String filename;

    @Column(name = "rast_date")
    private java.sql.Timestamp rastDate;

    @Column(columnDefinition = "geometry")
    private Geometry boundary;

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Object getRast() {
        return rast;
    }

    public void setRast(Object rast) {
        this.rast = rast;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public java.sql.Timestamp getRastDate() {
        return rastDate;
    }

    public void setRastDate(java.sql.Timestamp rastDate) {
        this.rastDate = rastDate;
    }

    public Geometry getBoundary() {
        return boundary;
    }

    public void setBoundary(Geometry boundary) {
        this.boundary = boundary;
    }
}