# Virgin Atlantic - Flight Information Display

This repository contains a Spring Boot service that returns flights operating on the day-of-week for a requested date.

## One-step run

```bash
mvn spring-boot:run
```

The app starts with context path `/back-end-test`.

## API endpoint

`GET /back-end-test/{date}/results`

- `date` must be ISO-8601 format: `YYYY-MM-DD`
- Response is sorted by `departureTime` ascending

## Improvements implemented (and why)

1. Date parameter is now actually used in request processing.
    - Why: the original controller ignored `{date}` and always queried `LocalDate.now()`, which breaks expected behavior.
2. Flight filtering and chronological sorting implemented in `FlightInfoServiceImpl`.
    - Why: the requirement is a date-driven flight information display ordered by departure time.
3. Repository-level CSV caching added in `FlightInfoRepositoryImpl`.
    - Why: avoids parsing the CSV on every call and improves repeat-request latency.
4. Structured 400 error payload introduced.
    - Why: clients need stable, machine-readable fields instead of plain string errors.
5. Test coverage expanded, including Spring MVC HTTP-layer validation.
    - Why: validates routing, JSON shape, headers, and response status behavior end-to-end at the web layer.

## Note on build file

`pom.xml` has been kept unchanged per the original exercise guidance.

## Example requests and responses

### 200 OK - flights found

Request:

```bash
curl -i http://localhost:8080/back-end-test/2026-09-08/results
```

Response:

```http
HTTP/1.1 200 OK
Cache-Control: no-cache
Content-Type: application/json

[
  {
	"departureTime": "09:00",
	"destination": "Antigua",
	"iata": "ANU",
	"flightNo": "VS033",
	"days": ["TUESDAY"]
  },
  {
	"departureTime": "15:00",
	"destination": "Las Vegas",
	"iata": "LAS",
	"flightNo": "VS044",
	"days": ["TUESDAY"]
  }
]
```

### 204 No Content - no flights for date

This is the expected API behavior when a requested day has no matching flights in the current dataset.

Request:

```bash
curl -i http://localhost:8080/back-end-test/2030-01-01/results
```

Response:

```http
HTTP/1.1 204 No Content
Cache-Control: no-cache
```

### 400 Bad Request - invalid date format

Request:

```bash
curl -i http://localhost:8080/back-end-test/not-a-date/results
```

Response:

```http
HTTP/1.1 400 Bad Request
Cache-Control: no-cache
Content-Type: application/json

{
  "code": "INVALID_DATE",
  "message": "Invalid date format.",
  "details": "Use ISO-8601 format YYYY-MM-DD."
}
```

## Test coverage

- Unit tests for service behavior in `src/test/java/com/virginholidays/backend/test/service/FlightInfoServiceImplTest.java`
- Unit tests for controller behavior in `src/test/java/com/virginholidays/backend/test/resource/FlightInfoResourceTest.java`
- Spring MVC integration tests in `src/test/java/com/virginholidays/backend/test/resource/FlightInfoResourceWebMvcTest.java`

Run tests with:

```bash
mvn test
```
