package com.example.currency.models;

import jakarta.xml.bind.annotation.*;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@XmlRootElement(name = "ValCurs")
@XmlAccessorType(XmlAccessType.FIELD)
public class ValuteCurrencies{

    @XmlAttribute(name = "Date")
    private String date;

    @XmlAttribute(name = "name")
    private String name;

    @XmlElement(name = "Valute")
    private List<Valute> valutes;

}
