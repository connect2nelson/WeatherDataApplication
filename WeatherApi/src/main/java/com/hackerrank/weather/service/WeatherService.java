package com.hackerrank.weather.service;

import com.hackerrank.weather.exception.DuplicateWeatherDataException;
import com.hackerrank.weather.model.Weather;

import java.text.ParseException;
import java.util.Date;

public interface WeatherService   {

    void eraseAllWeatherData();

    void eraseWeatherDataForGivenDateRangeAndLocation(Date startDate, Date endDate, Float latitude, Float longitude) throws ParseException;

    Weather create(Weather weather) throws DuplicateWeatherDataException;
}
