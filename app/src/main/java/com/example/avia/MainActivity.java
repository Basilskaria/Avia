package com.example.avia;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentTransaction;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();
    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (savedInstanceState == null) {
            mBluetoothManager = (BluetoothManager)getSystemService(Context.BLUETOOTH_SERVICE);
            mBluetoothAdapter = mBluetoothManager.getAdapter();
            // Is Bluetooth available on this device?
            if (mBluetoothAdapter != null) {
                // Is Bluetooth turned on?
                if (mBluetoothAdapter.isEnabled()){
                    checkBluetoothMultipleAdvertisement();
                } else {
                    // Prompt user to turn on bluetooth
                    Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(enableIntent, Constants.REQUEST_ENABLE_BT);
                }
            } else {
                // Bluetooth is not available
                showErrorToast(R.string.BLE_Not_Available);
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case Constants.REQUEST_ENABLE_BT:

                if (resultCode == RESULT_OK) {
                    checkBluetoothMultipleAdvertisement();
                } else {
                    // User declined to enable Bluetooth, exit the app.
                    showErrorToast(R.string.BLE_Not_Enabled);
                    finish();
                }
            default:
                super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void setupFragments() {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        AdvertiseFragment advertiserFragment = new AdvertiseFragment();
        transaction.replace(R.id.advertiser_fragment_container, advertiserFragment);
        transaction.commit();
    }

    private void  checkBluetoothMultipleAdvertisement() {
        // Bluetooth is now Enabled, are Bluetooth Advertisements supported on
        // this device?
        if (mBluetoothAdapter.isMultipleAdvertisementSupported()) {
            // Everything is supported and enabled, load the fragments.
            setupFragments();
        } else {
            // Bluetooth Advertisements are not supported.
            showErrorToast(R.string.BLE_Adv_Not_Available);
        }
    }

    private void  showErrorToast(int messageId){
        Toast.makeText(this, getString(messageId), Toast.LENGTH_SHORT).show();
    }
}