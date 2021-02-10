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
    public static UUID charaUUID1 = UUID.fromString(Constants.Chara1_UUID);
    public static UUID charaUUID2 = UUID.fromString(Constants.Chara2_UUID);

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

        BluetoothGattCharacteristic char1 = new BluetoothGattCharacteristic(charaUUID1,
                BluetoothGattCharacteristic.PROPERTY_READ ,
                BluetoothGattCharacteristic.PERMISSION_READ);
        service.addCharacteristic(char1);

        BluetoothGattCharacteristic char2 = new BluetoothGattCharacteristic(charaUUID2,
                BluetoothGattCharacteristic.PROPERTY_READ | BluetoothGattCharacteristic.PROPERTY_WRITE,
                BluetoothGattCharacteristic.PERMISSION_READ|BluetoothGattCharacteristic.PERMISSION_WRITE);
        service.addCharacteristic(char2);

        return service;
    }
}
