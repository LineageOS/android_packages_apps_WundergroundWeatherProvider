package org.cyanogenmod.wundergroundcmweatherprovider;

import android.content.Context;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import org.cyanogenmod.wundergroundcmweatherprovider.wunderground.ConverterUtils;
import org.cyanogenmod.wundergroundcmweatherprovider.wunderground.Feature;
import org.cyanogenmod.wundergroundcmweatherprovider.wunderground.WundergroundServiceManager;
import org.cyanogenmod.wundergroundcmweatherprovider.wunderground.responses.CurrentObservationResponse;
import org.cyanogenmod.wundergroundcmweatherprovider.wunderground.responses.DisplayLocationResponse;
import org.cyanogenmod.wundergroundcmweatherprovider.wunderground.responses.ForecastResponse;
import org.cyanogenmod.wundergroundcmweatherprovider.wunderground.responses.WundergroundReponse;
import org.cyanogenmod.wundergroundcmweatherprovider.wunderground.responses.forecast.SimpleForecastResponse;

import java.util.ArrayList;

import javax.inject.Inject;

import cyanogenmod.providers.WeatherContract;
import cyanogenmod.weather.CMWeatherManager;
import cyanogenmod.weather.WeatherInfo;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DebugActivity extends WUBaseActivity implements
        CMWeatherManager.WeatherServiceProviderChangeListener,
        CMWeatherManager.WeatherUpdateRequestListener,
        LocationListener {

    private static final String TAG = DebugActivity.class.getSimpleName();

    @Inject
    WundergroundServiceManager mWundergroundServiceManager;

    private CMWeatherManager mWeatherManager;
    private LocationManager mLocationManager;

    private boolean mDirectRequest = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mWeatherManager = CMWeatherManager.getInstance(this);
        mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        mWeatherManager.registerWeatherServiceProviderChangeListener(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void requestWeatherInfo(View v) {
        mDirectRequest = false;
        requestWeatherInfo();
    }

    public void requestWeatherInfoDirectly(View v) {
        mDirectRequest = true;
        requestWeatherInfo();
    }

    private void requestWeatherInfo() {
        Log.d(TAG, "Requesting weather!");
        Criteria criteria = new Criteria();
        criteria.setAccuracy(Criteria.ACCURACY_FINE);
        mLocationManager.requestSingleUpdate(criteria, this, Looper.getMainLooper());
    }

    @Override
    public void onWeatherServiceProviderChanged(String s) {

    }

    @Override
    public void onWeatherRequestCompleted(int i, WeatherInfo weatherInfo) {
        switch (i) {
            case CMWeatherManager.WEATHER_REQUEST_COMPLETED:
                Log.d(TAG, "Weather request completed: " + weatherInfo.toString());
                break;
            case CMWeatherManager.WEATHER_REQUEST_FAILED:
                Log.d(TAG, "Weather request failed!");
                break;
            case CMWeatherManager.WEATHER_REQUEST_ALREADY_IN_PROGRESS:
                Log.d(TAG, "Weather request already in progress");
                break;
            case CMWeatherManager.WEATHER_REQUEST_SUBMITTED_TOO_SOON:
                Log.d(TAG, "Weather request submitted too soon");
                break;
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        if (mDirectRequest) {
            Call<WundergroundReponse> wundergroundCall =
                    mWundergroundServiceManager.query(location.getLatitude(),
                    location.getLongitude(), Feature.conditions, Feature.forecast);

            wundergroundCall.enqueue(new Callback<WundergroundReponse>() {
                @Override
                public void onResponse(Call<WundergroundReponse> call, Response<WundergroundReponse> response) {
                    if (response.isSuccessful()) {
                        Log.d(TAG, "Received response:\n" + response.body().toString());
                        WundergroundReponse wundergroundReponse = response.body();

                        if (wundergroundReponse == null) {
                            Log.d(TAG, "Null wu reponse, return");
                            return;
                        }

                        CurrentObservationResponse currentObservationResponse =
                                wundergroundReponse.getCurrentObservation();

                        if (currentObservationResponse == null) {
                            Log.d(TAG, "Null co reponse, return");
                            return;
                        }

                        WeatherInfo.Builder weatherInfoBuilder =
                                new WeatherInfo.Builder(System.currentTimeMillis());

                        weatherInfoBuilder.setTemperature(currentObservationResponse.getTempF()
                                        .floatValue(),
                                WeatherContract.WeatherColumns.TempUnit.FAHRENHEIT);

                        weatherInfoBuilder.setWeatherCondition(
                                WeatherContract.WeatherColumns.WeatherCode.CLOUDY);

                        DisplayLocationResponse displayLocationResponse =
                                currentObservationResponse.getDisplayLocation();

                        if (displayLocationResponse == null) {
                            Log.d(TAG, "Null dl reponse, return");
                            return;
                        }

                        weatherInfoBuilder.setCity(displayLocationResponse.getCity(),
                                displayLocationResponse.getCity());

                        ForecastResponse forecastResponse =
                                wundergroundReponse.getForecast();

                        if (forecastResponse == null) {
                            Log.d(TAG, "Null fc reponse, return");
                            return;
                        }

                        SimpleForecastResponse simpleForecastResponse =
                                forecastResponse.getSimpleForecast();

                        if (simpleForecastResponse == null) {
                            Log.d(TAG, "Null sf reponse, return");
                            return;
                        }

                        ArrayList<WeatherInfo.DayForecast> dayForecasts =
                                ConverterUtils.convertSimpleFCToDayForcast(
                                simpleForecastResponse.getForecastDay());
                        weatherInfoBuilder.setForecast(dayForecasts);

                        Log.d(TAG, "Weather info " + weatherInfoBuilder.build().toString());
                    } else {
                        Log.d(TAG, "Response " + response.toString());
                    }
                }

                @Override
                public void onFailure(Call<WundergroundReponse> call, Throwable t) {
                    Log.d(TAG, "Failure " + t.toString());
                }
            });
        } else {
            mWeatherManager.requestWeatherUpdate(location, this);
        }
    }

    @Override
    public void onStatusChanged(String s, int i, Bundle bundle) {

    }

    @Override
    public void onProviderEnabled(String s) {

    }

    @Override
    public void onProviderDisabled(String s) {

    }
}