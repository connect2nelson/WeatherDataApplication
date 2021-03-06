package com.hackerrank.weather.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hackerrank.weather.dto.WeatherStats;
import com.hackerrank.weather.exception.DuplicateWeatherDataException;
import com.hackerrank.weather.exception.WeatherDataNotFoundException;
import com.hackerrank.weather.model.Constants;
import com.hackerrank.weather.model.Location;
import com.hackerrank.weather.model.Weather;
import com.hackerrank.weather.service.WeatherService;
import io.vavr.control.Either;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.text.SimpleDateFormat;
import java.util.*;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;


@RunWith(MockitoJUnitRunner.class)
public class WeatherApiRestControllerTest {

    private MockMvc mockMvc;

    private static final String WEATHERS_ENDPOINT = "/weather"; //"/v1/api/weathers";
    private static final String TEMPERATURES_ENDPOINT = "/temperature"; //"/v1/api/weathers";
    private static final String ERASE_ENDPOINT = "/erase";

    @InjectMocks
    private WeatherApiRestController weatherApiRestController;

    @Mock
    private WeatherService weatherService;

    private static final ObjectMapper mapper = new ObjectMapper();
    private JacksonTester<Weather> weatherDOJacksonTester;
    private JacksonTester<List<Weather>> weatherDOListJacksonTester;
    private JacksonTester<List<WeatherStats>> weatherStatsListJacksonTester;
    private SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-mm-dd");


    @Before
    public void testSetup() {

        JacksonTester.initFields(this, new ObjectMapper());

        MockitoAnnotations.initMocks(WeatherApiRestController.class);
        mockMvc = MockMvcBuilders.standaloneSetup(weatherApiRestController).dispatchOptions(true).build();

    }

    @Test
    public void shouldEraseAllWeatherData() throws Exception {

        MockHttpServletResponse mvcResponse = mockMvc.perform(
                delete(ERASE_ENDPOINT))
                .andDo(print())
                .andReturn().getResponse();

        assertThat(mvcResponse.getStatus()).isEqualTo(HttpStatus.OK.value());
        assertThat(mvcResponse.getContentAsString()).isEmpty();

        verify(weatherService, times(1)).eraseAllWeatherData();
        verifyNoMoreInteractions(weatherService);
    }

    @Test
    public void shouldEraseWeatherDataForGivenDateRangeAndLocation() throws Exception {

        String startDateInString = "2018-02-11", endDateInString = "2018-02-12";
        Date startDate = simpleDateFormat.parse(startDateInString);
        Date endDate = simpleDateFormat.parse(endDateInString);

        MockHttpServletResponse mvcResponse = mockMvc.perform(
                delete(ERASE_ENDPOINT)
                        .param("start", startDateInString)
                        .param("end", endDateInString)
                        .param("lat", "10")
                        .param("lon", "10"))
                .andDo(print())
                .andReturn().getResponse();

        assertThat(mvcResponse.getStatus()).isEqualTo(HttpStatus.OK.value());
        assertThat(mvcResponse.getContentAsString()).isEmpty();

        verify(weatherService, times(1)).eraseWeatherDataForGivenDateRangeAndLocation(startDate, endDate, 10f, 10f);
        verifyNoMoreInteractions(weatherService);
    }

