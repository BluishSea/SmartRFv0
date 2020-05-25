package com.ruitai.aijiubao.module;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.ArrayMap;
import android.util.Log;

import com.ruitai.aijiubao.utils.BltConstants;

import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by static on 2017/10/9.
 */

public class BltConn {

    private static final String TAG = "BltConn";

    private final Context mContext;
    private final Timer mConnTimer;
    private boolean mScanning;
    private BluetoothAdapter mBluetoothAdapter;

    private Handler mHandler;


    private ArrayMap<String, Integer> mBltAddressContainer;
    private ArrayMap<Integer, BltNeedle> mNeedleContainer;

    public BltConn(Context context, Handler handler, BluetoothAdapter bluetoothAdapter, ArrayMap<String, Integer> bltAddressContainer, ArrayMap<Integer, BltNeedle> needleContainer) {
        this.mContext = context;
        this.mBluetoothAdapter = bluetoothAdapter;
        this.mBltAddressContainer = bltAddressContainer;
        this.mNeedleContainer = needleContainer;
        this.mHandler = handler;

        Log.e(TAG,"scanLeDevice");
        scanLeDevice(true);
        mConnTimer = new Timer();
        mConnTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                scanLeDevice(false);
                doConnect();
            }
        }, 5000);
    }

    private void scanLeDevice(final boolean enable) {
        if (enable) {
            mScanning = true;
            mBluetoothAdapter.startLeScan(mLeScanCallback);
        } else {
            mScanning = false;
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
        }
    }

    private void doConnect() {


        for (String key : mBltAddressContainer.keySet()) {
            Log.e(TAG,"doConnect key = "+key);
            BltNeedle bltNeedle = new BltNeedle(mContext, mHandler, mBluetoothAdapter, key);
            mNeedleContainer.put(mBltAddressContainer.get(key), bltNeedle);
        }
    }

    private void doDisConnect() {

        for (Integer key : mNeedleContainer.keySet()) {
            BltNeedle bltNeedle = mNeedleContainer.get(key);
            if (bltNeedle != null && bltNeedle.mNeedleBean != null) {
                Log.e(TAG,"doDisConnect");
                bltNeedle.disconnect();
            }
        }
    }


    private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {

        @Override
        public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
            Log.e(TAG,"onLeScan  device = " +device);

            if (mBltAddressContainer.size() >= 5) {
                scanLeDevice(false);
                doConnect();
            }

            String bluetoothAddress = device.getAddress();
            String name = device.getName();
            Log.e(TAG,"name = " +name);

            if (BltConstants.SMART_BLT_NAME.equals(name)&&!mBltAddressContainer.containsKey(bluetoothAddress)) {
                mBltAddressContainer.put(bluetoothAddress, mBltAddressContainer.size() + 1);
            }
        }
    };


    //+==============================================+//


    public void clearData() {
        doDisConnect();
        mBltAddressContainer.clear();
        mNeedleContainer.clear();
    }


    public void reflash() {
        Log.e(TAG,"reflash");
        scanLeDevice(true);
        mConnTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                Message msg = new Message();
                msg.what = BltConstants.MSG_HANDLER_ON_BLT_STOP_SCAN;
                mHandler.sendMessage(msg);
                scanLeDevice(false);
                doConnect();
            }
        }, 6000);
    }
}
