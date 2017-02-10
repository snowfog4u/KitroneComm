/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.dronehakgyo.kitronecomm;

import android.app.Activity;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.*;
import android.widget.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.dronehakgyo.mw.*;
import com.dronehakgyo.mw.OSDCommon.MSPCommnand;

/**
 * For a given BLE device, this Activity provides the user interface to connect, display data,
 * and display GATT services and characteristics supported by the device.  The Activity
 * communicates with {@code BluetoothLeService}, which in turn interacts with the
 * Bluetooth LE API.
 */
public class DeviceControlActivity extends Activity {
    private final static String TAG = DeviceControlActivity.class.getSimpleName();

    public static final String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
    public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";

    private TextView mConnectionState;
    private TextView mDataField;
    private String mDeviceName;
    private String mDeviceAddress;
    private ExpandableListView mGattServicesList;
    private BluetoothLeService mBluetoothLeService;
    private ArrayList<ArrayList<BluetoothGattCharacteristic>> mGattCharacteristics =
            new ArrayList<ArrayList<BluetoothGattCharacteristic>>();
    private boolean mConnected = false;
    private BluetoothGattCharacteristic mNotifyCharacteristic;

    private final String LIST_NAME = "NAME";
    private final String LIST_UUID = "UUID";
    
    //
    //++ lbo add
    //
    private Button mBtnVersion;
    private Button mBtnSensorInfo;
    
    private OSDData mOSDData = new OSDData();  
    private String[] MultiTypeName = { "", "TRI", "QUADP", "QUADX", "BI", "GIMBAL", "Y6", "HEX6", "FLYING_WING", "Y4", "HEX6X", "OCTOX8", "OCTOFLATX", "OCTOFLATP", "AIRPLANE", "HELI_120_CCPM", "HELI_90_DEG", "VTAIL4", "HEX6H", "PPM_TO_SERVO", "DUALCOPTER", "SINGLECOPTER" };

    //--

