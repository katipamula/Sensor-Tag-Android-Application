package com.example.nitin.sensortagandroid;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import org.amei.sensortagandroid.R;

public class ConfigurationDialogFragment extends DialogFragment {
    private BluetoothAdapter mBluetoothAdapter;
    private OnLeConnectionRequestHandler mCallback;

    public interface OnLeConnectionRequestHandler {
        public void onLeDeviceConnectionRequest(BluetoothDevice device);
    }

    public ConfigurationDialogFragment() {
        // Empty constructor required for DialogFragment
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        try {
            mCallback = (OnLeConnectionRequestHandler) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement OnLeConnectionRequestHandler.");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Initializes Bluetooth adapter.
        final BluetoothManager bluetoothManager =
                (BluetoothManager) getActivity().getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        View view = inflater.inflate(R.layout.fragment_dialog_configuration, container);
        getDialog().setTitle(getString(R.string.dialog_title_configuration));
        ((Button) view.findViewById(R.id.button_scan)).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // When button is clicked, call up to owning activity.
                String add = "B0:B4:48:C0:CA:03";
                BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(add);
                mCallback.onLeDeviceConnectionRequest(device);
                dismiss();
            }
        });
        return view;
    }
}
