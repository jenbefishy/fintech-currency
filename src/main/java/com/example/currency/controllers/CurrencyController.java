package com.example.currency.controllers;

import com.example.currency.exceptions.CurrencyException;
import com.example.currency.models.CurrencyConversionRequest;
import com.example.currency.models.CurrencyRateResponse;
import com.example.currency.models.CurrencyConversionResponse;
import com.example.currency.models.ErrorResponse;
import com.example.currency.services.CurrencyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/currencies")
public class CurrencyController {

    @Autowired
    private final CurrencyService currencyService;

    public CurrencyController(CurrencyService currencyService) {
        this.currencyService = currencyService;
    }

    @Operation(summary = "Get currency rate by code",
            description = "Fetches the current exchange rate for the specified currency code.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successful retrieval of currency rate"),
            @ApiResponse(responseCode = "400", description = "Bad request - invalid currency code"),
            @ApiResponse(responseCode = "404", description = "Currency not found in the response"),
            @ApiResponse(responseCode = "503", description = "Currency service is unavailable")
    })
    @GetMapping("/rates/{code}")
    public ResponseEntity<CurrencyRateResponse> getRate(@PathVariable String code) {
        validateCurrencyCode(code);
        CurrencyRateResponse response = currencyService.getRate(code);
        if (response == null) {
            throw new CurrencyException(HttpStatus.NOT_FOUND.value(), "Currency not found in the response");
        }
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Convert currency",
            description = "Converts the specified amount from one currency to another.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successful currency conversion"),
            @ApiResponse(responseCode = "400", description = "Bad request - invalid parameters"),
            @ApiResponse(responseCode = "404", description = "Currency not found"),
            @ApiResponse(responseCode = "503", description = "Currency service is unavailable")
    })
    @PostMapping("/convert")
    public ResponseEntity<CurrencyConversionResponse> convertCurrency(@RequestBody CurrencyConversionRequest request) {
        validateConversionRequest(request);
        CurrencyConversionResponse response = currencyService.convertCurrency(request.getFromCurrency(), request.getToCurrency(), request.getAmount());
        return ResponseEntity.ok(response);
    }

    private void validateCurrencyCode(String code) {
        if (code == null || code.isEmpty()) {
            throw new CurrencyException(HttpStatus.BAD_REQUEST.value(), "Currency code must not be null or empty");
        }
    }

    private void validateConversionRequest(CurrencyConversionRequest request) {
        if (request.getFromCurrency() == null || request.getToCurrency() == null || request.getAmount() <= 0) {
            throw new CurrencyException(HttpStatus.BAD_REQUEST.value(), "All fields must be provided and amount must be greater than 0");
        }
    }

    @ExceptionHandler(CurrencyException.class)
    public ResponseEntity<ErrorResponse> handleCustomException(CurrencyException ex) {
        if (ex.getCode() == HttpStatus.SERVICE_UNAVAILABLE.value()) {
            return ResponseEntity.status(ex.getCode())
                    .header("Retry-After", "3600")
                    .body(new ErrorResponse(ex.getCode(), ex.getMessage()));
        }
        return ResponseEntity.status(ex.getCode()).body(new ErrorResponse(ex.getCode(), ex.getMessage()));
    }

}
