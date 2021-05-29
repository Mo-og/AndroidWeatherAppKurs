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
import java.util.Locale;

import io.nlopez.smartlocation.OnLocationUpdatedListener;
import io.nlopez.smartlocation.SmartLocation;


public class MainActivity extends AppCompatActivity {
    private ImageButton refreshButton;
    private TableLayout tableLayout;
    private Location location;
    private Context context;
    TextView testText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        context = this;
        testText = (TextView) findViewById(R.id.responseText);

        refreshButton = (ImageButton) findViewById(R.id.refreshButton);
        tableLayout = (TableLayout) findViewById(R.id.daily_forecast_table);
        refreshButton.setOnClickListener(v -> requestData());
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        if (isInternetPermissionGranted()) {
            requestData();
        } else
            Toast.makeText(MainActivity.this, R.string.internetPermission, Toast.LENGTH_SHORT).show();

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 3);
        } else
            SmartLocation.with(this).location()
                    .oneFix()
                    .start(new OnLocationUpdatedListener() {
                        @Override
                        public void onLocationUpdated(Location l) {
                            location = l;
                            System.out.println("GOT LOCATION!");
                        }
                    });
    }


    private boolean isInternetPermissionGranted() {
        if (checkSelfPermission(Manifest.permission.INTERNET) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.INTERNET}, 1);
            return checkSelfPermission(Manifest.permission.INTERNET) == PackageManager.PERMISSION_GRANTED;
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull @NotNull String[] permissions, @NonNull @NotNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        SmartLocation.with(context).location()
                .oneFix()
                .start(new OnLocationUpdatedListener() {
                    @Override
                    public void onLocationUpdated(Location l) {
                        location = l;
                        System.out.println("GOT LOCATION!");
                        doGeocoding();
                    }
                });
    }

    private boolean isAwaitingGeocoding = false;

    private void checkLater(int tries) {
        if (isAwaitingGeocoding) return;
        else isAwaitingGeocoding = true;
        final int attempts = tries;
        Thread thread = new Thread(() -> {
            int time = attempts;
            while (time-- > 0) {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                if (location != null) {
                    doGeocoding();
                    return;
                }
                if (time <= 0)
                    isAwaitingGeocoding = false;
            }
        });
        thread.start();


    }

    private void doGeocoding() {
        if (location != null)
            SmartLocation.with(context).geocoding()
                    .reverse(location, (original, results) -> {
                        if (!results.isEmpty()) {
                            System.out.println("RESULTS FOUND!");
                            Address address = results.get(results.size() - 1);
                            testText.setText(String.valueOf(address.getAddressLine(0)));
                            TextView city = findViewById(R.id.current_location);
                            city.setText(address.getSubLocality());
                            requestData(location.getLatitude(), location.getLongitude());
                            findViewById(R.id.location_arrow_img).setVisibility(View.VISIBLE);
                        } else System.out.println("RESULTS ARE EMPTY");
                        isAwaitingGeocoding = false;
                    });
        else checkLater(5);
    }

    @SuppressLint("SetTextI18n")
    private void refreshData(String jsonResponse) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 3);
        }
        doGeocoding();
        Weather weather = new Weather();
        DateFormat formatter = new SimpleDateFormat("EEEE", Locale.getDefault());
        int childCount = tableLayout.getChildCount();
        if (childCount > 1) {
            tableLayout.removeViews(1, childCount - 1);
        }
        try {
            weather = WeatherParser.getWeather(jsonResponse);
        } catch (JSONException e) {
            e.printStackTrace();
            Toast.makeText(MainActivity.this, "Произошла ошибка при обработке ответа сервера!", Toast.LENGTH_SHORT).show();
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
        wind.setText(Double.toString(weather.getCurrentForecast().getWindSpeed()) + " м/с");
        humidity.setText(Integer.toString(weather.getCurrentForecast().getHumidity()) + "%");
        precipitation.setText(Integer.toString((int) Math.round(weather.getDailyForecast()[0].getPrecipitation())) + "%");
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
            dayTemp.setText(Long.toString(Math.round(weather.getDailyForecast()[i].getTemp())) + "°");

            TextView dayDescr = new TextView(this);
            dayDescr.setTextColor(ContextCompat.getColor(this, R.color.white));
            dayDescr.setText(weather.getDailyForecast()[i].getDescription());

            TextView dayWindSpeed = new TextView(this);
            dayWindSpeed.setTextColor(ContextCompat.getColor(this, R.color.white));
            dayWindSpeed.setText(Double.toString(weather.getDailyForecast()[i].getWindSpeed()));

            TextView dayPrecip = new TextView(this);
            dayPrecip.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
            dayPrecip.setTextColor(ContextCompat.getColor(this, R.color.white));
            dayPrecip.setText(String.valueOf(Math.round(weather.getDailyForecast()[i].getPrecipitation())) + " м/с");
//            ImageView imageView = new ImageView(this);

            row.addView(dayText);
            row.addView(dayTemp);
            row.addView(dayDescr);
            row.addView(dayPrecip);
            tableLayout.addView(row, i + 1);
        }
    }

    public void requestData() {
        String lat = "46.482525";
        String lon = "30.723309";
        requestData(lat, lon);
    }

    public void requestData(double lat, double lon) {
        requestData(Double.toString(lat), Double.toString(lon));
    }

    public void requestData(String lat, String lon) {
        String requestUrl = "https://api.openweathermap.org/data/2.5/onecall?lat=" + lat + "&lon=" + lon + "&exclude=minutely,hourly,alerts&appid=cbac1812fd456279a96a26a52409bdaa&lang=ru&units=metric";
        RequestQueue queue = Volley.newRequestQueue(this);
        final TextView textView = (TextView) findViewById(R.id.responseText);
// Request a string response from the provided URL.
        StringRequest stringRequest = new StringRequest(Request.Method.GET, requestUrl,
                request -> refreshData(request.toString()),
                error -> Toast.makeText(MainActivity.this, "Произошла ошибка сервера!", Toast.LENGTH_SHORT).show());

// Add the request to the RequestQueue.
        queue.add(stringRequest);
    }
}