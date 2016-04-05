package com.example.nitin.sensortagandroid;

import android.app.Service;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;

public class SensorTagService extends Service {

    private static final String TAG = SensorTagService.class.getSimpleName();

    public static final byte[] SERVICE_ENABLED = {0x01};
    public static final byte[] SERVICE_DISABLED = {0x00};

    public static final byte[] SERVICE_GYROSCOPE_ALL = {0x07};

    // Bluetooth LE stuff.
    private BluetoothGatt mBluetoothGatt;
    private int mConnectionState = STATE_DISCONNECTED;
    private static final int STATE_DISCONNECTED = 0;
    private static final int STATE_CONNECTING = 1;
    private static final int STATE_CONNECTED = 2;

    public final static String ACTION_GATT_CONNECTED =
            "com.example.bluetooth.le.ACTION_GATT_CONNECTED";
    public final static String ACTION_GATT_DISCONNECTED =
            "com.example.bluetooth.le.ACTION_GATT_DISCONNECTED";
    public final static String ACTION_GATT_SERVICES_DISCOVERED =
            "com.example.bluetooth.le.ACTION_GATT_SERVICES_DISCOVERED";
    public final static String ACTION_IR_TEMPERATURE_DATA_AVAILABLE =
            "com.example.bluetooth.le.ACTION_IR_TEMPERATURE_DATA_AVAILABLE";
    public final static String ACTION_SIMPLE_KEYS_DATA_AVAILABLE =
            "com.example.bluetooth.le.ACTION_SIMPLE_KEYS_DATA_AVAILABLE";
    public final static String ACTION_LIGHT_DATA_AVAILABLE =
            "com.example.bluetooth.le.ACTION_IR_TEMPERATURE_DATA_AVAILABLE";


    public final static String DATA_IR_TEMPERATURE_AMBIENT = "com.example.bluetooth.le.DATA_IR_TEMPERATURE_AMBIENT";
    public final static String DATA_IR_TEMPERATURE_TARGET = "com.example.bluetooth.le.DATA_IR_TEMPERATURE_TARGET";
    public final static String DATA_LIGHT = "com.example.bluetooth.le.DATA_LIGHT";

    public final static UUID UUID_SERVICE_GAP = UUID.fromString("F0001800-0451-4000-B000-000000000000");
    public final static UUID UUID_SERVICE_GATT = UUID.fromString("F0001801-0451-4000-B000-000000000000");
    public final static UUID UUID_SERVICE_DEVICE_INFORMATION = UUID.fromString("F000180A-0451-4000-B000-000000000000");
    public final static UUID UUID_SERVICE_IR_TEMPERATURE = UUID.fromString("F000AA00-0451-4000-B000-000000000000");
    public final static UUID UUID_SERVICE_SIMPLE_KEYS = UUID.fromString("F000FFE0-0451-4000-B000-000000000000");
    public final static UUID UUID_SERVICE_TEST = UUID.fromString("F000AA60-0451-4000-B000-000000000000");

    public final static UUID UUID_DESCRIPTOR_NOTIFICATION_CFG = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

    public final static UUID UUID_CHAR_IR_TEMPERATURE_CONF = UUID.fromString("F000AA02-0451-4000-B000-000000000000");
    public final static UUID UUID_CHAR_IR_TEMPERATURE_DATA = UUID.fromString("F000AA01-0451-4000-B000-000000000000");

    public final static UUID UUID_CHAR_SIMPLE_KEYS_CONF = UUID.fromString("F000FFE2-0451-4000-B000-000000000000");
    public final static UUID UUID_CHAR_SIMPLE_KEYS_DATA = UUID.fromString("F000FFE1-0451-4000-B000-000000000000");
    public final static UUID LUX_SERV = UUID.fromString("f000aa70-0451-4000-b000-000000000000");
    public final static UUID LUX_DATA = UUID.fromString("f000aa71-0451-4000-b000-000000000000");
    public final static UUID LUX_CON = UUID.fromString("f000aa72-0451-4000-b000-000000000000"); // 0: disable, 1: enable
    public final static UUID LUX_PERI = UUID.fromString("f000aa73-0451-4000-b000-000000000000");

    // Implement a queue of services to subscribe to.
    ArrayBlockingQueue<BluetoothGattService> mServiceToSubscribe = new ArrayBlockingQueue<BluetoothGattService>(12);

    /**
     * Class for clients to access.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with
     * IPC.
     */
    public class LocalBinder extends Binder {
        public SensorTagService getService() {
            return SensorTagService.this;
        }
    }

    // This is the object that receives interactions from clients.  See
    // RemoteService for a more complete example.
    private final IBinder mBinder = new LocalBinder();

