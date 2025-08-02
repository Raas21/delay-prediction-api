package com.transit.delay_prediction.config;

import com.transit.delay_prediction.service.GtfsStaticIngestionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.File;
import java.nio.file.Paths;

/**
 * Configuration class to initialize GTFS static data ingestion on application startup.
 * Processes GTFS files for each NYC borough and logs detailed progress.
 */
@Configuration
public class GtfsDataInitializer {
    private static final Logger logger = LoggerFactory.getLogger(GtfsDataInitializer.class);

    @Autowired
    private GtfsStaticIngestionService ingestionService;

    @Bean
    public CommandLineRunner initGtfsData() {
        return args -> {
            logger.info("Starting GTFS data initialization");
            String basePath = Paths.get("data", "gtfs_static").toAbsolutePath().toString();
            logger.info("Base GTFS path: {}", basePath);
            String[] boroughs = {"gtfs_b", "gtfs_busco", "gtfs_si", "gtfs_q", "gtfs_m", "gtfs_bx"};
            for (String borough : boroughs) {
                String path = Paths.get(basePath, borough).toString();
                File gtfsDir = new File(path);
                logger.info("Checking GTFS directory: {}", path);
                if (!gtfsDir.exists() || !gtfsDir.isDirectory()) {
                    logger.warn("GTFS directory does not exist or is not a directory: {}", path);
                    continue;
                }
                File[] files = gtfsDir.listFiles();
                if (files == null || files.length == 0) {
                    logger.warn("No files found in GTFS directory: {}", path);
                    continue;
                }
                logger.info("Found {} files in GTFS directory: {}", files.length, path);
                for (File file : files) {
                    logger.info("File: {}, Size: {} bytes", file.getName(), file.length());
                }
                logger.info("Ingesting GTFS data from: {}", path);
                try {
                    ingestionService.ingestGtfsStaticData(path);
                    logger.info("Successfully ingested GTFS data for: {}", borough);
                } catch (Exception e) {
                    logger.error("Failed to ingest GTFS data for {}: {}", borough, e.getMessage(), e);
                }
            }
            logger.info("GTFS data initialization completed");
        };
    }
}