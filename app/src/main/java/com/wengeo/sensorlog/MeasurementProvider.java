package com.wengeo.sensorlog;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.GnssMeasurementsEvent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class MeasurementProvider {
    Context context;
    public static final String TAG = "MeasurementProvider";
    private boolean mLogMeasurements = true;

    private static final long LOCATION_RATE_GPS_MS = TimeUnit.SECONDS.toMillis(1L);

    private final List<MeasurementListener> mListeners;
    private final LocationManager mLocationManager;
    private boolean mLogLocations = true;

    private final SensorManager sensorManager;
    Sensor mAcc, mGyr, mOri;

    private final LocationListener mLocationListener = new LocationListener() {
        @Override
        public void onProviderEnabled(@NonNull String provider) {
            if (mLogLocations) {
                for (MeasurementListener listener : mListeners) {
                    listener.onProviderEnabled(provider);
                }
            }
        }

        @Override
        public void onProviderDisabled(@NonNull String provider) {
            if (mLogLocations) {
                for (MeasurementListener logger : mListeners) {
                    logger.onProviderDisabled(provider);
                }
            }
        }

        @Override
        public void onLocationChanged(@NonNull Location location) {
            if (mLogLocations) {
                for (MeasurementListener logger : mListeners) {
                    logger.onLocationChanged(location);
                }
            }
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
            if (mLogLocations) {
                for (MeasurementListener logger : mListeners) {
                    logger.onLocationStatusChanged(provider, status, extras);
                }
            }
        }
    };

    public MeasurementProvider(Context c, MeasurementListener... loggers) {
        context = c;
        this.mListeners = Arrays.asList(loggers);
        mLocationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
    }


    public void registerLocation() {
        boolean isGpsProviderEnabled = mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        if (isGpsProviderEnabled) {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(context, "registerLocation permission not granted", Toast.LENGTH_LONG).show();
            }
            mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 500, 0.0f, mLocationListener);
        }
        logRegistration("LocationUpdates", isGpsProviderEnabled);
    }

    public void unregisterLocation() {
        mLocationManager.removeUpdates(mLocationListener);
    }

    public void registerMeasurements() {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(context, " registerMeasurements permission not granted", Toast.LENGTH_LONG).show();
            return;
        }
        logRegistration("GnssMeasurements", mLocationManager.registerGnssMeasurementsCallback(gnssMeasurementsEventListener));
    }
    public void unregisterMeasurements() {
        mLocationManager.unregisterGnssMeasurementsCallback(gnssMeasurementsEventListener);
    }

    public void registerSensors() {
//        Log.d("********************Sensors", "SensorStream****************************");
        mAcc = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mGyr = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        mOri = sensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION);
        logRegistration("Sensors", sensorManager.registerListener(sensorEventListener, mAcc, SensorManager.SENSOR_DELAY_NORMAL));
        logRegistration("Sensors", sensorManager.registerListener(sensorEventListener, mGyr, SensorManager.SENSOR_DELAY_NORMAL));
        logRegistration("Sensors", sensorManager.registerListener(sensorEventListener, mOri, SensorManager.SENSOR_DELAY_NORMAL));
    }

    public void unregisterSensors() {    }

    private final SensorEventListener sensorEventListener=new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent sensorEvent) {
//            Log.d("********************Uiloggger Sensors", "SensorStream****************************");
            if (mLogLocations) {
                for (MeasurementListener logger : mListeners) {
                    logger.onSensorChanged(sensorEvent);
                }
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int i) {
            if (mLogLocations) {
                for (MeasurementListener logger : mListeners) {
                    logger.onAccuracyChanged(sensor,i);
                }
            }
        }
    };


    private final GnssMeasurementsEvent.Callback gnssMeasurementsEventListener =
            new GnssMeasurementsEvent.Callback() {
                @Override
                public void onGnssMeasurementsReceived(GnssMeasurementsEvent event) {
                    if (mLogMeasurements) {
                        for (MeasurementListener logger : mListeners) {
                            logger.onGnssMeasurementsReceived(event);
                        }
                    }
                }

                @Override
                public void onStatusChanged(int status) {
                    if (mLogMeasurements) {
                        for (MeasurementListener logger : mListeners) {
                            logger.onGnssMeasurementsStatusChanged(status);
                        }
                    }
                }
            }; // private final GnssMeasurementsEvent.Callback


    private void logRegistration(String listener, boolean result) {
        for (MeasurementListener logger : mListeners) {    // if (logger instanceof AgnssUiLogger && !firstTime) { 2.0.0.1
            logger.onListenerRegistration(listener, result);
        }
    }

}

