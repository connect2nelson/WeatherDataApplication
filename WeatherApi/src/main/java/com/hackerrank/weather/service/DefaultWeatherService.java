package com.hackerrank.weather.service;

import com.hackerrank.weather.exception.DuplicateWeatherDataException;
import com.hackerrank.weather.model.Weather;
import com.hackerrank.weather.repository.WeatherRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
public class DefaultWeatherService implements WeatherService {

    private WeatherRepository weatherRepository;

    @Autowired
    public DefaultWeatherService(WeatherRepository weatherRepository) {
        this.weatherRepository = weatherRepository;
    }

    @Override
    public void eraseAllWeatherData() {
        weatherRepository.deleteAll();
    }

    @Override
    public void eraseWeatherDataForGivenDateRangeAndLocation(Date startDate, Date endDate, Float latitude, Float longitude) {

        weatherRepository.deleteByDateRangeForGivenLocation(startDate, endDate, latitude, longitude);

    }

    @Override
    public Weather create(Weather weather) throws DuplicateWeatherDataException {

        if (weatherRepository.findOne(weather.getId()) != null)
            throw new DuplicateWeatherDataException();

        return weatherRepository.save(weather);

    }
}
