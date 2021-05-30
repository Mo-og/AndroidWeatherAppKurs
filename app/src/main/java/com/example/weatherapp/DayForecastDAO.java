package com.example.weatherapp;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface DayForecastDAO {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertDayForecast(DayForecast dayForecast);

    @Delete
    void deleteDayForecast(DayForecast dayForecast);

    @Query("DELETE FROM day_forecast WHERE id=:id")
    void deleteDayForecastById(int id);

    @Query("SELECT * FROM day_forecast WHERE id=:id")
    DayForecast getDayForecastById(int id);

    @Query("DELETE FROM day_forecast")
    void deleteAll();

    @Query("SELECT * FROM day_forecast")
    List<DayForecast> getAll();
}