    private void processService() {
        if (!mServiceToSubscribe.isEmpty()) {
            BluetoothGattService service = null;
            try {
                service = mServiceToSubscribe.take();
                UUID uuid = service.getUuid();
                if (uuid.equals(UUID_SERVICE_IR_TEMPERATURE)) {
                    // This is the temperature service.
                    BluetoothGattCharacteristic temperatureDataCharacteristic = service.getCharacteristic(UUID_CHAR_IR_TEMPERATURE_DATA);
                    enableNotificationForService(true, mBluetoothGatt, temperatureDataCharacteristic);
                }else if (uuid.equals(UUID_SERVICE_SIMPLE_KEYS)) {
                    BluetoothGattCharacteristic keyCharacteristic = service.getCharacteristic(UUID_CHAR_SIMPLE_KEYS_DATA);
                    enableNotificationForService(true, mBluetoothGatt, keyCharacteristic);
                }
                else if (uuid.equals(LUX_SERV)) {
                    BluetoothGattCharacteristic lightCharacteristic = service.getCharacteristic(LUX_DATA);
                    enableNotificationForService(true, mBluetoothGatt, lightCharacteristic);
                }

            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    // Various callback methods defined by the BLE API.
    private final BluetoothGattCallback mGattCallback =
            new BluetoothGattCallback() {
                @Override
                public void onConnectionStateChange(BluetoothGatt gatt, int status,
                                                    int newState) {
                    String intentAction;
                    if (newState == BluetoothProfile.STATE_CONNECTED) {
                        intentAction = ACTION_GATT_CONNECTED;
                        mConnectionState = STATE_CONNECTED;
                        broadcastUpdate(intentAction);
                        Log.i(TAG, "Connected to GATT server.");
                        Log.i(TAG, "Attempting to start service discovery:" +
                                mBluetoothGatt.discoverServices());
                    } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                        intentAction = ACTION_GATT_DISCONNECTED;
                        mConnectionState = STATE_DISCONNECTED;
                        Log.i(TAG, "Disconnected from GATT server.");
                        broadcastUpdate(intentAction);
                    }
                }

                @Override
                // New services discovered
                public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                    if (status == BluetoothGatt.GATT_SUCCESS) {
                        // Set notifications on this service.
                        List<BluetoothGattService> services = gatt.getServices();
                        for (BluetoothGattService service : services) {
                            UUID uuid = service.getUuid();
                            try {
                                if (uuid.equals(UUID_SERVICE_IR_TEMPERATURE)) {
                                    mServiceToSubscribe.put(service);
                                } else if (uuid.equals(LUX_SERV)){
                                    mServiceToSubscribe.put(service);
                                }
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }

                        }
                        broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED);
                        processService();
                    } else {
                        Log.w(TAG, "onServicesDiscovered received: " + status);
                    }
                }

                @Override
                public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
                    BluetoothGattCharacteristic characteristic = descriptor.getCharacteristic();
                    UUID characteristicUUID = characteristic.getUuid();
                    UUID serviceCharToEnable = null;
                    byte[] configValue = null;
                    if (characteristicUUID.equals(UUID_CHAR_IR_TEMPERATURE_DATA)) {
                        serviceCharToEnable = UUID_CHAR_IR_TEMPERATURE_CONF;
                    }
                    if (characteristicUUID.equals(LUX_DATA)) {
                        serviceCharToEnable = LUX_CON;
                    }
                    if (serviceCharToEnable != null) {
                        // Time to turn on the sensor.
                        enableService(true, gatt, characteristic.getService(), serviceCharToEnable, configValue);
                    }
                    super.onDescriptorWrite(gatt, descriptor, status);
                }

