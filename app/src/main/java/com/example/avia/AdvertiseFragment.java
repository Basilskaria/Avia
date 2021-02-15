package com.example.avia;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.AdvertiseCallback;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.media.SoundPool;
import android.net.Uri;
import android.os.Bundle;
import android.os.Vibrator;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import java.lang.reflect.Method;
import java.security.PrivateKey;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

public class AdvertiseFragment extends Fragment implements View.OnClickListener {

    private static final String TAG = AdvertiseFragment.class.getSimpleName();

    /**
     * Button for start advertising
     */
    private Button pairButton;

    /**
     * Button for unlocking Lock
     */
    private Button unlockButton;

    /**
     * Button for unpairing
     */
    private Button unPairButton;

    /**
     * ImageView for fob
     */
    private ImageView fobImage;

    /**
     * BluetoothDevice
     */
    private BluetoothDevice pairedDevice;

    /**
     * Haptic touch
     */
    private Vibrator myVib;

    /**
     * BluetoothGattServer
     */
    private BluetoothGattServer mBluetoothGattServer;

    /**
     * BluetoothManager
     */
    private BluetoothManager mBluetoothManager;

    private  Boolean isGattServerInitialised = false;

    private MediaPlayer mediaPlayer;


    /**
     * Listens for notifications that the {@code AdvertiserService} has failed to start advertising.
     * This Receiver deals with Fragment UI elements and only needs to be active when the Fragment
     * is on-screen, so it's defined and registered in code instead of the Manifest.
     */
    private BroadcastReceiver advertisingFailureReceiver;

    private BroadcastReceiver advertisingSuccessReceiver;

    public static final int START_PAIRING = 0;
    public static final int INITIAL_CONNECTION_REQUEST = 1;
    public static final int READ_FWV_REQUEST = 2;
    public static final int PAIR_REQUEST = 3;

    public int fobPairingProcess = START_PAIRING;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        advertisingSuccessReceiver = (BroadcastReceiver)  new BroadcastReceiver() {

            /**
             * Receives Advertising error codes from {@code AdvertiserService} and displays error messages
             * to the user. Sets the advertising toggle to 'false.'
             */
            @Override
            public void onReceive(Context context, Intent intent) {
                Log.i(TAG, "Got success call back");
                startGattServer();
            }
        };

