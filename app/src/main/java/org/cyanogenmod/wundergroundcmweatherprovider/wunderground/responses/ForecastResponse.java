package org.cyanogenmod.wundergroundcmweatherprovider.wunderground.responses;

import com.google.gson.annotations.SerializedName;

import org.cyanogenmod.wundergroundcmweatherprovider.wunderground.responses.forecast.SimpleForecastResponse;

import java.io.Serializable;

public class ForecastResponse implements Serializable {
    @SerializedName("simpleforecast")
    private SimpleForecastResponse simpleForecast;

    public SimpleForecastResponse getSimpleForecast() {
        return simpleForecast;
    }

    public void setSimpleForecastResponse(SimpleForecastResponse simpleForecast) {
        this.simpleForecast = simpleForecast;
    }
}