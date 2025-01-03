package com.example.weatherapp;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Looper;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.Priority;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.squareup.picasso.Picasso;
import android.Manifest;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    private RelativeLayout homeRL;
    private ProgressBar loadingPB;
    private TextView cityNameTV, temperatureTV, conditionTV,humidityTV,real_feelTV,
            uvTV,pressureTV,wind_speedTV,cloudTV,visibilityTV;
    private RecyclerView weatherRV;
    private TextInputEditText cityEdit;
    private ImageView backIV, iconIV, searchIV;
    private ArrayList<WeatherRVModel> weatherRVModelArrayList;
    private WeatherRVAdapter weatherRVAdapter;
    private int PERMISSION_CODE = 1;
    private static String cityName;
    private AlertDialog.Builder alertdialogBuilder;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private ActivityResultLauncher<String[]> locationPermissionRequest;
    private static final int REQUEST_CHECK_SETTINGS = 1001;
    private static boolean granted=false;
    private String humidity,uv,real_feel,pressure,wind_speed,cloud,visibility;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        getWindow().setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        //finding views by their id
        homeRL = findViewById(R.id.idRHome);
        loadingPB = findViewById(R.id.idPBLoading);
        cityNameTV = findViewById(R.id.idTVCityName);
        temperatureTV = findViewById(R.id.idTVTemperature);
        conditionTV = findViewById(R.id.idTVCondition);

        humidityTV=findViewById(R.id.idTVHumidity);
        real_feelTV=findViewById(R.id.idTVRealFeel);
        uvTV=findViewById(R.id.idTVUV);
        pressureTV=findViewById(R.id.idTVAirPressure);
        wind_speedTV=findViewById(R.id.idTVWindKPH);
        cloudTV=findViewById(R.id.idTVCloud);
        visibilityTV=findViewById(R.id.idTVVisibility);

        weatherRV = findViewById(R.id.idRVWeather);
        cityEdit = findViewById(R.id.idEditCity);
        backIV = findViewById(R.id.idIVBack);
        iconIV = findViewById(R.id.idIVIcon);
        searchIV = findViewById(R.id.idIVSearch);
        weatherRVModelArrayList = new ArrayList<>();

        weatherRVAdapter = new WeatherRVAdapter(this, weatherRVModelArrayList);
        weatherRV.setAdapter(weatherRVAdapter);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        if (!isNetworkAvailable(this)){
            alertdialogBuilder = new AlertDialog.Builder(MainActivity.this);
            alertdialogBuilder.setTitle(R.string.title_string);
            alertdialogBuilder.setMessage(R.string.title_message);
            alertdialogBuilder.setIcon(R.drawable.alertm);
            alertdialogBuilder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    dialogInterface.dismiss();
                    finish();
                }
            });
            AlertDialog alertDialog = alertdialogBuilder.create();
            alertDialog.show();
            return;
        }

        if (!isLocationEnabled()){
            checkLocationSettings();
        }

        locationPermissionRequest =
                registerForActivityResult(new ActivityResultContracts
                                .RequestMultiplePermissions(), result -> {
                            Boolean fineLocationGranted = result.getOrDefault(
                                    Manifest.permission.ACCESS_FINE_LOCATION, false);
                            Boolean coarseLocationGranted = result.getOrDefault(
                                    Manifest.permission.ACCESS_COARSE_LOCATION,false);
                            if (fineLocationGranted != null && fineLocationGranted) {
                                // Precise location access granted.

                                if (isLocationEnabled())callbackCurrentLocation();

                            } else if (coarseLocationGranted != null && coarseLocationGranted) {
                                // Only approximate location access granted.

                                if (isLocationEnabled())callbackCurrentLocation();

                            } else {
                                // No location access granted.
                                Toast.makeText(this, "No location access granted.", Toast.LENGTH_SHORT).show();
                            }
                        }
                );


//        cityName="London";
//
//        cityNameTV.setText(cityName);
//        getWeatherInfo(cityName);


        locationPermission();
        searchIV.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String city = cityEdit.getText().toString().trim();
                if (city.isEmpty()) {
                    Toast.makeText(getApplicationContext(), "Please enter a city name", Toast.LENGTH_SHORT).show();
                } else {
                    if (isNetworkAvailable(MainActivity.this)) {
                        getWeatherInfo(city);
                    }
                    else
                        Toast.makeText(MainActivity.this, "No Internet", Toast.LENGTH_SHORT).show();
                }
            }
        });
