package com.example.currency.service;

import com.example.currency.models.CurrencyConversionResponse;
import com.example.currency.models.CurrencyRateResponse;
import com.example.currency.exceptions.CurrencyException;
import com.example.currency.services.CurrencyService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;


import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

public class CurrencyServiceTest {

    @InjectMocks
    private CurrencyService currencyService;

    @Mock
    private RestTemplate restTemplate;

    private final String ratesApiUrl = "http://www.cbr.ru/scripts/XML_daily.asp?date_req=02/03/2002";

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        currencyService = new CurrencyService(restTemplate, ratesApiUrl);
    }

    @Test
    public void testGetRate_ValidResponse() throws Exception {
        String currencyCode = "USD";
        String xmlResponse = "" +
                "<ValCurs Date=\"02.03.2002\" name=\"Foreign Currency Market\">" +
                "<Valute ID=\"R01010\">" +
                "<NumCode>036</NumCode>" +
                "<CharCode>USD</CharCode>" +
                "<Nominal>1</Nominal>" +
                "<Name>Доллар США</Name>" +
                "<Value>123,45</Value>" +
                "<VunitRate>123,45</VunitRate>" +
                "</Valute></ValCurs>";

        ResponseEntity<String> responseEntity = new ResponseEntity<>(xmlResponse, HttpStatus.OK);
        when(restTemplate.getForEntity(anyString(), eq(String.class))).thenReturn(responseEntity);

        CurrencyRateResponse rateResponse = currencyService.getRate(currencyCode);

        assertNotNull(rateResponse);
        assertEquals(currencyCode, rateResponse.getCurrency());
        assertEquals(123.45, rateResponse.getRate());
    }

    @Test
    public void testGetRate_ServiceUnavailable() {
        when(restTemplate.getForEntity(anyString(), eq(String.class)))
                .thenThrow(new RestClientException("Service Unavailable"));

        CurrencyException exception = assertThrows(CurrencyException.class, () -> currencyService.getRate("USD"));
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE.value(), exception.getCode());
        assertEquals("Currency service is unavailable", exception.getMessage());
    }



    @Test
    public void testGetRateFallback() {
        CurrencyRateResponse fallbackResponse = currencyService.getRateFallback("USD", new RuntimeException("Fallback triggered"));
        assertEquals("USD", fallbackResponse.getCurrency());
        assertEquals(0, fallbackResponse.getRate());
    }

    @Test
    public void testConvertCurrencyFallback() {
        CurrencyConversionResponse fallbackResponse = currencyService.convertCurrencyFallback("USD", "RUB", 100.0, new RuntimeException("Fallback triggered"));
        assertEquals("USD", fallbackResponse.getFromCurrency());
        assertEquals("RUB", fallbackResponse.getToCurrency());
        assertEquals(0, fallbackResponse.getConvertedAmount());
    }
}