        advertisingFailureReceiver = new BroadcastReceiver() {

            /**
             * Receives Advertising error codes from {@code AdvertiserService} and displays error messages
             * to the user. Sets the advertising toggle to 'false.'
             */
            @Override
            public void onReceive(Context context, Intent intent) {

                int errorCode = intent.getIntExtra(Constants.ADVERTISING_FAILED_EXTRA_CODE, -1);

                String errorMessage = getString(R.string.start_error_prefix);

                switch (errorCode) {
                    case AdvertiseCallback.ADVERTISE_FAILED_ALREADY_STARTED:
                        errorMessage += " " + getString(R.string.start_error_already_started);
                        break;
                    case AdvertiseCallback.ADVERTISE_FAILED_DATA_TOO_LARGE:
                        errorMessage += " " + getString(R.string.start_error_too_large);
                        break;
                    case AdvertiseCallback.ADVERTISE_FAILED_FEATURE_UNSUPPORTED:
                        errorMessage += " " + getString(R.string.start_error_unsupported);
                        break;
                    case AdvertiseCallback.ADVERTISE_FAILED_INTERNAL_ERROR:
                        errorMessage += " " + getString(R.string.start_error_internal);
                        break;
                    case AdvertiseCallback.ADVERTISE_FAILED_TOO_MANY_ADVERTISERS:
                        errorMessage += " " + getString(R.string.start_error_too_many);
                        break;
                    case Constants.ADVERTISING_TIMED_OUT:
                        errorMessage = " " + getString(R.string.advertising_timedout);
                        updateUI(AdvertiserService.running);
                        break;
                    default:
                        errorMessage += " " + getString(R.string.start_error_unknown);
                }

                if (pairedDevice == null) {
                    Toast.makeText(getActivity(), errorMessage, Toast.LENGTH_LONG).show();
                }
            }
        };
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_advertise, container, false);
        pairButton = (Button) view.findViewById(R.id.pairing);
        pairButton.setOnClickListener((View.OnClickListener) this);

        unlockButton = (Button) view.findViewById(R.id.unlock);
        unlockButton.setOnClickListener((View.OnClickListener) this);

        unPairButton = (Button) view.findViewById(R.id.deletePairing);
        unPairButton.setOnClickListener((View.OnClickListener) this);

        fobImage = (ImageView)view.findViewById(R.id.fobImg);

        mediaPlayer = MediaPlayer.create(getContext(), R.raw.notifysound);

        myVib = (Vibrator) getActivity().getSystemService(Context.VIBRATOR_SERVICE);
        return view;
    }

    @Override
    public void onClick(View view) {

        switch (view.getId()) {
            case R.id.pairing:
                if (!AdvertiserService.running) {
                    pairButton.setText("Stop Pairing");
                    fobPairingProcess = START_PAIRING;
                    startAdvertising();
                    startHapticFeedback();
                } else {
                    pairButton.setText("Start Pairing");
                    stopAdvertising();
                }
                break;
            case R.id.unlock:
                Log.i(TAG, "Unlock button");
                unlockDoorLock();
                startHapticFeedback();
                break;
            case R.id.deletePairing:
                showUnpairConfirmation();
                break;
            case R.id.fobImg:
                Log.d(TAG, "Clicked on image view");
                break;

            default:
                break;
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        updateUI(AdvertiserService.running);

        IntentFilter failureFilter = new IntentFilter(Constants.ADVERTISING_FAILED);
        getActivity().registerReceiver(advertisingFailureReceiver, failureFilter);

        IntentFilter successFilter = new IntentFilter(Constants.ADVERTISING_SUCCESS);
        getActivity().registerReceiver(advertisingSuccessReceiver, successFilter);
    }

    @Override
    public void onPause() {
        super.onPause();
        getActivity().unregisterReceiver(advertisingFailureReceiver);
        getActivity().unregisterReceiver(advertisingSuccessReceiver);
        Log.i(TAG, "pause fragment");
    }

    /**
     * Update UI
     */
    private void updateUI(boolean isAdvertising) {

        String savedDeviceName = getDeviceNameFromPreference();
        if (savedDeviceName != "") {
            Set<BluetoothDevice> bondedDevices = getPairedDevices();
            for (BluetoothDevice device: bondedDevices) {
                String deviceName = device.getAddress();
                if (deviceName != null) {
                    if (deviceName.contains(savedDeviceName)) {
                        pairedDevice = device;
                        break;
                    }
                }
            }
        }


        if (pairedDevice  != null ){
            pairButton.setVisibility(View.GONE);
            unlockButton.setVisibility(View.VISIBLE);
            unPairButton.setVisibility(View.VISIBLE);
            Log.i(TAG, "start gatt server to unlock door");
            startGattServer();
        } else  {
            unlockButton.setVisibility(View.GONE);
            unPairButton.setVisibility(View.GONE);
            pairButton.setVisibility(View.VISIBLE);

            if (isAdvertising) {
                pairButton.setText("Stop Pairing");
            } else {
                pairButton.setText("Start Pairing");
            }
        }
    }

    /**
     * Starts BLE Advertising by starting {@code AdvertiserService}.
     */
    private void startAdvertising() {
        Context c = getActivity();
        c.startService(getServiceIntent(c));
    }

    /**
     * Stops BLE Advertising by stopping {@code AdvertiserService}.
     */
    private void stopAdvertising() {
        Context c = getActivity();
        c.stopService(getServiceIntent(c));
        updateUI(false);
    }

    private void startHapticFeedback() {
        myVib.vibrate(50);
    }

    private  void  playNotificationSound() {
        mediaPlayer.start();
    }

    /**
     * Start Gatt server.
     */
    private void  startGattServer() {
        Log.i(TAG, "startGattServer");

        if (isGattServerInitialised == false) {
            isGattServerInitialised = true;

            if (mBluetoothManager == null) {
                mBluetoothManager = (BluetoothManager) getActivity().getSystemService(Context.BLUETOOTH_SERVICE);
            }

            if (mBluetoothGattServer == null) {
                mBluetoothGattServer = mBluetoothManager.openGattServer(getActivity(), new BluetoothGattServerCallback() {
                    @Override
                    public void onConnectionStateChange(BluetoothDevice device, int status, int newState) {
                        if (newState == BluetoothProfile.STATE_CONNECTED) {
                            Log.i(TAG, "BluetoothDevice CONNECTED: " + device);

                            if (fobPairingProcess == START_PAIRING) {
                                fobPairingProcess = INITIAL_CONNECTION_REQUEST;
                            }
                        } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                            Log.i(TAG, "BluetoothDevice DISCONNECTED: " + device);
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
                        } else if (FobService.FWVUUID1.equals(characteristic.getUuid())) {
                            Log.i(TAG, "Read chara FWV UUID");

//                    ByteBuffer b = ByteBuffer.allocate(4);
//                    b.putInt(0x00F424);
                            byte[] data = FobService.firmwareVersion();
                            System.out.println(Arrays.toString(data));

                            if (fobPairingProcess == INITIAL_CONNECTION_REQUEST) {
                                fobPairingProcess = READ_FWV_REQUEST;
                                pairedDevice = device;
                                saveDeviceNameInPreference();

                                startHapticFeedback();
                                playNotificationSound();
                                getActivity().runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        updateUI(false);
                                    }
                                });
                            }
                            mBluetoothGattServer.sendResponse(device,requestId,BluetoothGatt.GATT_SUCCESS,offset,data);
                        } else if (FobService.RebondUUID2.equals(characteristic.getUuid())) {
                            Log.i(TAG, "Read chara REBOUND UUID");
                        } else {
                            // Invalid characteristic
                            Log.w(TAG, "Invalid Characteristic Read: " + characteristic.getUuid());
                            mBluetoothGattServer.sendResponse(device,
                                    requestId,
                                    BluetoothGatt.GATT_FAILURE,
                                    0,
                                    null);
                        }

                        super.onCharacteristicReadRequest(device,requestId,offset,characteristic);
                    }

                    @Override
                    public void onServiceAdded(int status, BluetoothGattService service) {
                        Log.i(TAG, "onServiceAdded");
                        super.onServiceAdded(status, service);
                    }

                    @Override
                    public void onCharacteristicWriteRequest(BluetoothDevice device, int requestId, BluetoothGattCharacteristic characteristic, boolean preparedWrite, boolean responseNeeded, int offset, byte[] value) {
                        Log.i(TAG, "onCharacteristicWriteRequest");

                        if (FobService.buttonCharaUUID.equals(characteristic.getUuid())) {
                            Log.i(TAG, "Write button chara");
                        }

                        if (FobService.ledCharaUUID.equals(characteristic.getUuid())){
                            startHapticFeedback();
                            playNotificationSound();
                        }
                        super.onCharacteristicWriteRequest(device, requestId, characteristic, preparedWrite, responseNeeded, offset, value);
                    }

                    @Override
                    public void onDescriptorReadRequest(BluetoothDevice device, int requestId, int offset, BluetoothGattDescriptor descriptor) {
                        Log.i(TAG, "onDescriptorReadRequest");
                        super.onDescriptorReadRequest(device, requestId, offset, descriptor);
                    }

                    @Override
                    public void onDescriptorWriteRequest(BluetoothDevice device, int requestId, BluetoothGattDescriptor descriptor, boolean preparedWrite, boolean responseNeeded, int offset, byte[] value) {
                        Log.i(TAG, "onDescriptorWriteRequest");
                        super.onDescriptorWriteRequest(device, requestId, descriptor, preparedWrite, responseNeeded, offset, value);
                    }

                    @Override
                    public void onExecuteWrite(BluetoothDevice device, int requestId, boolean execute) {
                        Log.i(TAG, "onExecuteWrite");
                        super.onExecuteWrite(device, requestId, execute);
                    }

                    @Override
                    public void onNotificationSent(BluetoothDevice device, int status) {
                        Log.i(TAG, "onNotificationSent");
                        super.onNotificationSent(device, status);
                    }

                    @Override
                    public void onMtuChanged(BluetoothDevice device, int mtu) {
                        Log.i(TAG, "onMtuChanged");
                        super.onMtuChanged(device, mtu);
                    }

                    @Override
                    public void onPhyUpdate(BluetoothDevice device, int txPhy, int rxPhy, int status) {
                        Log.i(TAG, "onPhyUpdate");
                        super.onPhyUpdate(device, txPhy, rxPhy, status);
                    }

                    @Override
                    public void onPhyRead(BluetoothDevice device, int txPhy, int rxPhy, int status) {
                        Log.i(TAG, "onPhyRead");
                        super.onPhyRead(device, txPhy, rxPhy, status);
                    }

                });
                mBluetoothGattServer.addService(FobService.createFobService());
            }
        }
    }

    /**
     * Stop Gatt server.
     */
    private void  stopGattServer() {
        if (mBluetoothGattServer == null) return;
        mBluetoothGattServer.close();
    }

    private void saveDeviceNameInPreference() {
        SharedPreferences sharedPreferences = getActivity().getSharedPreferences("com.example.avia",Context.MODE_PRIVATE);
        String name = pairedDevice.getAddress();
        Log.i(TAG, "Device name" + name);
        sharedPreferences.edit().putString(Constants.LockName, name).apply();
    }

    private String getDeviceNameFromPreference() {
        SharedPreferences sharedPreferences = getActivity().getSharedPreferences("com.example.avia",Context.MODE_PRIVATE);
        String lockName = sharedPreferences.getString(Constants.LockName, "");
        Log.i(TAG, lockName);
        return  lockName;
    }

    /**
     * Show unpair confirmation
     */
    private void showUnpairConfirmation() {

        new AlertDialog.Builder(getContext())
                .setTitle("Unpair your phone?")
                .setMessage("Are you sure you want to unpair your phone from the lock?")

                // Specifying a listener allows you to take an action before dismissing the dialog.
                // The dialog is automatically dismissed when a dialog button is clicked.
                .setNegativeButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // Continue with delete operation
                        unpairDevice();
                    }
                })

                // A null listener allows the button to dismiss the dialog and take no further action.
                .setPositiveButton(android.R.string.no, null)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    /**
     * Unpair Bluetooth Device
     */

    private void unpairDevice() {
        try {
            Method m = pairedDevice.getClass().getMethod("removeBond",(Class[]) null);
            m.invoke(pairedDevice, (Object[]) null);
            pairedDevice = null;
            pairButton.setVisibility(View.VISIBLE);
            pairButton.setText(R.string.start_pairing);
            unPairButton.setVisibility(View.GONE);
            unlockButton.setVisibility(View.GONE);
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }
    }

    private void unlockDoorLock() {
        Log.i(TAG, "get service");
        BluetoothGattService service = mBluetoothGattServer.getService(FobService.serviceUUID);
        if (service != null) {
            Log.i(TAG, "get chara");
           BluetoothGattCharacteristic btnChara = service.getCharacteristic(FobService.buttonCharaUUID);
            byte[] data = {0x00};
            btnChara.setValue(data);
            Log.i(TAG, "notify chara");
            mBluetoothGattServer.notifyCharacteristicChanged(pairedDevice,btnChara,false);
        }
    }


    /**
     * Returns Intent addressed to the {@code AdvertiserService} class.
     */
    private static Intent getServiceIntent(Context c) {
        Intent intent = new Intent(c, AdvertiserService.class);
        return  intent;
    }

    private Set<BluetoothDevice> getPairedDevices () {
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        Set<BluetoothDevice> bondedDevices = bluetoothAdapter.getBondedDevices();
        return  bondedDevices;
    }
}
