package ru.iguana.deal.api.controller;
import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import ru.iguana.deal.api.dto.FinishRegistrationRequestDto;
import ru.iguana.deal.api.service.CalculateCreditService;
import ru.iguana.deal.api.service.SelectOfferService;
import ru.iguana.deal.api.service.StatementService;


import java.util.List;


@RestController
@AllArgsConstructor
@Slf4j
public class DealController {

    private final StatementService statementService;
    private final SelectOfferService selectOfferService;
    private final CalculateCreditService calculateCreditService;

    @Operation(
            summary = "Get loan offers",
            description = "Accepts a loan statement request and returns a list of loan offers.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    description = "Loan statement request",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "LoanStatementRequestDto Example",
                                    value = "{\n" +
                                            "  \"amount\": \"BigDecimal\",\n" +
                                            "  \"term\": \"Integer\",\n" +
                                            "  \"firstName\": \"String\",\n" +
                                            "  \"lastName\": \"String\",\n" +
                                            "  \"middleName\": \"String\",\n" +
                                            "  \"email\": \"String\",\n" +
                                            "  \"birthdate\": \"LocalDate\",\n" +
                                            "  \"passportSeries\": \"String\",\n" +
                                            "  \"passportNumber\": \"String\"\n" +
                                            "}"
                            )
                    )
            ),
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Successful retrieval of loan offers",
                            content = @Content(
                                    mediaType = "application/json",
                                    examples = @ExampleObject(
                                            name = "LoanOfferDto Example",
                                            value = "[\n" +
                                                    "  {\n" +
                                                    "  \"statementId\": \"UUID\",\n" +
                                                    "  \"requestedAmount\": \"BigDecimal\",\n" +
                                                    "  \"totalAmount\": \"BigDecimal\",\n" +
                                                    "  \"term\": \"Integer\",\n" +
                                                    "  \"monthlyPayment\": \"BigDecimal\",\n" +
                                                    "  \"rate\": \"BigDecimal\",\n" +
                                                    "  \"isInsuranceEnabled\": \"Boolean\",\n" +
                                                    "  \"isSalaryClient\": \"Boolean\"\n" +
                                                    "}\n" +
                                                    "]"
                                    )
                            )
                    ),
                    @ApiResponse(responseCode = "400", description = "Invalid request data")
            }
    )
    @PostMapping("/deal/statement")
    public ResponseEntity<List<JsonNode>> getOffers(@RequestBody JsonNode json) {
        log.info("Received request to get loan offers");
        log.debug("Received request to get loan offers: {}", json);
        ResponseEntity<List<JsonNode>> response = statementService.getLoanOfferList(json);
        log.info("Loan offers successfully retrieved:");
        log.debug("Loan offers successfully retrieved: {}", response);
        return response;
    }


    @Operation(
            summary = "select loan offer",
            description = "Accepts a selected loan offer",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    description = "select loan offer",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "LoanOfferDto Example",
                                    value = "  {\n" +
                                            "  \"statementId\": \"UUID\",\n" +
                                            "  \"requestedAmount\": \"BigDecimal\",\n" +
                                            "  \"totalAmount\": \"BigDecimal\",\n" +
                                            "  \"term\": \"Integer\",\n" +
                                            "  \"monthlyPayment\": \"BigDecimal\",\n" +
                                            "  \"rate\": \"BigDecimal\",\n" +
                                            "  \"isInsuranceEnabled\": \"Boolean\",\n" +
                                            "  \"isSalaryClient\": \"Boolean\"\n" +
                                            "}\n"
                            )
                    )
            ),
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Successful retrieval of loan offers",
                            content = @Content(
                                    mediaType = "application/json",
                                    examples = @ExampleObject(
                                            name = "void"
                                    )
                            )
                    ),
                    @ApiResponse(responseCode = "400", description = "Invalid request data")
            }
    )
    @PostMapping("/deal/offer/select")
    public void selectOffer(@RequestBody JsonNode json) {
        log.info("Received request to select loan offer");
        log.debug("Received request to select loan offer: {}", json);
        try {
            selectOfferService.selectLoanOffer(json);
            log.info("Loan offer successfully selected.");
        } catch (Exception e) {
            log.error("Error selecting loan offer: {}", e.getMessage(), e);
            throw e;
        }
    }

    @Operation(
            summary = "Calculate credit",
            description = "Calculates credit for a given statement based on the registration data.",
            parameters = {
                    @io.swagger.v3.oas.annotations.Parameter(
                            name = "statementId",
                            description = "ID of the statement",
                            required = true,
                            example = "550e8400-e29b-41d4-a716-446655440000"
                    )
            },
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Registration data required for credit calculation",
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = FinishRegistrationRequestDto.class),
                            examples = @ExampleObject(
                                    name = "FinishRegistrationRequestDto Example",
                                    value = "{\n" +
                                            "  \"gender\": \"Enum\",\n" +
                                            "  \"maritalStatus\": \"Enum\",\n" +
                                            "  \"dependentAmount\": \"Integer\",\n" +
                                            "  \"passportIssueDate\": \"LocalDate\",\n" +
                                            "  \"passportIssueBrach\": \"String\",\n" +
                                            "  \"employment\": \"EmploymentDto\",\n" +
                                            "  \"accountNumber\": \"String\"\n" +
                                            "}"
                            )
                    )
            ),
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Credit successfully calculated"
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Invalid input data",
                            content = @Content
                    ),
                    @ApiResponse(
                            responseCode = "500",
                            description = "Internal server error",
                            content = @Content
                    )
            }
    )
    @PostMapping("/deal/calculate/{statementId}")
    public void calculate(@RequestBody FinishRegistrationRequestDto finishRegistrationRequestDto,
                          @PathVariable String statementId) {
        log.info("Received request to calculate credit for statementId");
        log.debug("Received request to calculate credit for statementId: {} with data: {}", statementId, finishRegistrationRequestDto);
        try {
            calculateCreditService.calculate(finishRegistrationRequestDto, statementId);
            log.info("Credit successfully calculated for statementId: {}", statementId);
        } catch (Exception e) {
            log.error("Error calculating credit for statementId: {}", statementId, e);
            throw e;
        }
    }
}
