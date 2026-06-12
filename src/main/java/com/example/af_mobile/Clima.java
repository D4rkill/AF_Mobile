package com.example.af_mobile;

import com.google.gson.annotations.SerializedName;

public class Clima {

    private Current current;

    public Current getCurrent() {
        return current;
    }

    public static class Current {

        @SerializedName("temperature_2m")
        private double temperature2m;

        @SerializedName("wind_speed_10m")
        private double windSpeed10m;

        @SerializedName("weather_code")
        private int weatherCode;

        public double getTemperature2m() {
            return temperature2m;
        }

        public double getWindSpeed10m() {
            return windSpeed10m;
        }

        public int getWeatherCode() {
            return weatherCode;
        }
    }
}