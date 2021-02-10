package com.example.avia;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class AdvertiserService extends Service {
    private static final String TAG = AdvertiserService.class.getSimpleName();
    private BluetoothLeAdvertiser mBluetoothLeAdvertiser;
    private BluetoothGattServer mBluetoothGattServer;
    private  BluetoothManager mBluetoothManager;

    public static boolean running = false;
    private AdvertiseCallback mAdvertiseCallback;
    private Handler mHandler;
    private Runnable timeoutRunnable;
    private Set<BluetoothDevice> mRegisteredDevices = new HashSet<>();

    private long TIMEOUT = TimeUnit.MILLISECONDS.convert(10, TimeUnit.MINUTES);

    @Override
    public void onCreate() {
        running = true;
        initialize();
        startAdvertising();
        setTimeout();
        super.onCreate();
    }

    @Override
    public void onDestroy() {
        /**
         * Note that onDestroy is not guaranteed to be called quickly or at all. Services exist at
         * the whim of the system, and onDestroy can be delayed or skipped entirely if memory need
         * is critical.
         */
        running = false;
        stopAdvertising();

        mHandler.removeCallbacks(timeoutRunnable);
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    /**
     * Get references to system Bluetooth objects if we don't have them already.
     */
    private void initialize() {
        if (mBluetoothLeAdvertiser == null) {
            mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            if (mBluetoothManager != null) {
                BluetoothAdapter mBluetoothAdapter = mBluetoothManager.getAdapter();
                if (mBluetoothAdapter != null) {
                    mBluetoothLeAdvertiser = mBluetoothAdapter.getBluetoothLeAdvertiser();
                    BluetoothAdapter.getDefaultAdapter().setName(getString(R.string.Ble_Adv_Local_Name1));
                } else {
                    Toast.makeText(this, getString(R.string.bt_null), Toast.LENGTH_LONG).show();
                }
            } else {
                Toast.makeText(this, getString(R.string.bt_null), Toast.LENGTH_LONG).show();
            }
        }
    }

    /**
     * Starts a delayed Runnable that will cause the BLE Advertising to timeout and stop after a
     * set amount of time.
     */
    private void setTimeout(){
        mHandler = new Handler();
        timeoutRunnable = new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "AdvertiserService has reached timeout of "+TIMEOUT+" milliseconds, stopping advertising.");
                running = false;
                sendFailureIntent(Constants.ADVERTISING_TIMED_OUT);
                stopSelf();
            }
        };
        mHandler.postDelayed(timeoutRunnable, Constants.ADVERTISING_TIMED_OUT);
    }

    /**
     * Starts BLE Advertising.
     */
    private void startAdvertising() {
        // goForeground();

        Log.d(TAG, "Service: Starting Advertising");

        if (mAdvertiseCallback == null) {
            AdvertiseSettings settings = buildAdvertiseSettings();
            AdvertiseData data = buildAdvertiseData();
            AdvertiseData scanResponse = buildScanResponse();
            mAdvertiseCallback = new FobAdvertiseCallback();

            if (mBluetoothLeAdvertiser != null) {
                //mBluetoothLeAdvertiser.startAdvertising(settings, data, mAdvertiseCallback);
                mBluetoothLeAdvertiser.startAdvertising(settings,data, scanResponse,mAdvertiseCallback);
            }
        }
    }


    /**
     * Stops BLE Advertising.
     */
    private void stopAdvertising() {
        Log.d(TAG, "Service: Stopping Advertising");
        stopGattServer();
        if (mBluetoothLeAdvertiser != null) {
            running = false;
            mBluetoothLeAdvertiser.stopAdvertising(mAdvertiseCallback);
            mAdvertiseCallback = null;
        }
    }

    /**
     * Start Gatt server.
     */
    private void  startGattServer() {
        mBluetoothGattServer = mBluetoothManager.openGattServer(this, new BluetoothGattServerCallback() {
            @Override
            public void onConnectionStateChange(BluetoothDevice device, int status, int newState) {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    Log.i(TAG, "BluetoothDevice CONNECTED: " + device);
                    mHandler.removeCallbacks(timeoutRunnable);
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    Log.i(TAG, "BluetoothDevice DISCONNECTED: " + device);
                    //Remove device from any active subscriptions
                    mRegisteredDevices.remove(device);
                }
                super.onConnectionStateChange(device, status, newState);
            }

            @Override
            public void onCharacteristicReadRequest(BluetoothDevice device, int requestId, int offset,
                                                    BluetoothGattCharacteristic characteristic) {
                if (FobService.buttonCharaUUID.equals(characteristic.getUuid())) {
                    Log.i(TAG, "Read button chara");
                } else if (FobService.ledCharaUUID.equals(characteristic.getUuid())) {
                    Log.i(TAG, "Read led chara");
                } else if (FobService.charaUUID1.equals(characteristic.getUuid())) {
                    Log.i(TAG, "Read chara uuid 1");
                } else if (FobService.charaUUID2.equals(characteristic.getUuid())) {
                    Log.i(TAG, "Read chara uuid 2");
                } else {
                    // Invalid characteristic
                    Log.w(TAG, "Invalid Characteristic Read: " + characteristic.getUuid());
                    mBluetoothGattServer.sendResponse(device,
                            requestId,
                            BluetoothGatt.GATT_FAILURE,
                            0,
                            null);
                }
                byte[] data = {0x02};
                mBluetoothGattServer.sendResponse(device,requestId,BluetoothGatt.GATT_SUCCESS,offset,data);
                super.onCharacteristicReadRequest(device,requestId,offset,characteristic);
            }

            @Override
            public void onCharacteristicWriteRequest(BluetoothDevice device, int requestId, BluetoothGattCharacteristic characteristic, boolean preparedWrite, boolean responseNeeded, int offset, byte[] value) {
                if (FobService.buttonCharaUUID.equals(characteristic.getUuid())) {
                    Log.i(TAG, "Write button chara");
                } else if (FobService.ledCharaUUID.equals(characteristic.getUuid())) {
                    Log.i(TAG, "Write led chara");
                } else if (FobService.charaUUID1.equals(characteristic.getUuid())) {
                    Log.i(TAG, "Write chara uuid 1");
                } else if (FobService.charaUUID2.equals(characteristic.getUuid())) {
                    Log.i(TAG, "Write chara uuid 2");
                } else {
                    // Invalid characteristic
                    Log.w(TAG, "Invalid Characteristic Read: " + characteristic.getUuid());
                    mBluetoothGattServer.sendResponse(device,
                            requestId,
                            BluetoothGatt.GATT_FAILURE,
                            0,
                            null);
                }
                byte[] data = {0x01,0x02};
                mBluetoothGattServer.sendResponse(device,requestId,BluetoothGatt.GATT_SUCCESS,offset,data);
                super.onCharacteristicWriteRequest(device, requestId, characteristic, preparedWrite, responseNeeded, offset, value);
            }

            @Override
            public void onNotificationSent(BluetoothDevice device, int status) {
                Log.i(TAG, "Got notification");

                super.onNotificationSent(device, status);
            }

            @Override
            public void onDescriptorReadRequest(BluetoothDevice device, int requestId, int offset,
                                                BluetoothGattDescriptor descriptor) {
                if (FobService.CLIENT_CONFIG.equals(descriptor.getUuid())) {
                    Log.d(TAG, "Config descriptor read");
                    byte[] returnValue;
                    if (mRegisteredDevices.contains(device)) {
                        returnValue = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE;
                    } else {
                        returnValue = BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE;
                    }
                    mBluetoothGattServer.sendResponse(device,
                            requestId,
                            BluetoothGatt.GATT_FAILURE,
                            0,
                            returnValue);
                } else {
                    Log.w(TAG, "Unknown descriptor read request");
                    mBluetoothGattServer.sendResponse(device,
                            requestId,
                            BluetoothGatt.GATT_FAILURE,
                            0,
                            null);
                }
                byte[] data = {0x01,0x02};
                mBluetoothGattServer.sendResponse(device,requestId,BluetoothGatt.GATT_SUCCESS,offset,data);

                super.onDescriptorReadRequest(device, requestId, offset, descriptor);
            }

            @Override
            public void onDescriptorWriteRequest(BluetoothDevice device, int requestId,
                                                 BluetoothGattDescriptor descriptor,
                                                 boolean preparedWrite, boolean responseNeeded,
                                                 int offset, byte[] value) {
                if (FobService.CLIENT_CONFIG.equals(descriptor.getUuid())) {
                    if (Arrays.equals(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE, value)) {
                        Log.d(TAG, "Subscribe device to notifications: " + device);
                        mRegisteredDevices.add(device);
                    } else if (Arrays.equals(BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE, value)) {
                        Log.d(TAG, "Unsubscribe device from notifications: " + device);
                        mRegisteredDevices.remove(device);
                    }

                    if (responseNeeded) {
                        mBluetoothGattServer.sendResponse(device,
                                requestId,
                                BluetoothGatt.GATT_SUCCESS,
                                0,
                                null);
                    }
                } else {
                    Log.w(TAG, "Unknown descriptor write request");
                    if (responseNeeded) {
                        mBluetoothGattServer.sendResponse(device,
                                requestId,
                                BluetoothGatt.GATT_FAILURE,
                                0,
                                null);
                    }
                }
                super.onDescriptorWriteRequest(device, requestId, descriptor, preparedWrite,responseNeeded, offset, value);
            }
        });

        mBluetoothGattServer.addService(FobService.createFobService());
    }

    /**
     * Stop Gatt server.
     */
    private void  stopGattServer() {
        if (mBluetoothGattServer == null) return;

        mBluetoothGattServer.close();
    }

    /**
     * Returns an AdvertiseData object which includes the Service UUID and Device Name.
     */
    private AdvertiseData buildAdvertiseData() {

        /**
         * Note: There is a strict limit of 31 Bytes on packets sent over BLE Advertisements.
         *  This includes everything put into AdvertiseData including UUIDs, device info, &
         *  arbitrary service or manufacturer data.
         *  Attempting to send packets over this limit will result in a failure with error code
         *  AdvertiseCallback.ADVERTISE_FAILED_DATA_TOO_LARGE. Catch this error in the
         *  onStartFailure() method of an AdvertiseCallback implementation.
         */

        AdvertiseData.Builder dataBuilder = new AdvertiseData.Builder();
        dataBuilder.setIncludeDeviceName(true);

        /* For example - this will cause advertising to fail (exceeds size limit) */
        //String failureData = "asdghkajsghalkxcjhfa;sghtalksjcfhalskfjhasldkjfhdskf";
        //dataBuilder.addServiceData(Constants.Service_UUID, failureData.getBytes());

        return dataBuilder.build();
    }

    /**
     * Returns an AdvertiseSettings object set to use low power (to help preserve battery life)
     * and disable the built-in timeout since this code uses its own timeout runnable.
     */
    private AdvertiseSettings buildAdvertiseSettings() {
        AdvertiseSettings.Builder settingsBuilder = new AdvertiseSettings.Builder();
        settingsBuilder.setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_LOW);
        settingsBuilder.setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY);
        settingsBuilder.setTimeout(0);
        return settingsBuilder.build();
    }

    /**
     * Returns an AdvertiseData object as scan response with service uuid.
     */
    private AdvertiseData buildScanResponse() {
        AdvertiseData.Builder scanResponse = new AdvertiseData.Builder();
        scanResponse.addServiceUuid(Constants.Service_UUID);
        return scanResponse.build();
    }

    /**
     * Builds and sends a broadcast intent indicating Advertising has failed. Includes the error
     * code as an extra. This is intended to be picked up by the {@code AdvertiserFragment}.
     */
    private void sendFailureIntent(int errorCode){
        Intent failureIntent = new Intent();
        failureIntent.setAction(Constants.ADVERTISING_FAILED);
        failureIntent.putExtra(Constants.ADVERTISING_FAILED_EXTRA_CODE, errorCode);
        sendBroadcast(failureIntent);
    }

    /**
     * Custom callback after Advertising succeeds or fails to start. Broadcasts the error code
     * in an Intent to be picked up by AdvertiserFragment and stops this Service.
     */
    private class FobAdvertiseCallback extends AdvertiseCallback {

        @Override
        public void onStartFailure(int errorCode) {
            super.onStartFailure(errorCode);
            Log.d(TAG, "Advertising failed");
            sendFailureIntent(errorCode);
            stopSelf();
        }

        @Override
        public void onStartSuccess(AdvertiseSettings settingsInEffect) {
            super.onStartSuccess(settingsInEffect);
            startGattServer();
            Log.d(TAG, "Advertising successfully started");
        }
    }
}