    @Test
    public void shouldCreateWeatherDataResourceForValidWeatherDataObject() throws Exception {

        Weather expectedWeather = createWeatherDO();

        given(weatherService.create(isA(Weather.class))).willReturn(expectedWeather);

        String weatherDataAsString = mapper.writeValueAsString(expectedWeather);

        MockHttpServletResponse mvcResponse = mockMvc.perform(
                post(WEATHERS_ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                        .content(weatherDataAsString))
                .andDo(print())
                .andReturn().getResponse();

        assertThat(mvcResponse.getStatus()).isEqualTo(HttpStatus.CREATED.value());
        assertThat(mvcResponse.getContentAsString()).isEqualTo(
                weatherDOJacksonTester.write(expectedWeather).getJson()
        );

        verify(weatherService, times(1)).create(isA(Weather.class));
        verifyNoMoreInteractions(weatherService);

    }

    @Test
    public void shouldNotCreateWeatherDataResourceForDuplicateWeatherDataObjectAndReturn400StatusCode() throws Exception {

        Weather expectedWeather = createWeatherDO();

        given(weatherService.create(isA(Weather.class))).willThrow(DuplicateWeatherDataException.class);

        String weatherDataAsString = mapper.writeValueAsString(expectedWeather);

        MockHttpServletResponse createWeatherDataResponse = mockMvc.perform(
                post(WEATHERS_ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                        .content(weatherDataAsString))
                .andDo(print())
                .andReturn().getResponse();

        assertThat(createWeatherDataResponse.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(createWeatherDataResponse.getContentAsString()).isEmpty();

        verify(weatherService, times(1)).create(isA(Weather.class));
        verifyNoMoreInteractions(weatherService);

    }


    @Test
    public void shouldGetAllWeatherDataWhenDataIsPresent() throws Exception {

        Weather expectedWeather = createWeatherDO();

        List<Weather> expectedWeatherDataList = Collections.singletonList(expectedWeather);
        given(weatherService.getAllWeatherData()).willReturn(expectedWeatherDataList);

        MockHttpServletResponse getAllWeatherDataResponse = mockMvc.perform(
                get(WEATHERS_ENDPOINT))
                .andDo(print())
                .andReturn().getResponse();

        assertThat(getAllWeatherDataResponse.getStatus()).isEqualTo(HttpStatus.OK.value());
        assertThat(getAllWeatherDataResponse.getContentAsString()).isNotEmpty();
        assertThat(getAllWeatherDataResponse.getContentAsString()).isEqualTo(
                weatherDOListJacksonTester.write(expectedWeatherDataList).getJson()
        );

        verify(weatherService, times(1)).getAllWeatherData();
        verifyNoMoreInteractions(weatherService);

    }


    @Test
    public void shouldGetAllWeatherDataForGivenLatAndLongitude() throws Exception {

        Weather expectedWeather = createWeatherDO();
        Float latitude = expectedWeather.getLocation().getLatitude();
        Float longitude = expectedWeather.getLocation().getLongitude();

        List<Weather> expectedWeatherDataList = Collections.singletonList(expectedWeather);
        given(weatherService.getAllWeatherDataForGivenLatitudeAndLongitude(latitude, longitude))
                .willReturn(expectedWeatherDataList);

        MockHttpServletResponse getFilterWeatherDataResponse = mockMvc.perform(
                get(WEATHERS_ENDPOINT)
                        .param("lat", "10")
                        .param("lon", "10"))
                .andDo(print())
                .andReturn().getResponse();

        assertThat(getFilterWeatherDataResponse.getStatus()).isEqualTo(HttpStatus.OK.value());
        assertThat(getFilterWeatherDataResponse.getContentAsString()).isNotEmpty();
        assertThat(getFilterWeatherDataResponse.getContentAsString()).isEqualTo(
                weatherDOListJacksonTester.write(expectedWeatherDataList).getJson()
        );

        verify(weatherService, times(1))
                .getAllWeatherDataForGivenLatitudeAndLongitude(latitude, longitude);
        verifyNoMoreInteractions(weatherService);
    }

    @Test
    public void shouldReturn404IfThereAreNoWeatherDataForGivenLatAndLongitude() throws Exception {

        Weather expectedWeather = createWeatherDO();
        Float latitude = expectedWeather.getLocation().getLatitude();
        Float longitude = expectedWeather.getLocation().getLongitude();

        given(weatherService.getAllWeatherDataForGivenLatitudeAndLongitude(latitude, longitude))
                .willThrow(WeatherDataNotFoundException.class);

        MockHttpServletResponse getFilterWeatherDataResponse = mockMvc.perform(
                get(WEATHERS_ENDPOINT)
                        .param("lat", "10")
                        .param("lon", "10"))
                .andDo(print())
                .andReturn().getResponse();

        assertThat(getFilterWeatherDataResponse.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        assertThat(getFilterWeatherDataResponse.getContentAsString()).isEmpty();

        verify(weatherService, times(1))
                .getAllWeatherDataForGivenLatitudeAndLongitude(latitude, longitude);
        verifyNoMoreInteractions(weatherService);
    }

    @Test
    public void shouldGetAllWeatherDataForGivenDateRange() throws Exception {

        Weather expectedWeather = createWeatherDO();

        String startDateInString = "2018-02-11", endDateInString = "2018-02-12";
        Date startDate = simpleDateFormat.parse(startDateInString);
        Date endDate = simpleDateFormat.parse(endDateInString);

        List<WeatherStats> weatherStatsList = new ArrayList<>();

        DoubleSummaryStatistics temperatureSummaryStatistics = new DoubleSummaryStatistics();
        temperatureSummaryStatistics.accept(11f);
        temperatureSummaryStatistics.accept(12f);

        WeatherStats e = new WeatherStats(expectedWeather.getLocation(), Either.left(temperatureSummaryStatistics));
        weatherStatsList.add(e);


        given(weatherService.getAllWeatherDataForGivenDateRange(startDate, endDate))
                .willReturn( weatherStatsList);

        MockHttpServletResponse getFilterWeatherDataResponse = mockMvc.perform(
                get(WEATHERS_ENDPOINT + TEMPERATURES_ENDPOINT)
                        .param("start", startDateInString)
                        .param("end", endDateInString))
                .andDo(print())
                .andReturn().getResponse();

        assertThat(getFilterWeatherDataResponse.getStatus()).isEqualTo(HttpStatus.OK.value());
        assertThat(getFilterWeatherDataResponse.getContentAsString()).isNotEmpty();
        assertThat(getFilterWeatherDataResponse.getContentAsString()).isEqualTo(
                weatherStatsListJacksonTester.write(weatherStatsList).getJson()
        );

        verify(weatherService, times(1))
                .getAllWeatherDataForGivenDateRange(startDate, endDate);
        verifyNoMoreInteractions(weatherService);
    }

    @Test
    public void shouldGetWeatherStatsWithFailureMessageWhenNoDataForGivenDateRange() throws Exception {

        Weather expectedWeather = createWeatherDO();

        String startDateInString = "2018-02-11", endDateInString = "2018-02-12";
        Date startDate = simpleDateFormat.parse(startDateInString);
        Date endDate = simpleDateFormat.parse(endDateInString);

        List<WeatherStats> weatherStatsList = new ArrayList<>();

        WeatherStats e = new WeatherStats(expectedWeather.getLocation(), Either.right(Constants.NO_DATA_FOR_GIVEN_DATE_RANGE));
        weatherStatsList.add(e);

        given(weatherService.getAllWeatherDataForGivenDateRange(startDate, endDate))
                .willReturn( weatherStatsList);

        MockHttpServletResponse getFilterWeatherDataResponse = mockMvc.perform(
                get(WEATHERS_ENDPOINT + TEMPERATURES_ENDPOINT)
                        .param("start", startDateInString)
                        .param("end", endDateInString))
                .andDo(print())
                .andReturn().getResponse();

        assertThat(getFilterWeatherDataResponse.getStatus()).isEqualTo(HttpStatus.OK.value());
        assertThat(getFilterWeatherDataResponse.getContentAsString()).isNotEmpty();
        assertThat(getFilterWeatherDataResponse.getContentAsString()).isEqualTo(
                weatherStatsListJacksonTester.write(weatherStatsList).getJson()
        );

        verify(weatherService, times(1))
                .getAllWeatherDataForGivenDateRange(startDate, endDate);
        verifyNoMoreInteractions(weatherService);
    }

    private Weather createWeatherDO() {
        Weather expectedWeather = new Weather();
        expectedWeather.setId(1L);
        expectedWeather.setDateRecorded(new Date());
        Location location = new Location("wolfsburg", "lower saxony", 10f, 10f);
        expectedWeather.setLocation(location);
        expectedWeather.setTemperature("11, 12");
        return expectedWeather;
    }
}