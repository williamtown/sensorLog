package com.wengeo.sensorlog;

import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.location.GnssClock;
import android.location.GnssMeasurement;
import android.location.GnssMeasurementsEvent;
import android.location.GnssStatus;
import android.location.Location;
import android.location.LocationProvider;
import android.os.Bundle;
import android.util.Log;

import java.text.DecimalFormat;

/* A class representing a UI logger for the application. Its responsibility is show information in the UI. */
public class UiLogger implements MeasurementListener{
    private static final int USED_COLOR = Color.rgb(0x4a, 0x5f, 0x70);

    public UiLogger() { }

    private LoggerFragment.UIFragmentComponent mUiFragmentComponent;
    public synchronized LoggerFragment.UIFragmentComponent getUiFragmentComponent() {
        return mUiFragmentComponent;
    }
    public synchronized void setUiFragmentComponent(LoggerFragment.UIFragmentComponent value) {
        mUiFragmentComponent = value;
    }

    private void logLocationEvent(String event) {
        logEvent("Location", event, USED_COLOR);
    }

    private void logEvent(String tag, String message, int color) {
        String composedTag = MeasurementProvider.TAG + tag;
        Log.d(composedTag, message);
        logText(tag, message, color);
    }

    private void logText(String tag, String text, int color) {
        LoggerFragment.UIFragmentComponent component = getUiFragmentComponent();
        if (component != null) {
            component.logTextFragment(tag, text, color);
        }
    }

    @Override
    public void onProviderEnabled(String provider) {
        logLocationEvent("onProviderEnabled: " + provider);
    }

    @Override
    public void onProviderDisabled(String provider) {
        logLocationEvent("onProviderDisabled: " + provider);
    }

    @Override
    public void onLocationChanged(Location location) {
        logLocationEvent("onLocationChanged: " + location + "\n");
    }

    @Override
    public void onLocationStatusChanged(String provider, int status, Bundle extras) {
        String message = String.format("onStatusChanged: provider=%s, status=%s, extras=%s",
                        provider, locationStatusToString(status), extras);
        logLocationEvent(message);
    }

    private String locationStatusToString(int status) {
        switch (status) {
            case LocationProvider.AVAILABLE:
                return "AVAILABLE";
            case LocationProvider.OUT_OF_SERVICE:
                return "OUT_OF_SERVICE";
            case LocationProvider.TEMPORARILY_UNAVAILABLE:
                return "TEMPORARILY_UNAVAILABLE";
            default:
                return "<Unknown>";
        }
    }

    @Override
    public void onListenerRegistration(String listener, boolean result) {
        logEvent("Registration", String.format("add%sListener: %b", listener, result), USED_COLOR);
    }

    private String toStringClock(GnssClock gnssClock) {
        final String format = "   %-4s = %s\n";
        StringBuilder builder = new StringBuilder("GnssClock:\n");
        DecimalFormat numberFormat = new DecimalFormat("#0.000");
        if (gnssClock.hasLeapSecond()) {
            builder.append(String.format(format, "LeapSecond", gnssClock.getLeapSecond()));
        }

        builder.append(String.format(format, "TimeNanos", gnssClock.getTimeNanos()));
        if (gnssClock.hasTimeUncertaintyNanos()) {
            builder.append( String.format(format, "TimeUncertaintyNanos", gnssClock.getTimeUncertaintyNanos()));
        }

        if (gnssClock.hasFullBiasNanos()) {
            builder.append(String.format(format, "FullBiasNanos", gnssClock.getFullBiasNanos()));
        }

        if (gnssClock.hasBiasNanos()) {
            builder.append(String.format(format, "BiasNanos", gnssClock.getBiasNanos()));
        }
        if (gnssClock.hasBiasUncertaintyNanos()) {
            builder.append(String.format(format,"BiasUncertaintyNanos",numberFormat.format(gnssClock.getBiasUncertaintyNanos())));
        }

        if (gnssClock.hasDriftNanosPerSecond()) {
            builder.append(String.format(format,"DriftNanosPerSecond", numberFormat.format(gnssClock.getDriftNanosPerSecond())));
        }

        if (gnssClock.hasDriftUncertaintyNanosPerSecond()) {
            builder.append(String.format(format,"DriftUncertaintyNanosPerSecond",numberFormat.format(gnssClock.getDriftUncertaintyNanosPerSecond())));
        }

        builder.append(String.format(format,"HardwareClockDiscontinuityCount",gnssClock.getHardwareClockDiscontinuityCount()));

        return builder.toString();
    } // private String toStringClock

