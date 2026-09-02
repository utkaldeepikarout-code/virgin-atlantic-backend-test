package com.virginholidays.backend.test.resource;

import com.virginholidays.backend.test.api.Flight;
import com.virginholidays.backend.test.api.ErrorResponse;
import com.virginholidays.backend.test.service.FlightInfoService;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import javax.validation.constraints.NotEmpty;
import static org.springframework.http.CacheControl.noCache;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import static org.springframework.http.ResponseEntity.status;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Geoff Perks
 *
 * The FlightInfoResource
 */
@RestController
public class FlightInfoResource {

    private final FlightInfoService flightInfoService;

    /**
     * The constructor
     *
     * @param flightInfoService the flightInfoService
     */
    public FlightInfoResource(FlightInfoService flightInfoService) {
        this.flightInfoService = flightInfoService;
    }

    /**
     * Resource method for returning flight results
     *
     * @param date the chosen date
     * @return flights for the day of the chosen date
     */
    @RequestMapping(method = RequestMethod.GET, path = "/{date}/results")
    public CompletionStage<ResponseEntity<?>> getResults(@PathVariable("date") @NotEmpty String date) {

        LocalDate outboundDate;
        try {
            outboundDate = LocalDate.parse(date);
        } catch (DateTimeParseException e) {
            return CompletableFuture.completedFuture(
                    status(HttpStatus.BAD_REQUEST)
                            .cacheControl(noCache())
                            .body(new ErrorResponse(
                                    "INVALID_DATE",
                                    "Invalid date format.",
                                    "Use ISO-8601 format YYYY-MM-DD."
                            ))
            );
        }

        return flightInfoService.findFlightByDate(outboundDate).thenApply(maybeResults -> {

            // no results, no content
            if (maybeResults.isEmpty() || maybeResults.get().isEmpty()) {
                return status(HttpStatus.NO_CONTENT).cacheControl(noCache()).build();
            }

            List<Flight> results = maybeResults.get();

            return status(HttpStatus.OK).cacheControl(noCache()).body(results);
        });
    }
}
