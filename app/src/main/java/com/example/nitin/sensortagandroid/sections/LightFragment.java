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

public class LightFragment extends com.example.nitin.sensortagandroid.sections.SectionFragment {
    private TextView mLightValue;
    private LinearLayout mLightDetails;
    private LightFragment.LightReceiver mReceiver;


    public class LightReceiver extends BroadcastReceiver {
        public LightReceiver() {
            // Android needs the empty constructor.
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (SensorTagService.ACTION_GATT_CONNECTED.equals(action)) {
                setConnected(true);
            } else if (SensorTagService.ACTION_GATT_DISCONNECTED.equals(action)) {
                setConnected(false);
            } else if (SensorTagService.ACTION_LIGHT_DATA_AVAILABLE.equals(action)) {
                setConnected(true);
                double light = intent.getDoubleExtra(SensorTagService.DATA_LIGHT,0);
                mLightValue.setText(Double.toString(light));

            }
        }
    };

    @Override
    protected int getLayoutResource() {
        return R.layout.fragment_sensor_tag_luxometer;
    }

    @Override
    protected void onCreateViewHook(View rootView) {
        mLightValue = (TextView) rootView.findViewById(R.id.light_details_ambient);
        mLightDetails = (LinearLayout) rootView.findViewById(R.id.light_details);
    }

    @Override
    protected LinearLayout getSectionLayout() {
        return mLightDetails;
    }

    @Override
    protected void registerSectionReceiver(IntentFilter filter) {
        filter.addAction(SensorTagService.ACTION_LIGHT_DATA_AVAILABLE);
        mReceiver = new LightFragment.LightReceiver();
        getActivity().registerReceiver(mReceiver, filter);
    }

    @Override
    protected void unregisterSectionReceiver() {
        getActivity().unregisterReceiver(mReceiver);
    }
}