// xử lý chọn
        weatherRVAdapter.setOnItemClickListener(model -> {
            // Update ConstraintLayout views with model data
            temperatureTV.setText(model.getTemperature() + "°c");
            humidityTV.setText(humidity + "%");
            real_feelTV.setText(model.getTemperature() + "°c");
            wind_speedTV.setText(model.getWindSpeed());

            // Update weather condition icon
            Picasso.get().load("http:".concat(model.getIcon())).into(iconIV);

            // Show the time of selected forecast
            String time = model.getTime();
            SimpleDateFormat input = new SimpleDateFormat("yyyy-MM-dd hh:mm");
            SimpleDateFormat output = new SimpleDateFormat("hh:mm aa");
            try {
                Date t = input.parse(time);
                conditionTV.setText(output.format(t));
            } catch (ParseException e) {
                e.printStackTrace();
            }
        });
    }

    private boolean isLocationEnabled() {
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        // Check if GPS or Network Provider is enabled
        boolean gpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        boolean networkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

        return gpsEnabled || networkEnabled;
    }

    private void locationPermission() {
        locationPermissionRequest.launch(new String[] {
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
        });
    }

    private void callbackCurrentLocation() {

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) {
                    return;
                }
                // Handle the current location here
                for (Location location : locationResult.getLocations()) {
                    if (location != null) {
                        double latitude = location.getLatitude();
                        double longitude = location.getLongitude();
                        getCityName(latitude,longitude);
                        // Stop updates after receiving the current location (optional)
                        fusedLocationClient.removeLocationUpdates(this);
                    }else {
                        Toast.makeText(MainActivity.this, "location is null", Toast.LENGTH_SHORT).show();
                    }

                }
            }
        };
        // Start the process of getting location updates
        requestCurrentLocation();
    }

    private void requestCurrentLocation() {

        LocationRequest locationRequest = new LocationRequest.Builder(
                Priority.PRIORITY_HIGH_ACCURACY, 1000) // 10 seconds interval
                .setMinUpdateIntervalMillis(500)       // Fastest interval of 5 seconds
                .setMaxUpdates(1)                       // Get only one update for current location
                .build();

        // Check for location permissions
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED  | ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {

            // Request a single location update (current location)
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());

        } else {
            // Request location permission if not granted
            locationPermission();
        }

    }


    private void getCityName(double latitude, double longitude) {
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(latitude, longitude, 1);
            if (addresses != null && !addresses.isEmpty()) {
                String cityName = addresses.get(0).getLocality();
                getWeatherInfo(cityName);
            } else {
                Toast.makeText(this, "City not found" ,Toast.LENGTH_SHORT).show();
            }
        } catch (IOException e) {
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    public static boolean isNetworkAvailable(Context context) {
        return ((ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE))
                .getActiveNetworkInfo() != null;
    }

    private void checkLocationSettings() {

        LocationRequest locationRequest = new LocationRequest.Builder(
                Priority.PRIORITY_HIGH_ACCURACY, 10000) // 1 seconds interval
                .setMinUpdateIntervalMillis(5000)       // Fastest interval of .5 seconds
                .build();


        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(locationRequest);


        SettingsClient settingsClient = LocationServices.getSettingsClient(this);


        Task<LocationSettingsResponse> task = settingsClient.checkLocationSettings(builder.build());


        task.addOnSuccessListener(this, locationSettingsResponse -> {
            granted=true;
        });

        task.addOnFailureListener(this, e -> {
            if (e instanceof ResolvableApiException) {
                try {
                    // Show the dialog prompting the user to enable location settings
                    ResolvableApiException resolvable = (ResolvableApiException) e;
                    resolvable.startResolutionForResult(this, REQUEST_CHECK_SETTINGS);
                } catch (IntentSender.SendIntentException sendEx) {
                    // Handle the error
                    sendEx.printStackTrace();
                }
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CHECK_SETTINGS) {
            if (resultCode == Activity.RESULT_OK) {
                // User enabled location settings, proceed to get current location
                callbackCurrentLocation();
            } else {
                // User did not enable location services
                Toast.makeText(this, "Location services are required for this feature", Toast.LENGTH_SHORT).show();
            }
        }
    }

    //method to get weather information of a specific city that user searched for
    private void getWeatherInfo(String cityName) {
        String api_key= "9cfc41ac26cd4eebb7b133930250301";
        //Replace Credentials.api_key with your own api key
        //To get your api key go to https://www.weatherapi.com/ and signup
        //after successful signup they will provide your an api key
//        String url = "http://api.weatherapi.com/v1/forecast.json?key= "+Credentials.api_key+"&q=" + cityName + "&days=1&aqi=yes&alerts=yes";
        String url = "http://api.weatherapi.com/v1/forecast.json?key= "+api_key +"&q=" + cityName + "&days=1&aqi=yes&alerts=yes";
        // xử lý main
        RequestQueue requestQueue = Volley.newRequestQueue(this);
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, url, null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {

                loadingPB.setVisibility(View.GONE);
                homeRL.setVisibility(View.VISIBLE);
                weatherRVModelArrayList.clear();
                try {
                    String city_name;
                    city_name=response.getJSONObject("location").getString("name");
                    cityNameTV.setText(city_name);

                    String temperature = response.getJSONObject("current").getString("temp_c");
                    temperatureTV.setText(temperature + "°C");
                    int isDay = response.getJSONObject("current").getInt("is_day");
                    String condition = response.getJSONObject("current").getJSONObject("condition").getString("text");
                    conditionTV.setText(condition);
                    String conditionIcon = response.getJSONObject("current").getJSONObject("condition").getString("icon");
                    Picasso.get().load("http:".concat(conditionIcon)).into(iconIV);

                    humidity =response.getJSONObject("current").getString("humidity");
                    real_feel=response.getJSONObject("current").getString("feelslike_c");
                    uv=response.getJSONObject("current").getString("uv");
                    pressure=response.getJSONObject("current").getString("pressure_mb");
                    wind_speed=response.getJSONObject("current").getString("wind_kph");
                    cloud=response.getJSONObject("current").getString("cloud");
                    visibility=response.getJSONObject("current").getString("vis_km");

                    humidityTV.setText(humidity+"%");
                    real_feelTV.setText(real_feel+ "°C");
                    uvTV.setText(uv);
                    pressureTV.setText(pressure+"mbar");
                    wind_speedTV.setText(wind_speed+"km/h");
                    cloudTV.setText(cloud+"%");
                    visibilityTV.setText(visibility+"km");


                    if (isDay == 1) {
                        ///day
                        Picasso.get().load("https://images.pexels.com/photos/2086748/pexels-photo-2086748.jpeg?auto=compress&cs=tinysrgb&w=1260&h=750&dpr=1").into(backIV);
                    } else {
                        ///night
                        Picasso.get().load("https://images.pexels.com/photos/36487/above-adventure-aerial-air.jpg").into(backIV);
                    }

                    JSONObject forecastObj = response.getJSONObject("forecast");
                    JSONObject forecast0 = forecastObj.getJSONArray("forecastday").getJSONObject(0);
                    JSONArray hourArray = forecast0.getJSONArray("hour");
                    for (int i = 0; i < hourArray.length(); i++) {
                        JSONObject hourObj = hourArray.getJSONObject(i);
                        String time = hourObj.getString("time");
                        String temper = hourObj.getString("temp_c");
                        String image = hourObj.getJSONObject("condition").getString("icon");
                        String wind = hourObj.getString("wind_kph");
                        weatherRVModelArrayList.add(new WeatherRVModel(time, temper, image, wind));
                    }
                    weatherRVAdapter.notifyDataSetChanged();

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Toast.makeText(getApplicationContext(), "Please enter valid city name", Toast.LENGTH_SHORT).show();
            }
        });
        requestQueue.add(jsonObjectRequest);
    }

    @Override
    public void onBackPressed() {
        alertdialogBuilder = new AlertDialog.Builder(MainActivity.this);
        alertdialogBuilder.setTitle(R.string.title_string1);
        alertdialogBuilder.setMessage(R.string.title_message1);
        alertdialogBuilder.setIcon(R.drawable.alertm);
        alertdialogBuilder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                finish();
            }
        });
        alertdialogBuilder.setNegativeButton("NO", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {

            }
        });
        AlertDialog alertDialog = alertdialogBuilder.create();
        alertDialog.show();
    }

}