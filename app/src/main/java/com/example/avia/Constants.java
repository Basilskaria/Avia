package com.example.avia;

import android.os.ParcelUuid;

public class Constants {
    public static  final int REQUEST_ENABLE_BT = 1;
    public static final String ADVERTISING_FAILED =
            "com.example.android.bluetoothadvertisements.advertising_failed";
    public static final String ADVERTISING_SUCCESS =
            "com.example.android.bluetoothadvertisements.advertising_success";

    public static final String ADVERTISING_FAILED_EXTRA_CODE = "failureCode";
    public static final int ADVERTISING_TIMED_OUT = 60000;

    public static final ParcelUuid Service_UUID = ParcelUuid
            .fromString(Constants.Fob_Service_UUID);
    public  static  final String Fob_Service_UUID = "00001523-1312-EFDE-1523-785FEABCD123";

    public  static  final String LED_UUID = "00001525-1312-EFDE-1523-785FEABCD123";
    public  static  final String Button_UUID = "00001524-1312-EFDE-1523-785FEABCD123";
    public  static  final String FWV_UUID = "00001526-1312-EFDE-1523-785FEABCD123";
    public  static  final String Rebond_UUID = "00001530-1312-EFDE-1523-785FEABCD123";

    public  static  final  int firmwareVersion = 1000000;
    public  static  final  String LockName = "Avia Doorlock";
}
