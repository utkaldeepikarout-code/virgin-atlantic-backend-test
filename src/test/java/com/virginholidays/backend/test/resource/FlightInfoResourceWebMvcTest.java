package com.virginholidays.backend.test.resource;

import com.virginholidays.backend.test.api.Flight;
import com.virginholidays.backend.test.repository.FlightInfoRepository;
import com.virginholidays.backend.test.service.FlightInfoServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Spring MVC tests that validate endpoint routing and HTTP payload behavior.
 */
@WebMvcTest(controllers = FlightInfoResource.class)
@Import(FlightInfoServiceImpl.class)
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

