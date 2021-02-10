package com.example.avia;

import android.bluetooth.le.AdvertiseCallback;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.drawable.AnimationDrawable;
import android.media.Image;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import static android.content.ContentValues.TAG;

public class AdvertiseFragment extends Fragment implements View.OnClickListener {

    private static final String TAG = AdvertiseFragment.class.getSimpleName();

    /**
     * Button for start advertising
     */
    private Button advButton;

    /**
     * ImageView for fob
     */
    private ImageView fobImage;



    /**
     * Listens for notifications that the {@code AdvertiserService} has failed to start advertising.
     * This Receiver deals with Fragment UI elements and only needs to be active when the Fragment
     * is on-screen, so it's defined and registered in code instead of the Manifest.
     */
    private BroadcastReceiver advertisingFailureReceiver;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

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

                Toast.makeText(getActivity(), errorMessage, Toast.LENGTH_LONG).show();
            }
        };
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_advertise, container, false);
        advButton = (Button) view.findViewById(R.id.advertisingButton);
        advButton.setOnClickListener((View.OnClickListener) this);
        fobImage = (ImageView)view.findViewById(R.id.fobImg);

        return view;
    }

    @Override
    public void onClick(View view) {

        switch (view.getId()) {
            case R.id.advertisingButton:
                if (!AdvertiserService.running) {
                    advButton.setVisibility(View.INVISIBLE);
                    fobImage.setVisibility(View.VISIBLE);
                    startAdvertising();
                } else {
                    advButton.setVisibility(View.VISIBLE);
                    fobImage.setVisibility(View.INVISIBLE);
                    stopAdvertising();
                }
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
    }
    @Override
    public void onPause() {
        super.onPause();
        getActivity().unregisterReceiver(advertisingFailureReceiver);
    }

    /**
     * Update UI
     */
    private void updateUI(boolean isAdvertising) {
        if (isAdvertising) {
            advButton.setVisibility(View.INVISIBLE);
            fobImage.setVisibility(View.VISIBLE);
        } else {
            advButton.setVisibility(View.VISIBLE);
            fobImage.setVisibility(View.INVISIBLE);
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
        advButton.setVisibility(View.VISIBLE);
    }

    /**
     * Returns Intent addressed to the {@code AdvertiserService} class.
     */
    private static Intent getServiceIntent(Context c) {
        Intent intent = new Intent(c, AdvertiserService.class);
        return  intent;
    }
}
