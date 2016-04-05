package com.example.nitin.sensortagandroid.sections;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.nitin.sensortagandroid.SensorTagService;
import org.amei.sensortagandroid.R;

public class TemperatureFragment extends com.example.nitin.sensortagandroid.sections.SectionFragment {
    private TextView mAmbientValueTextView;
    private LinearLayout mTemperatureDetails;
    private TemperatureFragment.TemperatureReceiver mReceiver;

    public class TemperatureReceiver extends BroadcastReceiver {
        public TemperatureReceiver() {
            // Android needs the empty constructor.
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (SensorTagService.ACTION_GATT_CONNECTED.equals(action)) {
                setConnected(true);
            } else if (SensorTagService.ACTION_GATT_DISCONNECTED.equals(action)) {
                setConnected(false);
            } else if (SensorTagService.ACTION_IR_TEMPERATURE_DATA_AVAILABLE.equals(action)) {
                setConnected(true);
                double ambientTemperature = intent.getDoubleExtra(SensorTagService.DATA_IR_TEMPERATURE_AMBIENT, 0);

                mAmbientValueTextView.setText(Double.toString(ambientTemperature));

            }
        }
    };

    @Override
    protected int getLayoutResource() {
        return R.layout.fragment_sensor_tag_temperature;
    }

    @Override
    protected void onCreateViewHook(View rootView) {

        mAmbientValueTextView = (TextView) rootView.findViewById(R.id.temperature_details_ambient_value);
        mTemperatureDetails = (LinearLayout) rootView.findViewById(R.id.temperature_details);
    }

    @Override
    protected LinearLayout getSectionLayout() {
        return mTemperatureDetails;
    }

    @Override
    protected void registerSectionReceiver(IntentFilter filter) {
        filter.addAction(SensorTagService.ACTION_IR_TEMPERATURE_DATA_AVAILABLE);
        mReceiver = new TemperatureFragment.TemperatureReceiver();
        getActivity().registerReceiver(mReceiver, filter);
    }

    @Override
    protected void unregisterSectionReceiver() {
        getActivity().unregisterReceiver(mReceiver);
    }
}
