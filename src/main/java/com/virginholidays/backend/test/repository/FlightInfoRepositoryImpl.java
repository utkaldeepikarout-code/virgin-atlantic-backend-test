package com.virginholidays.backend.test.repository;

import com.virginholidays.backend.test.api.Flight;
import com.virginholidays.backend.test.configuration.DataSourceConfiguration;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.net.URL;
import java.time.DayOfWeek;
import static java.time.LocalTime.parse;
import java.util.ArrayList;
import java.util.List;
import static java.util.Objects.requireNonNull;
import java.util.Optional;
import static java.util.Optional.ofNullable;
import static java.util.concurrent.CompletableFuture.completedFuture;
import java.util.concurrent.CompletionStage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Repository;

/**
 * The implementation of FlightInfoRepository
 *
 * @author Geoff Perks
 */
@Repository
public class FlightInfoRepositoryImpl implements FlightInfoRepository {

    private final Logger LOGGER = LoggerFactory.getLogger(FlightInfoRepositoryImpl.class);

    private final ResourceLoader resourceLoader;

    private final DataSourceConfiguration dataSourceConfiguration;

    private List<Flight> cachedFlights;


    /**
     * The constructor
     *
     * @param resourceLoader          the resource loader
     * @param dataSourceConfiguration the datasource config
     */
    public FlightInfoRepositoryImpl(ResourceLoader resourceLoader, DataSourceConfiguration dataSourceConfiguration) {
        this.resourceLoader = resourceLoader;
        this.dataSourceConfiguration = dataSourceConfiguration;
    }

    @Override
    public CompletionStage<Optional<List<Flight>>> findAll() {

        if (cachedFlights != null) {
            return completedFuture(ofNullable(cachedFlights));
        }

        LOGGER.info("Loading flight information");

        // load the resource
        URL resource = requireNonNull(resourceLoader.getClassLoader()).getResource(dataSourceConfiguration.getCsvLocation());

        // create the reader
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(requireNonNull(resource).openStream()))) {

            // map the flights
            List<Flight> flights = reader
                    .lines()
                    .skip(1L)
                    .map(this::flight)
                    .toList();

            // cache for subsequent calls
            cachedFlights = flights;

            // return the results
            return completedFuture(ofNullable(flights));

        } catch (IOException e) {
            LOGGER.error("Could not retrieve flight data", e);
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Maps the line from the CSV to a flight record
     *
     * @param line the line
     * @return a flight record
     */
    private Flight flight(String line) {

        // create the tokenizer from the line in the CSV
        String[] split = line.split(",");

        // create the lists to hold the elements
        List<String> elements = new ArrayList<>();
        List<DayOfWeek> days = new ArrayList<>();

        // iterate the tokens and add the elements to the list
        int index = 0;

        for (String token : split) {

            if (token != null && !token.isBlank()) {
                switch (index) {
                    case 4 -> days.add(DayOfWeek.SUNDAY);
                    case 5 -> days.add(DayOfWeek.MONDAY);
                    case 6 -> days.add(DayOfWeek.TUESDAY);
                    case 7 -> days.add(DayOfWeek.WEDNESDAY);
                    case 8 -> days.add(DayOfWeek.THURSDAY);
                    case 9 -> days.add(DayOfWeek.FRIDAY);
                    case 10 -> days.add(DayOfWeek.SATURDAY);
                    default -> elements.add(token);
                }
            }
            index++;
        }

        // Build the properties of the flight
        return new Flight(parse(elements.get(0)), elements.get(1), elements.get(2), elements.get(3), days);
    }
}
