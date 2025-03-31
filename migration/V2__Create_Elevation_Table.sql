-- Create extension if not exists
CREATE EXTENSION IF NOT EXISTS postgis;
CREATE EXTENSION IF NOT EXISTS postgis_raster;

-- Create table for elevation data
CREATE TABLE elevation_data (
    rid SERIAL PRIMARY KEY,
    rast raster,
    filename text
);

-- Create spatial index on the raster column
CREATE INDEX elevation_data_st_convexhull_idx
    ON elevation_data
    USING gist (ST_ConvexHull(rast));

-- Add raster constraints
SELECT AddRasterConstraints('elevation_data'::name, 'rast'::name);

-- Create a function to import DEM data
CREATE OR REPLACE FUNCTION import_dem(
    filepath text,
    filename text
) RETURNS void AS $$
BEGIN
    EXECUTE format(
        'COPY (SELECT ST_FromGDALRaster(%L)) TO PROGRAM ''raster2pgsql -s 4326 -I -C -M -d - elevation_data'' | psql',
        filepath
    );
    UPDATE elevation_data
    SET filename = filename
    WHERE filename IS NULL;
END;
$$ LANGUAGE plpgsql;