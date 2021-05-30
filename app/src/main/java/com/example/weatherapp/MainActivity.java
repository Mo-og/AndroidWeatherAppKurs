package com.example.weatherapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Location;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageButton;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.jetbrains.annotations.NotNull;
import org.json.JSONException;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import io.nlopez.smartlocation.OnLocationUpdatedListener;
import io.nlopez.smartlocation.SmartLocation;
import io.nlopez.smartlocation.location.config.LocationParams;


public class MainActivity extends AppCompatActivity {
    final int CODE_INTERNET = 1;
    final int CODE_LOCATION = 3;
    private ImageButton refreshButton;
    private TableLayout tableLayout;
    private Location location;
    private Context context;
    private Animation spinAnimation;
    private TextView testText;
    private RequestQueue queue;

    private List<DayForecast> list;
    private AppDatabase db;
    private final Executor executor = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        context = this;
        testText = findViewById(R.id.responseText);
        queue = Volley.newRequestQueue(this);
        db = AppDatabase.getInstance(this);

        refreshButton = findViewById(R.id.refreshButton);
        ImageButton showLocationButton = findViewById(R.id.show_location_button);
        showLocationButton.setOnClickListener(v -> {
            if (testText.getVisibility() == View.INVISIBLE)
                testText.setVisibility(View.VISIBLE);
            else testText.setVisibility(View.INVISIBLE);
        });
        spinAnimation = AnimationUtils.loadAnimation(this, R.anim.roll);
        tableLayout = findViewById(R.id.daily_forecast_table);
        refreshButton.setOnClickListener(v -> refreshLocation());

