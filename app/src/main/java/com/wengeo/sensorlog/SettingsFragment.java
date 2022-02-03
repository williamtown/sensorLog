package com.wengeo.sensorlog;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class SettingsFragment extends Fragment {

    /** Key in the {@link SharedPreferences} indicating whether auto-scroll has been enabled */
    protected static String PREFERENCE_KEY_AUTO_SCROLL =  "autoScroll";

    private MeasurementProvider mGpsContainer;
    public void setGpsContainer(MeasurementProvider value) { mGpsContainer = value;  }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        final View view=inflater.inflate(R.layout.fragment_main,container,false);

        final Switch registerLocation = (Switch) view.findViewById(R.id.register_location);
        final TextView registerLocationLabel = (TextView) view.findViewById(R.id.register_location_label);
        //set the switch to OFF
        registerLocation.setChecked(false);
        registerLocationLabel.setText("Switch is OFF");
        registerLocation.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        if (isChecked) {
                            mGpsContainer.registerLocation();
                            registerLocationLabel.setText("Switch is ON");
                        } else {
                            mGpsContainer.unregisterLocation();
                            registerLocationLabel.setText("Switch is OFF");
                        }
                    }
                });

        final Switch registerMeasurements = (Switch) view.findViewById(R.id.register_measurements);
        final TextView registerMeasurementsLabel = (TextView) view.findViewById(R.id.register_measurement_label);
        registerMeasurements.setChecked(false);
        registerMeasurementsLabel.setText("Switch is OFF");
        registerMeasurements.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        if (isChecked) {
                            mGpsContainer.registerMeasurements();
                            registerMeasurementsLabel.setText("Switch is ON");
                        } else {
                            mGpsContainer.unregisterMeasurements();
                            registerMeasurementsLabel.setText("Switch is OFF");
                        }
                    }
                });

        final Switch registerSensors = (Switch) view.findViewById(R.id.register_sensors);
        final TextView registerSensorsLabel = (TextView) view.findViewById(R.id.register_sensor_label);
        registerSensors.setChecked(false);
        registerSensorsLabel.setText("Switch is OFF");
        registerSensors.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    mGpsContainer.registerSensors();
                    registerSensorsLabel.setText("Switch is ON");
                } else {
                    mGpsContainer.unregisterSensors();
                    registerSensorsLabel.setText("Switch is OFF");
                }
            }
        });


        return view;
    }
}