                @Override
                public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                    super.onCharacteristicWrite(gatt, characteristic, status);
                    boolean serviceEnabled = false;
                    BluetoothGattService service = characteristic.getService();
                    if (characteristic.getUuid().equals(UUID_CHAR_IR_TEMPERATURE_CONF)
                            || characteristic.getUuid().equals(LUX_CON)) {
                        // A service has changed.
                        processService();
                    }
                }

                @Override
                // Characteristic notification
                public void onCharacteristicChanged(BluetoothGatt gatt,
                                                    BluetoothGattCharacteristic characteristic) {
                    UUID characteristicUUID = characteristic.getUuid();
                    if (characteristicUUID.equals(UUID_CHAR_IR_TEMPERATURE_DATA)) {
                        final Intent intent = new Intent(ACTION_IR_TEMPERATURE_DATA_AVAILABLE);
                        /* The IR Temperature sensor produces two measurements;
                         * Object ( AKA target or IR) Temperature,
                         * and Ambient ( AKA die ) temperature.
                         *
                         * Both need some conversion, and Object temperature is dependent on Ambient temperature.
                         *
                         * They are stored as [ObjLSB, ObjMSB, AmbLSB, AmbMSB] (4 bytes)
                         * Which means we need to shift the bytes around to get the correct values.
                         */
                        double ambient = extractAmbientTemperature(characteristic);
                        double target = extractTargetTemperature(characteristic, ambient);

                        intent.putExtra(DATA_IR_TEMPERATURE_AMBIENT, ambient);
                        intent.putExtra(DATA_IR_TEMPERATURE_TARGET, target);
                        sendBroadcast(intent);
                    }else if (characteristicUUID.equals(LUX_DATA)) {
                        final Intent intent = new Intent(ACTION_LIGHT_DATA_AVAILABLE);
                        double light = convert(characteristic);
                        intent.putExtra(DATA_LIGHT, light);
                        sendBroadcast(intent);
                    }
                }
            };

    /**
     * Gyroscope, Magnetometer, Barometer, IR temperature
     * all store 16 bit two's complement values in the awkward format
     * LSB MSB, which cannot be directly parsed as getIntValue(FORMAT_SINT16, offset)
     * because the bytes are stored in the "wrong" direction.
     * <p/>
     * This function extracts these 16 bit two's complement values.
     */
    private static Integer shortSignedAtOffset(BluetoothGattCharacteristic c, int offset) {
        Integer lowerByte = c.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, offset);
        Integer upperByte = c.getIntValue(BluetoothGattCharacteristic.FORMAT_SINT8, offset + 1); // Note: interpret MSB as signed.

        return (upperByte << 8) + lowerByte;
    }

    private static Integer shortUnsignedAtOffset(BluetoothGattCharacteristic c, int offset) {
        Integer lowerByte = c.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, offset);
        Integer upperByte = c.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, offset + 1); // Note: interpret MSB as unsigned.

        return (upperByte << 8) + lowerByte;
    }


    /////////////////////////////////
    // IR Temperature stuff.
    private double extractAmbientTemperature(BluetoothGattCharacteristic c) {
        int offset = 2;
        return shortUnsignedAtOffset(c, offset) / 128.0;
    }

    private double extractTargetTemperature(BluetoothGattCharacteristic c, double ambient) {
        Integer twoByteValue = shortSignedAtOffset(c, 0);

        double Vobj2 = twoByteValue.doubleValue();
        Vobj2 *= 0.00000015625;

        double Tdie = ambient + 273.15;

        double S0 = 5.593E-14;    // Calibration factor
        double a1 = 1.75E-3;
        double a2 = -1.678E-5;
        double b0 = -2.94E-5;
        double b1 = -5.7E-7;
        double b2 = 4.63E-9;
        double c2 = 13.4;
        double Tref = 298.15;
        double S = S0 * (1 + a1 * (Tdie - Tref) + a2 * Math.pow((Tdie - Tref), 2));
        double Vos = b0 + b1 * (Tdie - Tref) + b2 * Math.pow((Tdie - Tref), 2);
        double fObj = (Vobj2 - Vos) + c2 * Math.pow((Vobj2 - Vos), 2);
        double tObj = Math.pow(Math.pow(Tdie, 4) + (fObj / S), .25);

        return tObj - 273.15;
    }
    public double convert(BluetoothGattCharacteristic value) {
        int mantissa;
        int exponent;
        Integer sfloat= shortUnsignedAtOffset(value, 0);
        mantissa = sfloat & 0x0FFF;
        exponent = (sfloat >> 12) & 0xFF;
        double output;
        double magnitude = Math.pow(2.0f, exponent);
        output = (mantissa * magnitude);
        return output / 100.0f;
    }

    /**
     * Enables/disables service.
     *
     * @param enable
     * @param gatt
     * @param service
     * @param confCharacteristicUUID
     */
    private void enableService(boolean enable, BluetoothGatt gatt, BluetoothGattService service, UUID confCharacteristicUUID, byte[] configValue) {
        BluetoothGattCharacteristic confCharacteristic = service.getCharacteristic(confCharacteristicUUID);

        if (configValue == null) {
            configValue = enable ? SERVICE_ENABLED : SERVICE_DISABLED;
        }

        // Just set the value on the characteristic.
        confCharacteristic.setValue(configValue);
        mBluetoothGatt.writeCharacteristic(confCharacteristic);
    }

    /**
     * Enables/disables notifications for a service.
     *
     * @param enable
     * @param gatt
     * @param dataCharacteristic
     */
    private void enableNotificationForService(boolean enable, BluetoothGatt gatt, BluetoothGattCharacteristic dataCharacteristic) {
        gatt.setCharacteristicNotification(dataCharacteristic, enable);
        BluetoothGattDescriptor descriptor = dataCharacteristic.getDescriptor(UUID_DESCRIPTOR_NOTIFICATION_CFG);

        descriptor.setValue(enable ? BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE : BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);
        mBluetoothGatt.writeDescriptor(descriptor);
    }

    private void broadcastUpdate(final String action) {
        final Intent intent = new Intent(action);
        sendBroadcast(intent);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    public void connectLeDevice(BluetoothDevice device) {
        mBluetoothGatt = device.connectGatt(this, false, mGattCallback);
    }
}
