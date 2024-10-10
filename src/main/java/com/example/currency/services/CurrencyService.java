package com.example.currency.services;

import com.example.currency.exceptions.CurrencyException;
import com.example.currency.models.CurrencyConversionResponse;
import com.example.currency.models.CurrencyRateResponse;
import com.example.currency.models.Valute;
import com.example.currency.models.ValuteCurrencies;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.io.StringReader;

@Service
public class CurrencyService {

    private final RestTemplate restTemplate;
    private final String ratesApiUrl;

    public CurrencyService(RestTemplate restTemplate, @Value("${external.api.rates.url}") String ratesApiUrl) {
        this.restTemplate = restTemplate;
        this.ratesApiUrl = ratesApiUrl;
    }

    @CircuitBreaker(name = "currencyService", fallbackMethod = "getRateFallback")
    @Cacheable(value = "ratesCache", key = "#code", cacheManager = "cacheManager")
    public CurrencyRateResponse getRate(String code) {
        try {
            String url = String.format("%s/%s", ratesApiUrl, code);

            ResponseEntity<String> responseEntity = restTemplate.getForEntity(url, String.class);

            String xmlResponse = responseEntity.getBody();

            if (xmlResponse == null || xmlResponse.isEmpty()) {
                throw new CurrencyException(HttpStatus.SERVICE_UNAVAILABLE.value(), "Currency service returned an empty response");
            }

            JAXBContext context = JAXBContext.newInstance(ValuteCurrencies.class);
            Unmarshaller unmarshaller = context.createUnmarshaller();
            ValuteCurrencies valCurs = (ValuteCurrencies) unmarshaller.unmarshal(new StringReader(xmlResponse));

            for (Valute valute : valCurs.getValutes()) {
                if (valute.getCharCode().equalsIgnoreCase(code)) {
                    double rate = Double.parseDouble(valute.getValue().replace(",", "."));
                    return new CurrencyRateResponse(valute.getCharCode(), rate);
                }
            }
            return null;
        } catch (JAXBException e) {
            throw new RuntimeException("Error parsing XML response", e);
        } catch (RuntimeException e) {
            throw new CurrencyException(HttpStatus.SERVICE_UNAVAILABLE.value(), "Currency service is unavailable");
        } catch (Exception e) {
            System.err.println("Unexpected error occurred: " + e.getMessage());
            throw new RuntimeException("Unexpected error occurred", e);
        }
    }

    @CircuitBreaker(name = "currencyService", fallbackMethod = "convertCurrencyFallback")
    @Cacheable(value = "conversionCache", key = "#fromCurrency + '_' + #toCurrency + '_' + #amount", cacheManager = "cacheManager")
    public CurrencyConversionResponse convertCurrency(String fromCurrency, String toCurrency, double amount) {
        try {
            CurrencyRateResponse fromRate = getRate(fromCurrency);
            CurrencyRateResponse toRate = getRate(toCurrency);

            double convertedAmount = amount * (toRate.getRate() / fromRate.getRate());
            return new CurrencyConversionResponse(fromCurrency, toCurrency, convertedAmount);
        } catch (CurrencyException e) {
            throw e;
        }
    }

    public CurrencyRateResponse getRateFallback(String code, Throwable throwable) {
        return new CurrencyRateResponse(code, 0);
    }

    public CurrencyConversionResponse convertCurrencyFallback(String fromCurrency, String toCurrency, double amount, Throwable throwable) {
        return new CurrencyConversionResponse(fromCurrency, toCurrency, 0);
    }
}