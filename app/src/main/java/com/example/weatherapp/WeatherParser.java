package com.example.weatherapp;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;
import java.util.Locale;

public class WeatherParser {
    public static Weather getWeather(String response) throws JSONException {
        JSONObject weatherJson = new JSONObject(response);
        return getWeather(weatherJson);
    }

    private static DayForecast parseDayForecast(JSONObject day) throws JSONException {
        DayForecast daily = new DayForecast();
        daily.setDate(
                new Date(day.getLong("dt") * 1000));
        daily.setSunrise(
                new Date(day.getLong("sunrise") * 1000));
        daily.setSunset(
                new Date(day.getLong("sunset") * 1000));
        daily.setWindSpeed(
                Math.abs(day.getDouble("wind_speed")));
        daily.setPressure(
                day.getInt("pressure"));
        try {
            daily.setTemp(
                    day.getDouble("temp"));
        } catch (JSONException e) {
            daily.setTemp(day.getJSONObject("temp").getDouble("day"));
        }
        try {
            daily.setFeelsLike(
                    day.getDouble("feels_like"));
        } catch (JSONException e) {
            daily.setFeelsLike(day.getJSONObject("feels_like").getDouble("day"));
        }
        try {
            daily.setPrecipitation(
                    day.getInt("rain")
            );
        } catch (JSONException e) {
            daily.setPrecipitation(-1);
        }
        daily.setHumidity(
                day.getInt("humidity"));
        daily.setDewPoint(
                day.getDouble("dew_point"));
        daily.setUvi(
                day.getDouble("uvi"));
        daily.setCloudinessPerc(
                day.getInt("clouds"));
        try {
            daily.setVisibility(
                    day.getInt("visibility"));
        } catch (JSONException e) {
            daily.setVisibility(-1);
        }
        daily.setWeatherId(
                day.getJSONArray("weather").getJSONObject(0).getInt("id"));
        String description = day.getJSONArray("weather").getJSONObject(0).getString("description");
        description = String.valueOf(description.charAt(0)).toUpperCase(Locale.ROOT) + description.substring(1);
        daily.setDescription(description);
        return daily;
    }

    private static Weather getWeather(JSONObject weatherJson) throws JSONException {
        JSONObject day = weatherJson.getJSONObject("current");
        Weather weather = new Weather();
        weather.setCurrentForecast(parseDayForecast(day));
        JSONArray jsonDays = weatherJson.getJSONArray("daily");
        DayForecast[] days = new DayForecast[jsonDays.length()];
        for (int i = 0; i < jsonDays.length(); i++) {
            days[i] = parseDayForecast(jsonDays.getJSONObject(i));
        }
        weather.setDailyForecast(days);
        return weather;
    }
}