    // Code to manage Service lifecycle.
    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            if (!mBluetoothLeService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                finish();
            }
            // Automatically connects to the device upon successful start-up initialization.
            mBluetoothLeService.connect(mDeviceAddress);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeService = null;
        }
    };

    // Handles various events fired by the Service.
    // ACTION_GATT_CONNECTED: connected to a GATT server.
    // ACTION_GATT_DISCONNECTED: disconnected from a GATT server.
    // ACTION_GATT_SERVICES_DISCOVERED: discovered GATT services.
    // ACTION_DATA_AVAILABLE: received data from the device.  This can be a result of read
    //                        or notification operations.
    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
                mConnected = true;
                updateConnectionState(R.string.connected);
                invalidateOptionsMenu();
            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                mConnected = false;
                updateConnectionState(R.string.disconnected);
                invalidateOptionsMenu();
                clearUI();
            } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                // Show all the supported services and characteristics on the user interface.
                // displayGattServices(mBluetoothLeService.getSupportedGattServices());
            } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
                //displayData(intent.getStringExtra(BluetoothLeService.EXTRA_DATA));
            	displayData( intent.getByteArrayExtra(BluetoothLeService.EXTRA_DATA) );
            }
        }
    };

    // If a given GATT characteristic is selected, check for supported features.  This sample
    // demonstrates 'Read' and 'Notify' features.  See
    // http://d.android.com/reference/android/bluetooth/BluetoothGatt.html for the complete
    // list of supported characteristic features.
    private final ExpandableListView.OnChildClickListener servicesListClickListner =
            new ExpandableListView.OnChildClickListener() {
                @Override
                public boolean onChildClick(ExpandableListView parent, View v, int groupPosition,
                                            int childPosition, long id) {
                    if (mGattCharacteristics != null) {
                        final BluetoothGattCharacteristic characteristic =
                                mGattCharacteristics.get(groupPosition).get(childPosition);
                        final int charaProp = characteristic.getProperties();
                        if ((charaProp | BluetoothGattCharacteristic.PROPERTY_READ) > 0) {
                            // If there is an active notification on a characteristic, clear
                            // it first so it doesn't update the data field on the user interface.
                            if (mNotifyCharacteristic != null) {
                                mBluetoothLeService.setCharacteristicNotification(
                                        mNotifyCharacteristic, false);
                                mNotifyCharacteristic = null;
                            }
                            mBluetoothLeService.readCharacteristic(characteristic);
                        }
                        if ((charaProp | BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
                            mNotifyCharacteristic = characteristic;
                            mBluetoothLeService.setCharacteristicNotification(
                                    characteristic, true);
                        }
                        return true;
                    }
                    return false;
                }
    };

    private void clearUI() {
        // mGattServicesList.setAdapter((SimpleExpandableListAdapter) null);
        mDataField.setText(R.string.no_data);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // setContentView(R.layout.gatt_services_characteristics);
        setContentView(R.layout.main_layout);

        final Intent intent = getIntent();
        mDeviceName = intent.getStringExtra(EXTRAS_DEVICE_NAME);
        mDeviceAddress = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS);

        // Sets up UI references.
        ((TextView) findViewById(R.id.device_address)).setText(mDeviceAddress);
        //mGattServicesList = (ExpandableListView) findViewById(R.id.gatt_services_list);
        //mGattServicesList.setOnChildClickListener(servicesListClickListner);
        mConnectionState = (TextView) findViewById(R.id.connection_state);
        mDataField = (TextView) findViewById(R.id.data_value);

        getActionBar().setTitle(mDeviceName);
        getActionBar().setDisplayHomeAsUpEnabled(true);
        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
        
        //
        //++ lbo add
        //
        mBtnVersion = (Button) findViewById( R.id.btn_version );
        mBtnVersion.setOnClickListener( new OnClickListener() {

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				byte[] mspData = OSDCommon.getSimpleCommand( MSPCommnand.MSP_IDENT );
		    	
				if( mConnected == true && mspData != null )
				{
					mBluetoothLeService.WriteValue( mspData );
					mDataField.setText( "Send Version Command \n" );
				}
				else
				{
					Toast.makeText( getBaseContext(), "Not Connected", Toast.LENGTH_SHORT ).show();
					mDataField.append( "Not Connected \n" );
				}
			}
        	
        });
        
        mBtnSensorInfo = (Button) findViewById( R.id.btn_sensor_info );
        mBtnSensorInfo.setOnClickListener( new OnClickListener() {

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
		    	byte[] mspData = OSDCommon.getSimpleCommand( MSPCommnand.MSP_STATUS );
				//byte[] mspData = OSDCommon.getSimpleCommand( MSPCommnand.MSP_IDENT );
		    	
				if( mConnected == true && mspData != null )
				{
					mBluetoothLeService.WriteValue( mspData );
					mDataField.setText( "Send Sensor Command \n" );
				}
				else
				{
					Toast.makeText( getBaseContext(), "Not Connected", Toast.LENGTH_SHORT ).show();
					mDataField.append( "Not Connected \n" );
				}
			}
        	
        });
        //--
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        if (mBluetoothLeService != null) {
            final boolean result = mBluetoothLeService.connect(mDeviceAddress);
            Log.d(TAG, "Connect request result=" + result);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mGattUpdateReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(mServiceConnection);
        mBluetoothLeService = null;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.gatt_services, menu);
        if (mConnected) {
            menu.findItem(R.id.menu_connect).setVisible(false);
            menu.findItem(R.id.menu_disconnect).setVisible(true);
        } else {
            menu.findItem(R.id.menu_connect).setVisible(true);
            menu.findItem(R.id.menu_disconnect).setVisible(false);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.menu_connect:
                mBluetoothLeService.connect(mDeviceAddress);
                return true;
            case R.id.menu_disconnect:
                mBluetoothLeService.disconnect();
                return true;
            case android.R.id.home:
                onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void updateConnectionState(final int resourceId) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mConnectionState.setText(resourceId);
            }
        });
    }

    private void displayData(String data) {
        if (data != null) {
            mDataField.setText(data);
        }
    }
    

    // Demonstrates how to iterate through the supported GATT Services/Characteristics.
    // In this sample, we populate the data structure that is bound to the ExpandableListView
    // on the UI.
    private void displayGattServices(List<BluetoothGattService> gattServices) {
        if (gattServices == null) return;
        String uuid = null;
        String unknownServiceString = getResources().getString(R.string.unknown_service);
        String unknownCharaString = getResources().getString(R.string.unknown_characteristic);
        ArrayList<HashMap<String, String>> gattServiceData = new ArrayList<HashMap<String, String>>();
        ArrayList<ArrayList<HashMap<String, String>>> gattCharacteristicData
                = new ArrayList<ArrayList<HashMap<String, String>>>();
        mGattCharacteristics = new ArrayList<ArrayList<BluetoothGattCharacteristic>>();

        // Loops through available GATT Services.
        for (BluetoothGattService gattService : gattServices) {
            HashMap<String, String> currentServiceData = new HashMap<String, String>();
            uuid = gattService.getUuid().toString();
            currentServiceData.put(
                    LIST_NAME, SampleGattAttributes.lookup(uuid, unknownServiceString));
            currentServiceData.put(LIST_UUID, uuid);
            gattServiceData.add(currentServiceData);

            ArrayList<HashMap<String, String>> gattCharacteristicGroupData =
                    new ArrayList<HashMap<String, String>>();
            List<BluetoothGattCharacteristic> gattCharacteristics =
                    gattService.getCharacteristics();
            ArrayList<BluetoothGattCharacteristic> charas =
                    new ArrayList<BluetoothGattCharacteristic>();

            // Loops through available Characteristics.
            for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
                charas.add(gattCharacteristic);
                HashMap<String, String> currentCharaData = new HashMap<String, String>();
                uuid = gattCharacteristic.getUuid().toString();
                currentCharaData.put(
                        LIST_NAME, SampleGattAttributes.lookup(uuid, unknownCharaString));
                currentCharaData.put(LIST_UUID, uuid);
                gattCharacteristicGroupData.add(currentCharaData);
            }
            mGattCharacteristics.add(charas);
            gattCharacteristicData.add(gattCharacteristicGroupData);
        }

        SimpleExpandableListAdapter gattServiceAdapter = new SimpleExpandableListAdapter(
                this,
                gattServiceData,
                android.R.layout.simple_expandable_list_item_2,
                new String[] {LIST_NAME, LIST_UUID},
                new int[] { android.R.id.text1, android.R.id.text2 },
                gattCharacteristicData,
                android.R.layout.simple_expandable_list_item_2,
                new String[] {LIST_NAME, LIST_UUID},
                new int[] { android.R.id.text1, android.R.id.text2 }
        );
        mGattServicesList.setAdapter(gattServiceAdapter);
    }

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        return intentFilter;
    }
    
    //
    //++ lbo add
    //
    private void displayData( byte[] data )
    {
    	//byte[] data = intent.getByteArrayExtra(BluetoothLeService.EXTRA_DATA);
    	Log.d( TAG, "Instream Data - Length : " + data.length + ", data : " + HexUtils.hexToStringPrint( data, 0, data.length ) );
    	
    	mDataField.append( "Receive Data : " + HexUtils.hexToStringPrint( data, 0, data.length ) + "\n" );
    	
    	mOSDData.parseRawData( data );
    	
    	int command = (data[4] & 0xFF);
    	
    	if( command == OSDCommon.MSP_IDENT )
    	{    	
	    	displayVersion();
    	}
    	else if( command == OSDCommon.MSP_STATUS )
    	{
    		displaySensorInfo();
    	}
    }
    
    private void displayVersion()
    {
    	int version = mOSDData.getVersion();
    	int multiType = mOSDData.getMultiType();
    	int mspVersion = mOSDData.getMSPVersion();
    	int multiCapability = mOSDData.getMultiCapability();
    	
    	String strMsg = "Version : " + version + "(" + String.valueOf(version / 100f) +")\n";
    	strMsg += "MultiType : " + multiType + "[" + MultiTypeName[multiType] + "]\n";
    	strMsg += "MSP_VERSION : " + mspVersion + "(" + String.valueOf(mspVersion) + ")\n";
    	strMsg += "Capability : " + multiCapability;
    	
    	mDataField.append( strMsg );    	
    }
    
    private void displaySensorInfo()
    {
    	boolean accPresent = false;
    	boolean baroPresent = false;
    	boolean magPresent = false;
    	boolean gpsPresent = false;
    	boolean sonarPresent = false;
    	
    	int cycleTime = mOSDData.getCycleTime();
    	int i2cError = mOSDData.getI2cError();
    	int sensorPresent = mOSDData.getPresent();
    	int mode = mOSDData.getMode();
    	
    	if( (sensorPresent & 1) > 0 )
    	{
    		accPresent = true;
    	}
    	
    	if( (sensorPresent & 2) > 0 )
    	{
    		baroPresent = true;
    	}
    	
    	if( (sensorPresent & 4) > 0 )
    	{
    		magPresent = true;
    	}
    	
    	if( (sensorPresent & 8) > 0 )
    	{
    		gpsPresent = true;
    	}
    	
    	if( (sensorPresent & 16) > 0 )
    	{
    		sonarPresent = true;
    	}
    	
    	String strMsg = "CycleTime : " + cycleTime + "\n";
    	strMsg += "I2c Error Count : " + i2cError + "\n";
    	strMsg += "Sensor Present : " + sensorPresent + "\n";
    	strMsg += "\t AccPresent : " + accPresent + "\n";
    	strMsg += "\t BaroPresent : " + baroPresent + "\n";
    	strMsg += "\t MagPresent : " + magPresent + "\n";
    	strMsg += "\t gpsPresent : " + gpsPresent + "\n";
    	strMsg += "\t sonarPresent : " + sonarPresent + "\n";
    	strMsg += "Mode : " + mode;
    	
    	mDataField.append( strMsg );
    }
    
    //--
}
