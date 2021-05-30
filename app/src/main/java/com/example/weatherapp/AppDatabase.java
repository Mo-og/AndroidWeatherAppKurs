package com.example.weatherapp;

import android.content.Context;

import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverter;

import java.util.Date;

@androidx.room.Database(entities = {DayForecast.class}, version = 1)
public abstract class AppDatabase extends RoomDatabase {

    public static final String DATABASE_NAME = "forecast_db.db";
    private static volatile AppDatabase instance;

    public abstract DayForecastDAO dayForecastDAO();

    private static final Object LOCK = new Object();

    public static AppDatabase getInstance(Context context) {
        if (instance == null) {
            synchronized (LOCK) {
                if (instance == null)
                    instance = Room.databaseBuilder(context, AppDatabase.class, DATABASE_NAME).build();
            }
        }
        return instance;
    }

}

