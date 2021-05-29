package com.example.weatherapp;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.Date;

@Data
@ToString
@NoArgsConstructor
public class Weather {
    private DayForecast currentForecast;
    private DayForecast[] dailyForecast;
}

@Data
@ToString
@NoArgsConstructor
class DayForecast {
    private Date date;
    private Date sunrise;
    private Date sunset;
    private double windSpeed;
    private int pressure;
    private double temp;
    private double feelsLike;
    private double precipitation;

    private int humidity;
    private double dewPoint;
    private double uvi;
    private int cloudinessPerc;
    private int visibility;
    private int weatherId;
    private String description;
}
