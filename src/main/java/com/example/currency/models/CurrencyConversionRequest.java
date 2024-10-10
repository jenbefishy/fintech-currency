package com.example.currency.models;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class CurrencyConversionRequest {
    private String fromCurrency;
    private String toCurrency;
    private double amount;

}
