package com.virginholidays.backend.test.service;

import com.virginholidays.backend.test.api.Flight;
import com.virginholidays.backend.test.repository.FlightInfoRepository;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * The FlightInfoServiceImpl unit tests
 *
 * @author Geoff Perks
 */
@ExtendWith(MockitoExtension.class)
class FlightInfoServiceImplTest {

    @Mock
    private FlightInfoRepository flightInfoRepository;

    @InjectMocks
    private FlightInfoServiceImpl service;

    // Monday 2026-09-07
    private static final LocalDate MONDAY = LocalDate.of(2026, 9, 7);
    // Tuesday 2026-09-08
    private static final LocalDate TUESDAY = LocalDate.of(2026, 9, 8);

    /**
     * Flights that run on a given day should be returned, filtered to that day only,
     * and sorted in chronological departure-time order.
     */
    @Test
    void testFindFlightByDate_returnsFlightsForDay_sortedChronologically() throws ExecutionException, InterruptedException {

        // Two flights that both run on MONDAY; deliberately given out of time order
        Flight afternoonFlight = new Flight(LocalTime.of(15, 0), "Las Vegas", "LAS", "VS044", List.of(DayOfWeek.MONDAY));
        Flight morningFlight   = new Flight(LocalTime.of(9, 0),  "Grenada",   "GND", "VS089", List.of(DayOfWeek.MONDAY));
        // One flight that does NOT run on MONDAY
        Flight tuesdayFlight   = new Flight(LocalTime.of(10, 0), "Antigua",   "ANU", "VS033", List.of(DayOfWeek.TUESDAY));

        when(flightInfoRepository.findAll())
                .thenReturn(CompletableFuture.completedFuture(
                        Optional.of(List.of(afternoonFlight, morningFlight, tuesdayFlight))));

        Optional<List<Flight>> result = service.findFlightByDate(MONDAY)
                .toCompletableFuture().get();

        assertThat(result.isPresent(), equalTo(true));
        assertThat(result.get(), hasSize(2));
        // chronological order: morning first, then afternoon
        assertThat(result.get(), contains(morningFlight, afternoonFlight));
    }

    /**
     * When no flights operate on the requested day the returned list should be empty (not absent).
     */
    @Test
    void testFindFlightByDate_noFlightsForDay_returnsEmptyList() throws ExecutionException, InterruptedException {

        Flight tuesdayFlight = new Flight(LocalTime.of(9, 0), "Antigua", "ANU", "VS033", List.of(DayOfWeek.TUESDAY));

        when(flightInfoRepository.findAll())
                .thenReturn(CompletableFuture.completedFuture(Optional.of(List.of(tuesdayFlight))));

        Optional<List<Flight>> result = service.findFlightByDate(MONDAY)
                .toCompletableFuture().get();

        assertThat(result.isPresent(), equalTo(true));
        assertThat(result.get(), hasSize(0));
    }

    /**
     * When the repository returns an empty Optional the service should propagate it unchanged.
     */
    @Test
    void testFindFlightByDate_repositoryReturnsEmpty_propagatesEmpty() throws ExecutionException, InterruptedException {

        when(flightInfoRepository.findAll())
                .thenReturn(CompletableFuture.completedFuture(Optional.empty()));

        Optional<List<Flight>> result = service.findFlightByDate(TUESDAY)
                .toCompletableFuture().get();

        assertThat(result.isPresent(), equalTo(false));
    }

    /**
     * A flight that runs every day of the week should appear in results for any given date.
     */
    @Test
    void testFindFlightByDate_dailyFlight_alwaysIncluded() throws ExecutionException, InterruptedException {

        Flight dailyFlight = new Flight(LocalTime.of(13, 0), "Orlando", "MCO", "VS015",
                List.of(DayOfWeek.values()));

        when(flightInfoRepository.findAll())
                .thenReturn(CompletableFuture.completedFuture(Optional.of(List.of(dailyFlight))));

        for (DayOfWeek day : DayOfWeek.values()) {
            LocalDate date = MONDAY.with(day);
            Optional<List<Flight>> result = service.findFlightByDate(date)
                    .toCompletableFuture().get();

            assertThat("Expected daily flight for " + day, result.get(), hasSize(1));
            assertThat(result.get().get(0).flightNo(), equalTo("VS015"));
        }
    }
}