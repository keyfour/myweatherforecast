/*
 * Copyright 2018 (c) Aleksandr Karpov <keyfour13@gmail.com>
 */
package com.github.keyfour.myweatherforecast.features.weatherforecast.current;

import android.content.Context;
import android.location.Location;

import com.github.keyfour.myweatherforecast.data.OpenWeatherMap;
import com.github.keyfour.myweatherforecast.data.SharedPrefUserData;
import com.github.keyfour.myweatherforecast.data.UserData;
import com.github.keyfour.myweatherforecast.data.gps.GPSService;
import com.github.keyfour.myweatherforecast.data.gps.SmartGPS;
import com.github.keyfour.myweatherforecast.model.pojo.Forecast;

import java.util.concurrent.TimeUnit;

import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;


/**
 * @author Alex Karpov <keyfour13@gmail.com>
 */

public class CurrentPresenter implements CurrentContract.Presenter {

    private final CurrentContract.View view;
    private final GPSService gpsService;
    private Disposable disposableLoaction;
    private Disposable disposableForecast;
    private String apiKey = "";

    public CurrentPresenter(CurrentContract.View view) {
        this.view = view;
        gpsService = new SmartGPS();
    }

    @Override
    public void start(Context context) {
        UserData userData = new SharedPrefUserData(context);
        apiKey = userData.getOpenWeatherApiKey();
        gpsService.getLocation(context)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .timeout(1, TimeUnit.MINUTES)
                .safeSubscribe(new CoordinatesObserver());
    }

    @Override
    public void stop() {
        if (disposableForecast != null && !disposableForecast.isDisposed()) {
            disposableForecast.dispose();
        }
        if (disposableLoaction != null && !disposableLoaction.isDisposed()) {
            disposableLoaction.dispose();
        }
    }

    public class CoordinatesObserver implements Observer<Location> {

        @Override
        public void onSubscribe(Disposable d) {
            disposableLoaction = d;
        }

        @Override
        public void onNext(Location location) {
            view.showLocation(location);
            OpenWeatherMap.getInstance().getService().getForecast(location.getLatitude(),
                    location.getLongitude(), apiKey)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .safeSubscribe(new ForecastObserver());
        }

        @Override
        public void onError(Throwable e) {
            view.showMessage(e.getMessage());
        }

        @Override
        public void onComplete() {

        }
    }

    public class  ForecastObserver implements Observer<Forecast> {

        @Override
        public void onSubscribe(Disposable d) {
           disposableForecast = d;
        }

        @Override
        public void onNext(Forecast forecast) {
            view.showCurrentForecast(forecast);
        }

        @Override
        public void onError(Throwable e) {
            view.showMessage(e.getMessage());
        }

        @Override
        public void onComplete() {

        }
    }
}
