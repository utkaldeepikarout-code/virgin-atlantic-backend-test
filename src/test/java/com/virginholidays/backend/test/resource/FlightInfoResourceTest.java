package com.virginholidays.backend.test.resource;

import com.virginholidays.backend.test.api.Flight;
import com.virginholidays.backend.test.service.FlightInfoService;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * The FlightInfoResource unit tests
 *
 * @author Geoff Perks
 */
@ExtendWith(MockitoExtension.class)
class FlightInfoResourceTest {

    @Mock
    private FlightInfoService flightInfoService;

    @InjectMocks
    private FlightInfoResource resource;

    /**
     * When flights are found for the given date, the endpoint should return HTTP 200
     * with the list of flights as the body.
     */
    @Test
    void testGetResults_validDateWithFlights_returns200() throws ExecutionException, InterruptedException {

        Flight flight = new Flight(LocalTime.of(9, 0), "Antigua", "ANU", "VS033", List.of(DayOfWeek.TUESDAY));

        when(flightInfoService.findFlightByDate(any(LocalDate.class)))
                .thenReturn(CompletableFuture.completedFuture(Optional.of(List.of(flight))));

        ResponseEntity<?> response = (ResponseEntity<?>) resource.getResults("2026-09-08")
                .toCompletableFuture().get();

        assertThat(response.getStatusCode(), equalTo(HttpStatus.OK));
        assertThat(response.getBody(), instanceOf(List.class));
        @SuppressWarnings("unchecked")
        List<Flight> body = (List<Flight>) response.getBody();
        assertThat(body.size(), equalTo(1));
        assertThat(body.get(0).flightNo(), equalTo("VS033"));
    }

    /**
     * When no flights are found for the given date, the endpoint should return HTTP 204 No Content.
     */
    @Test
    void testGetResults_validDateWithNoFlights_returns204() throws ExecutionException, InterruptedException {

        when(flightInfoService.findFlightByDate(any(LocalDate.class)))
                .thenReturn(CompletableFuture.completedFuture(Optional.of(List.of())));

        ResponseEntity<?> response = (ResponseEntity<?>) resource.getResults("2026-09-08")
                .toCompletableFuture().get();

        assertThat(response.getStatusCode(), equalTo(HttpStatus.NO_CONTENT));
    }

    /**
     * When the date string cannot be parsed, the endpoint should return HTTP 400 Bad Request.
     */
    @Test
    void testGetResults_invalidDateFormat_returns400() throws ExecutionException, InterruptedException {

        ResponseEntity<?> response = (ResponseEntity<?>) resource.getResults("not-a-date")
                .toCompletableFuture().get();

        assertThat(response.getStatusCode(), equalTo(HttpStatus.BAD_REQUEST));
    }

    /**
     * When the date is provided in a non-ISO format (e.g. DD/MM/YYYY), it should
     * be rejected with HTTP 400 Bad Request.
     */
    @Test
    void testGetResults_wrongDateFormat_returns400() throws ExecutionException, InterruptedException {

        ResponseEntity<?> response = (ResponseEntity<?>) resource.getResults("08/09/2026")
                .toCompletableFuture().get();

        assertThat(response.getStatusCode(), equalTo(HttpStatus.BAD_REQUEST));
    }

    /**
     * When the service returns an empty Optional the endpoint should return HTTP 204 No Content.
     */
    @Test
    void testGetResults_serviceReturnsEmptyOptional_returns204() throws ExecutionException, InterruptedException {

        when(flightInfoService.findFlightByDate(any(LocalDate.class)))
                .thenReturn(CompletableFuture.completedFuture(Optional.empty()));

        ResponseEntity<?> response = (ResponseEntity<?>) resource.getResults("2026-09-08")
                .toCompletableFuture().get();

        assertThat(response.getStatusCode(), equalTo(HttpStatus.NO_CONTENT));
    }
}
