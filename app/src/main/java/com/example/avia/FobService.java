package com.example.avia;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;

import java.util.UUID;

public class FobService {

    private static final String TAG = FobService.class.getSimpleName();

    /* Mandatory Client Characteristic Config Descriptor */
    public static UUID CLIENT_CONFIG = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
    public static UUID serviceUUID = UUID.fromString(Constants.Fob_Service_UUID);
    public static UUID buttonCharaUUID = UUID.fromString(Constants.Button_UUID);
    public static UUID ledCharaUUID = UUID.fromString(Constants.LED_UUID);
    public static UUID FWVUUID1 = UUID.fromString(Constants.FWV_UUID);
    public static UUID RebondUUID2 = UUID.fromString(Constants.Rebond_UUID);

    public static BluetoothGattService createFobService() {
        BluetoothGattService service = new BluetoothGattService(serviceUUID, BluetoothGattService.SERVICE_TYPE_PRIMARY);

        BluetoothGattCharacteristic buttonChar = new BluetoothGattCharacteristic(buttonCharaUUID,
                BluetoothGattCharacteristic.PROPERTY_READ | BluetoothGattCharacteristic.PROPERTY_NOTIFY,
                BluetoothGattCharacteristic.PERMISSION_READ);
        BluetoothGattDescriptor configDescriptor = new BluetoothGattDescriptor(CLIENT_CONFIG,
                BluetoothGattDescriptor.PERMISSION_READ | BluetoothGattDescriptor.PERMISSION_WRITE);
        buttonChar.addDescriptor(configDescriptor);
        service.addCharacteristic(buttonChar);

        BluetoothGattCharacteristic ledChar = new BluetoothGattCharacteristic(ledCharaUUID,
                BluetoothGattCharacteristic.PROPERTY_READ | BluetoothGattCharacteristic.PROPERTY_WRITE,
                BluetoothGattCharacteristic.PERMISSION_READ|BluetoothGattCharacteristic.PERMISSION_WRITE);
        service.addCharacteristic(ledChar);

        BluetoothGattCharacteristic fwvCharacteristic = new BluetoothGattCharacteristic(FWVUUID1,
                BluetoothGattCharacteristic.PROPERTY_READ ,
                BluetoothGattCharacteristic.PERMISSION_READ);
        service.addCharacteristic(fwvCharacteristic);

        BluetoothGattCharacteristic rebondCharacteristic = new BluetoothGattCharacteristic(RebondUUID2,
                BluetoothGattCharacteristic.PROPERTY_READ | BluetoothGattCharacteristic.PROPERTY_WRITE,
                BluetoothGattCharacteristic.PERMISSION_READ|BluetoothGattCharacteristic.PERMISSION_WRITE);
        service.addCharacteristic(rebondCharacteristic);

        return service;
    }

    public static byte[] firmwareVersion() {
        int i = Constants.firmwareVersion;
        byte[] result = new byte[4];
        result[0] = (byte) (i);
        result[1] = (byte) (i >> 8);
        result[2] = (byte) (i >> 16);
        result[3] = (byte) (i >> 24);
        return  result;
    }
}