        loadForecast();
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        if (isInternetPermissionGranted()) {
            refreshLocation();
        } else
            Toast.makeText(MainActivity.this, R.string.internetPermission, Toast.LENGTH_SHORT).show();

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, CODE_LOCATION);
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
                Toast.makeText(MainActivity.this, R.string.locationPermission, Toast.LENGTH_SHORT).show();
        } else
            refreshLocation();
    }

    public void saveForecast(Weather weather) {
        executor.execute(() -> db.dayForecastDAO().deleteAll());
        executor.execute(() -> db.dayForecastDAO().insertDayForecast(weather.getCurrentForecast()));
        for (DayForecast d : weather.getDailyForecast())
            executor.execute(() -> db.dayForecastDAO().insertDayForecast(d));
        executor.execute(() -> list = db.dayForecastDAO().getAll());
        testText.setText(list.toString());
    }


    public void loadForecast() {
        executor.execute(() -> {
            list = db.dayForecastDAO().getAll();
            if (list == null || list.isEmpty()) {
                System.out.println("DaysForecast list from DB is empty: " + list);
            } else {
                Weather weather = new Weather();
                weather.setCurrentForecast(list.get(0));
                list.remove(0);
                DayForecast[] dayForecasts = new DayForecast[list.size()];
                for (int i = 0; i < list.size(); i++) {
                    dayForecasts[i] = list.get(i);
                }
                weather.setDailyForecast(dayForecasts);
                refreshData(weather);
            }
        });
    }


    private boolean isInternetPermissionGranted() {
        if (checkSelfPermission(Manifest.permission.INTERNET) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.INTERNET}, CODE_INTERNET);
            return checkSelfPermission(Manifest.permission.INTERNET) == PackageManager.PERMISSION_GRANTED;
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull @NotNull String[] permissions, @NonNull @NotNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        refreshLocation();
    }

    private boolean isAwaitingGeocoding = false;

    private void checkLater(int tries) {
        if (isAwaitingGeocoding) return;
        isAwaitingGeocoding = true;
        final int attempts = tries;
        final Thread thread = new Thread(() -> {
            int time = attempts;
            while (time-- > 0) {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                if (location != null) {
                    doGeocoding();
                    isAwaitingGeocoding = false;
                    return;
                }
                if (time <= 0) {
                    isAwaitingGeocoding = false;
                    return;
                }
            }
        });
        thread.start();


    }

    private void tryGeocoding() {
        if (location != null)
            doGeocoding();
        else checkLater(5);
    }

    private void stopLocating() {
        SmartLocation.with(context).location().stop();
        SmartLocation.with(context).geocoding().stop();
        SmartLocation.with(context).activity().stop();
    }

    private void doGeocoding() {
        refreshButton.clearAnimation();
        if (location != null) {
            SmartLocation.with(context).geocoding()
                    .reverse(location, (original, results) -> {
                        if (!results.isEmpty()) {
                            Address address = results.get(results.size() - 1);
                            testText.setText(String.valueOf(address.getAddressLine(0)));
                            TextView city = findViewById(R.id.current_location);
                            if (address.getSubLocality() == null)
                                city.setText("Текущее местоположение");
                            else
                                city.setText(address.getSubLocality());
                            requestData(location.getLatitude(), location.getLongitude());
                            findViewById(R.id.location_arrow_img).setVisibility(View.VISIBLE);
                        } else
                            System.out.println("RESULTS ARE EMPTY - address not found for current location");
                    });
        }
    }

    private Weather parseWeather(String jsonResponse) {
        Weather weather = new Weather();
        try {
            weather = WeatherParser.getWeather(jsonResponse);
        } catch (JSONException e) {
            e.printStackTrace();
            Toast.makeText(MainActivity.this, "Произошла ошибка при обработке ответа сервера!", Toast.LENGTH_SHORT).show();
        }
        saveForecast(weather);
        return weather;
    }

    @SuppressLint("SetTextI18n")
    private void refreshData(Weather weather) {

        if (weather == null) {
            Toast.makeText(MainActivity.this, "Предыдущие данные о погоде недоступны!", Toast.LENGTH_SHORT).show();
            return;
        }

        DateFormat formatter = new SimpleDateFormat("EEEE", Locale.getDefault());

        int childCount = tableLayout.getChildCount();
        if (childCount > 1) {
            tableLayout.removeViews(1, childCount - 1);
        }
        TextView currentTemp = findViewById(R.id.current_temp);
        TextView currentDescr = findViewById(R.id.current_descr);

        TextView feelsLike = findViewById(R.id.feels_like);
        TextView wind = findViewById(R.id.wind_speed);
        TextView humidity = findViewById(R.id.humidity);
        TextView precipitation = findViewById(R.id.precipitation);

        currentTemp.setText(Math.round(weather.getCurrentForecast().getTemp()) + "°");
        currentDescr.setText(weather.getCurrentForecast().getDescription());

        feelsLike.setText(Math.round(weather.getCurrentForecast().getFeelsLike()) + "°");
        wind.setText(weather.getCurrentForecast().getWindSpeed() + " м/с");
        humidity.setText(weather.getCurrentForecast().getHumidity() + "%");
        precipitation.setText((int) Math.round(weather.getDailyForecast()[0].getPrecipitation()) + "%");
        TableRow topRow = new TableRow(this);
        TextView tempr = new TextView(this);
        TextView weekDay = new TextView(this);
        TextView descr = new TextView(this);
        TextView windSpeed = new TextView(this);

        tempr.setText(R.string.temperature);
        weekDay.setText(R.string.weekday);
        descr.setText(R.string.forecast_description);
        windSpeed.setText(R.string.wind_speed);

        topRow.addView(tempr);
        topRow.addView(weekDay);
        topRow.addView(descr);
        topRow.addView(windSpeed);

        for (int i = 0; i < weather.getDailyForecast().length; i++) {
            TableRow row = new TableRow(this);
            row.setGravity(Gravity.CENTER_HORIZONTAL);
            TableRow.LayoutParams lp = new TableRow.LayoutParams(TableRow.LayoutParams.WRAP_CONTENT);
            row.setLayoutParams(lp);

            TextView dayText = new TextView(this);
            dayText.setTextColor(ContextCompat.getColor(this, R.color.white));
            String weekDayStr = formatter.format(weather.getDailyForecast()[i].getDate());
            weekDayStr = String.valueOf(weekDayStr.charAt(0)).toUpperCase() + weekDayStr.substring(1);
            dayText.setText(weekDayStr);

            TextView dayTemp = new TextView(this);
            dayTemp.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
            dayTemp.setTextColor(ContextCompat.getColor(this, R.color.white));
            dayTemp.setText(Math.round(weather.getDailyForecast()[i].getTemp()) + "°");

            TextView dayDescr = new TextView(this);
            dayDescr.setTextColor(ContextCompat.getColor(this, R.color.white));
            dayDescr.setText(weather.getDailyForecast()[i].getDescription());

            TextView dayWindSpeed = new TextView(this);
            dayWindSpeed.setTextColor(ContextCompat.getColor(this, R.color.white));
            dayWindSpeed.setText(Double.toString(weather.getDailyForecast()[i].getWindSpeed()));

            TextView dayPrecip = new TextView(this);
            dayPrecip.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
            dayPrecip.setTextColor(ContextCompat.getColor(this, R.color.white));
            dayPrecip.setText(Math.round(weather.getDailyForecast()[i].getPrecipitation()) + " м/с");
//            ImageView imageView = new ImageView(this);

            row.addView(dayText);
            row.addView(dayTemp);
            row.addView(dayDescr);
            row.addView(dayPrecip);
            tableLayout.addView(row, i + 1);
        }
    }

    //one time location refreshing, resolving address and displaying new forecast
    private void refreshLocation() {
        if (refreshButton != null)
            refreshButton.startAnimation(spinAnimation);

        final int[] counter = {1};
        SmartLocation.with(this).location().config(LocationParams.NAVIGATION).continuous()
//                .oneFix()
                .start(l -> {
                    if (location != null) {
                        if ((location.getLatitude() - l.getLatitude() <= 0.003
                                && location.getLongitude() - l.getLongitude() <= 0.003 && counter[0]++ >= 2) || counter[0] >= 8) {
                            counter[0] = 0;
                            stopLocating();
                        }
                    }
                    location = l;
                    System.out.println("GOT LOCATION!");
                    if (counter[0]++ == 0)
                        doGeocoding();
                });
    }

    public void requestData(double lat, double lon) {
        requestData(Double.toString(lat), Double.toString(lon));
    }

    public void requestData(String lat, String lon) {
//        if (true)
//            return;
        System.out.println("Requested new forecast");
        String apiKey = "81d18acbc6ea0f4c8648ea61512deb3e"; //first account
//        apiKey = "157596984a9aadcef048f98b4a1c7079";//second account
//        apiKey ="81d18acbc6ea0f4c8648ea61addwqdqqwfqf23f512deb3e"; // invalid
        String requestUrl = "https://api.openweathermap.org/data/2.5/onecall?lat=" + lat + "&lon=" + lon + "&exclude=minutely,hourly,alerts&appid=" + apiKey + "&lang=ru&units=metric";
        final TextView textView = findViewById(R.id.responseText);
// Request a string response from the provided URL.
        StringRequest stringRequest = new StringRequest(Request.Method.GET, requestUrl,
                request -> {
                    System.out.println("Got forecast successfully!");
                    refreshData(parseWeather(request));
                },
                error -> {
                    System.out.println("Failed to get forecast!");
                    Toast.makeText(MainActivity.this, "Нет подключения к интернету, либо произошла ошибка сервера прогнозов!", Toast.LENGTH_LONG).show();
                });

// Add the request to the RequestQueue.
        queue.add(stringRequest);
    }
}