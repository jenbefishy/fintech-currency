package com.example.currency.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class CurrencyRateResponse {
    @JsonProperty("currency")
    private String currency;

    @JsonProperty("rate")
    private double rate;

    public CurrencyRateResponse() {}

    public CurrencyRateResponse(String currency, double rate) {
        this.currency = currency;
        this.rate = rate;
    }

}
