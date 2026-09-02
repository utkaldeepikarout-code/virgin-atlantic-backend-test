package com.virginholidays.backend.test.resource;

import com.virginholidays.backend.test.api.Flight;
import com.virginholidays.backend.test.repository.FlightInfoRepository;
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

import com.virginholidays.backend.test.service.FlightInfoServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

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

    /**
     * Spring MVC tests that validate endpoint routing and HTTP payload behavior.
     */
    @WebMvcTest(controllers = FlightInfoResource.class)
    @Import(FlightInfoServiceImpl.class)
    static
    class FlightInfoResourceWebMvcTest {

        @Autowired
        private MockMvc mockMvc;

        @MockBean
        private FlightInfoRepository flightInfoRepository;

        @Test
        void getResults_returnsSortedJson_andNoCacheHeader() throws Exception {
            Flight lateFlight = new Flight(LocalTime.of(15, 0), "Las Vegas", "LAS", "VS044", List.of(DayOfWeek.TUESDAY));
            Flight earlyFlight = new Flight(LocalTime.of(9, 0), "Antigua", "ANU", "VS033", List.of(DayOfWeek.TUESDAY));
            Flight nonMatchingDay = new Flight(LocalTime.of(10, 0), "Barbados", "BGI", "VS029", List.of(DayOfWeek.MONDAY));

            when(flightInfoRepository.findAll())
                    .thenReturn(CompletableFuture.completedFuture(Optional.of(List.of(lateFlight, earlyFlight, nonMatchingDay))));

            MvcResult result = mockMvc.perform(get("/2026-09-08/results").accept(MediaType.APPLICATION_JSON))
                    .andExpect(request().asyncStarted())
                    .andReturn();

            mockMvc.perform(asyncDispatch(result))
                    .andExpect(status().isOk())
                    .andExpect(header().string("Cache-Control", "no-cache"))
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$[0].departureTime").value("09:00"))
                    .andExpect(jsonPath("$[0].destination").value("Antigua"))
                    .andExpect(jsonPath("$[0].iata").value("ANU"))
                    .andExpect(jsonPath("$[0].flightNo").value("VS033"))
                    .andExpect(jsonPath("$[1].departureTime").value("15:00"))
                    .andExpect(jsonPath("$[1].flightNo").value("VS044"));
        }

        @Test
        void getResults_whenNoFlights_returns204AndNoCacheHeader() throws Exception {
            when(flightInfoRepository.findAll())
                    .thenReturn(CompletableFuture.completedFuture(Optional.of(List.of())));

            MvcResult result = mockMvc.perform(get("/2026-09-08/results").accept(MediaType.APPLICATION_JSON))
                    .andExpect(request().asyncStarted())
                    .andReturn();

            mockMvc.perform(asyncDispatch(result))
                    .andExpect(status().isNoContent())
                    .andExpect(header().string("Cache-Control", "no-cache"));
        }

        @Test
        void getResults_invalidDate_returnsStructured400Payload() throws Exception {
            MvcResult result = mockMvc.perform(get("/invalid-date/results").accept(MediaType.APPLICATION_JSON))
                    .andExpect(request().asyncStarted())
                    .andReturn();

            mockMvc.perform(asyncDispatch(result))
                    .andExpect(status().isBadRequest())
                    .andExpect(header().string("Cache-Control", "no-cache"))
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.code").value("INVALID_DATE"))
                    .andExpect(jsonPath("$.message").value("Invalid date format."))
                    .andExpect(jsonPath("$.details").value("Use ISO-8601 format YYYY-MM-DD."));
        }
    }
}
