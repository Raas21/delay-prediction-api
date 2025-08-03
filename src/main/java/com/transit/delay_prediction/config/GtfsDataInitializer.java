package com.transit.delay_prediction.config;

import com.transit.delay_prediction.repository.StopTimeRepository;
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
 * Processes GTFS files for Brooklyn (gtfs_b) only if data is not already in the database.
 */
@Configuration
public class GtfsDataInitializer {
    private static final Logger logger = LoggerFactory.getLogger(GtfsDataInitializer.class);

    @Autowired
    private GtfsStaticIngestionService ingestionService;

    @Autowired
    private StopTimeRepository stopTimeRepository;

    @Bean
    public CommandLineRunner initGtfsData() {
        return args -> {
            logger.info("Checking for existing GTFS static data");
            long stopTimeCount = stopTimeRepository.count();
            if (stopTimeCount > 0) {
                logger.info("Found {} StopTime records in database, skipping GTFS static data ingestion", stopTimeCount);
                return;
            }

            logger.info("Starting GTFS static data initialization");
            String basePath = Paths.get("data", "gtfs_static").toAbsolutePath().toString();
            logger.info("Base GTFS path: {}", basePath);
            //String[] boroughs = {"gtfs_b","gtfs_busco", "gtfs_bx", "gtfs_m", "gtfs_q", "gtfs_si"}; // Original code for all boroughs
            String[] boroughs = {"gtfs_b"}; // Limited to gtfs_b
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
            logger.info("GTFS static data initialization completed");
        };
    }
}