package com.wengeo.sensorlog;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.location.GnssMeasurementsEvent;
import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;

public interface MeasurementListener {
    /** @see LocationListener#onProviderEnabled(String) */
    void onProviderEnabled(String provider);
    /** @see LocationListener#onProviderDisabled(String) */
    void onProviderDisabled(String provider);
    /** @see LocationListener#onLocationChanged(Location) */
    void onLocationChanged(Location location);
    /** @see LocationListener#onStatusChanged(String, int, Bundle) */
    void onLocationStatusChanged(String provider, int status, Bundle extras);

    /** Called when the listener is registered to listen to GNSS events */
    void onListenerRegistration(String listener, boolean result);

    /* @see GnssMeasurementsEvent.Callback#    onGnssMeasurementsReceived(GnssMeasurementsEvent)  */
    void onGnssMeasurementsReceived(GnssMeasurementsEvent event);
    /** @see GnssMeasurementsEvent.Callback#onStatusChanged(int) */
    void onGnssMeasurementsStatusChanged(int status);

    void onSensorChanged(SensorEvent event); // wenlin 2022-02-02
    void onAccuracyChanged(Sensor sensor, int i);
}