    private String toStringMeasurement(GnssMeasurement measurement) {
        final String format = "   %-4s = %s\n";
        StringBuilder builder = new StringBuilder("GnssMeasurement:\n");
        DecimalFormat numberFormat = new DecimalFormat("#0.000");
        DecimalFormat numberFormat1 = new DecimalFormat("#0.000E00");
        builder.append(String.format(format, "Svid", measurement.getSvid()));
        builder.append(String.format(format, "ConstellationType", measurement.getConstellationType()));
        builder.append(String.format(format, "TimeOffsetNanos", measurement.getTimeOffsetNanos()));

        builder.append(String.format(format, "State", measurement.getState()));

        builder.append(String.format(format, "ReceivedSvTimeNanos", measurement.getReceivedSvTimeNanos()));
        builder.append(String.format(format, "ReceivedSvTimeUncertaintyNanos",measurement.getReceivedSvTimeUncertaintyNanos()));
        builder.append(String.format(format, "Cn0DbHz", numberFormat.format(measurement.getCn0DbHz())));

        builder.append(String.format(format,"PseudorangeRateMetersPerSecond",numberFormat.format(measurement.getPseudorangeRateMetersPerSecond())));
        builder.append(String.format(format,"PseudorangeRateUncertaintyMetersPerSeconds", numberFormat.format(measurement.getPseudorangeRateUncertaintyMetersPerSecond())));

        if (measurement.getAccumulatedDeltaRangeState() != 0) { // !=GnssMeasurement.ADR_STATE_UNKNOWN
            builder.append(String.format(format, "AccumulatedDeltaRangeState", measurement.getAccumulatedDeltaRangeState()));
            builder.append(String.format(format,"AccumulatedDeltaRangeMeters",numberFormat.format(measurement.getAccumulatedDeltaRangeMeters())));
            builder.append(String.format(format,"AccumulatedDeltaRangeUncertaintyMeters", numberFormat1.format(measurement.getAccumulatedDeltaRangeUncertaintyMeters())));
        }

        if (measurement.hasCarrierFrequencyHz()) {
            builder.append(String.format(format, "CarrierFrequencyHz", measurement.getCarrierFrequencyHz()));
        }

        if (measurement.hasCarrierCycles()) {
            builder.append(String.format(format, "CarrierCycles", measurement.getCarrierCycles()));
        }

        if (measurement.hasCarrierPhase()) {
            builder.append(String.format(format, "CarrierPhase", measurement.getCarrierPhase()));
        }

        if (measurement.hasCarrierPhaseUncertainty()) {
            builder.append(String.format(format, "CarrierPhaseUncertainty", measurement.getCarrierPhaseUncertainty()));
        }

        builder.append(String.format(format, "MultipathIndicator", measurement.getMultipathIndicator()));

        if (measurement.hasSnrInDb()) {
            builder.append(String.format(format, "SnrInDb", measurement.getSnrInDb()));
        }

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            if (measurement.hasAutomaticGainControlLevelDb()) {
                builder.append(String.format(format, "AgcDb", measurement.getAutomaticGainControlLevelDb()));
            }
            if (measurement.hasCarrierFrequencyHz()) {
                builder.append(String.format(format, "CarrierFreqHz", measurement.getCarrierFrequencyHz()));
            }
        }

        return builder.toString();
    } // private String toStringMeasurement

    private void logMeasurementEvent(String event) { logEvent("Measurement", event, USED_COLOR);   }

    private void logStatusEvent(String event) { logEvent("Status", event, USED_COLOR);   }
    private String gnssStatusToString(GnssStatus gnssStatus) {
        StringBuilder builder = new StringBuilder("SATELLITE_STATUS | [Satellites:\n");
        for (int i = 0; i < gnssStatus.getSatelliteCount(); i++) {
            builder.append("Constellation = ").append(getConstellationName(gnssStatus.getConstellationType(i))).append(", ");
            builder.append("Svid = ").append(gnssStatus.getSvid(i)).append(", ");
            builder.append("Cn0DbHz = ").append(gnssStatus.getCn0DbHz(i)).append(", ");
            builder.append("Elevation = ").append(gnssStatus.getElevationDegrees(i)).append(", ");
            builder.append("Azimuth = ").append(gnssStatus.getAzimuthDegrees(i)).append(", ");
            builder.append("hasEphemeris = ").append(gnssStatus.hasEphemerisData(i)).append(", ");
            builder.append("hasAlmanac = ").append(gnssStatus.hasAlmanacData(i)).append(", ");
            builder.append("usedInFix = ").append(gnssStatus.usedInFix(i)).append("\n");
        }
        builder.append("]");
        return builder.toString();
    } // private String gnssStatusToString


    @Override
    public void onGnssMeasurementsReceived(GnssMeasurementsEvent event) {
        StringBuilder builder = new StringBuilder("[ GnssMeasurementsEvent:\n\n");
        builder.append(toStringClock(event.getClock()));
        builder.append("\n");

        for (GnssMeasurement measurement : event.getMeasurements()) {
            builder.append(toStringMeasurement(measurement));
            builder.append("\n");
        }

        builder.append("]");
        logMeasurementEvent("onGnssMeasurementsReceived: " + builder.toString());
    }

    @Override
    public void onGnssMeasurementsStatusChanged(int status) {    }

    @Override
    public void onSensorChanged(SensorEvent event) {
//       Log.d("********************Uiloggger Sensors", "SensorStream****************************");
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    private String getConstellationName(int id) {
        switch (id) {
            case 1:
                return "GPS";
            case 2:
                return "SBAS";
            case 3:
                return "GLONASS";
            case 4:
                return "QZSS";
            case 5:
                return "BEIDOU";
            case 6:
                return "GALILEO";
            default:
                return "UNKNOWN";
        }
    } // private String getConstellationName(int id)
}
