package com.example.currency.exceptions;

import lombok.Getter;

@Getter
public class CurrencyException extends RuntimeException {
    private final int code;

    public CurrencyException(int code, String message) {
        super(message);
        this.code = code;
    }

}
