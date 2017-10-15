package com.example.tiago.oscble2.Device;

/**
 * Created by tiago on 19/09/17.
 */


import android.app.Activity;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.WindowManager;
import android.widget.TextView;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

import android.widget.SeekBar;

import com.example.tiago.oscble2.Service.BluetoothLeService;
import com.example.tiago.oscble2.R;
import com.example.tiago.oscble2.SampleGattAttributes;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.github.mikephil.charting.utils.ColorTemplate;


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
    private int[] RGBFrame = {0, 0, 0};
    private TextView isSerial;
    private TextView mConnectionState;
    private TextView mDataField;
    private TextView mTDivision;
    private TextView mVDivision;

    private SeekBar mRed, mGreen, mBlue;
    private String mDeviceName;
    private String mDeviceAddress;
    //  private ExpandableListView mGattServicesList;
    private BluetoothLeService mBluetoothLeService;
    private boolean mConnected = false;
    private BluetoothGattCharacteristic characteristicTX;
    private BluetoothGattCharacteristic characteristicRX;

    public final static UUID HM_RX_TX =
            UUID.fromString(SampleGattAttributes.HM_RX_TX);

    private final String LIST_NAME = "NAME";
    private final String LIST_UUID = "UUID";


    private LineChart mChart;
    Typeface mTfLight;

    byte vDiv;
    byte tDiv;

    static final int SUP = 255;
    static final char SDN = 245;
    static final char SVD = 243;
    static final char STD = 247;

    char state = READ_HEADER;
    static final char SUP_READ = 0;
    static final char SDN_READ = 1;
    static final char SVD_READ = 2;
    static final char STD_READ = 3;
    static final char READ_HEADER = 4;

    List<String> vDivArray = Arrays.asList("0v", "10mV", "100mV", "1V", "10V");
    List<String> tDivArray = Arrays.asList("0s", "10uS", "100uV", "1mS", "10mS");




    StringBuilder recDataString = new StringBuilder();

    int[] procData = new int[640];
    int j = 0, dataPckRec = 0;

/*______________________________________DEBUGGING VARIABLES_____________________________________________________________*/
    Long tsLong, tsLongPrev, tsLongShow;
    private TextView mTs;

    float receivedDataLength = 0;
    private TextView dataLoss;

/*______________________________________________________________________________________________________________________*/

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
                displayGattServices(mBluetoothLeService.getSupportedGattServices());
                mBluetoothLeService.setCharacteristicNotification(characteristicRX, true);               //esta bien ahi?????????????????????????????????''
            } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
                //displayData(intent.getStringExtra(mBluetoothLeService.EXTRA_DATA));
                displayData(intent.getByteArrayExtra(mBluetoothLeService.EXTRA_DATA));
            }
        }
    };

    private void clearUI() {
        mDataField.setText(R.string.no_data);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.gatt_services_characteristics);

        final Intent intent = getIntent();
        mDeviceName = intent.getStringExtra(EXTRAS_DEVICE_NAME);
        mDeviceAddress = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS);

        // Sets up UI references.
        ((TextView) findViewById(R.id.device_address)).setText(mDeviceAddress);
        mConnectionState = (TextView) findViewById(R.id.connection_state);
        mTDivision = (TextView) findViewById(R.id.tDivision);
        mVDivision = (TextView) findViewById(R.id.vDivision);

        tsLongPrev = System.currentTimeMillis()/1000;



/*______________________________________DEBUGGING VARIABLES_____________________________________________________________*/
        mTs = (TextView) findViewById(R.id.ts);
        mDataField = (TextView) findViewById(R.id.data_value);
        dataLoss = (TextView) findViewById(R.id.dl);

/*______________________________________________________________________________________________________________________*/

        // is serial present?
//        isSerial = (TextView) findViewById(R.id.isSerial);




        getActionBar().setTitle(mDeviceName);
        getActionBar().setDisplayHomeAsUpEnabled(true);
        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);


        // LineChart inicialisation
        /*________________________________________________________________________*/


        mChart = (LineChart) findViewById(R.id.chart1);

        // no description text
        mChart.getDescription().setEnabled(false);

        // enable touch gestures
        mChart.setTouchEnabled(false);
//        mChart.setDragDecelerationFrictionCoef(0.9f);

        //set borders
        mChart.setDrawBorders(true);
        //mChart.setBorderColor(Color.rgb(27, 21, 189));
        mChart.setBorderColor(Color.rgb(0, 0, 0));
        mChart.setBorderWidth(10f);

        // enable scaling and dragging
//        mChart.setDragEnabled(true);
//        mChart.setScaleEnabled(true);
        mChart.setDrawGridBackground(true);
        mChart.setHighlightPerDragEnabled(true);


        // set an alternative background color
        mChart.setBackgroundColor(Color.rgb(160, 160, 160));
        mChart.setViewPortOffsets(0f, 0f, 0f, 0f);

        //set nodata text
        mChart.setNoDataText("No se recibieron datos para mostrar");

        // get the legend (only possible after setting data)
        Legend l = mChart.getLegend();
        l.setEnabled(false);


        XAxis xAxis = mChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM_INSIDE);
        xAxis.setTypeface(mTfLight);
        xAxis.setTextSize(10f);
//        xAxis.setXOffset(5000f);
//        xAxis.setDrawAxisLine(true);
//        xAxis.setAxisLineWidth(20);
//        xAxis.setAxisLineColor(Color.rgb(0, 255, 0));
        xAxis.setDrawGridLines(true);
        xAxis.setTextColor(Color.rgb(0, 0, 0));
        xAxis.setDrawLabels(false);
        xAxis.setCenterAxisLabels(true);
        xAxis.setGranularityEnabled(true);
        xAxis.setGranularity(106.6667f);
        xAxis.setAxisMaximum(640);
        xAxis.setAxisMinimum(0);


        YAxis leftAxis = mChart.getAxisLeft();
        leftAxis.setPosition(YAxis.YAxisLabelPosition.INSIDE_CHART);
        leftAxis.setTypeface(mTfLight);
        leftAxis.setDrawGridLines(true);
        leftAxis.setDrawLabels(false);
        leftAxis.setAxisMinimum(-250f);
        leftAxis.setAxisMaximum(250f);
        leftAxis.setGranularityEnabled(true);
        leftAxis.setGranularity(125f);
//        leftAxis.setYOffset(0f);
//        leftAxis.setDrawAxisLine(true);
//        leftAxis.setAxisLineWidth(2);
//        leftAxis.setAxisLineColor(Color.rgb(0, 255, 0));
        leftAxis.setTextColor(Color.rgb(0, 0, 0));

        YAxis rightAxis = mChart.getAxisRight();
        rightAxis.setEnabled(false);


        /*_____________________________________________________________________*/

        updateChart();
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
        switch (item.getItemId()) {
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
                if( resourceId == R.string.connected ){
                    mConnectionState.setTextColor(Color.parseColor("#27AE60"));
                }else{
                    mConnectionState.setTextColor(Color.parseColor("#E74C3C"));
                }
            }
        });
    }

    private void displayData(byte[] data) {

        validateData(data);

    }


    // Demonstrates how to iterate through the supported GATT Services/Characteristics.
    // In this sample, we populate the data structure that is bound to the ExpandableListView
    // on the UI.
    private void displayGattServices(List<BluetoothGattService> gattServices) {
        if (gattServices == null) return;
        String uuid = null;
        String unknownServiceString = getResources().getString(R.string.unknown_service);
        ArrayList<HashMap<String, String>> gattServiceData = new ArrayList<HashMap<String, String>>();


        // Loops through available GATT Services.
        for (BluetoothGattService gattService : gattServices) {
            HashMap<String, String> currentServiceData = new HashMap<String, String>();
            uuid = gattService.getUuid().toString();
            currentServiceData.put(
                    LIST_NAME, SampleGattAttributes.lookup(uuid, unknownServiceString));

            // If the service exists for HM 10 Serial, say so.
 /*           if (SampleGattAttributes.lookup(uuid, unknownServiceString) == "HM 10 Serial") {
                isSerial.setText("Yes, serial :-)");
            } else {
                isSerial.setText("No, serial :-(");
            }*/
            currentServiceData.put(LIST_UUID, uuid);
            gattServiceData.add(currentServiceData);

            // get characteristic when UUID matches RX/TX UUID
            characteristicTX = gattService.getCharacteristic(BluetoothLeService.UUID_HM_RX_TX);
            characteristicRX = gattService.getCharacteristic(BluetoothLeService.UUID_HM_RX_TX);
        }

    }

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        return intentFilter;
    }


    public void validateData(byte[] data) {


        for (int i = 0; i < data.length; i++) {

            receivedDataLength++;

            int unsignedData = (int) data[i] & 0xFF;                                                //cast and bit multiplication for unsigned interpretation

            switch (state) {

                case READ_HEADER:

                    if (unsignedData == SUP)
                        state = SUP_READ;
                    if (unsignedData == SDN)
                        state = SDN_READ;
                    if (unsignedData == SVD)
                        state = SVD_READ;
                    if (unsignedData == STD)
                        state = STD_READ;

                    break;

                case SUP_READ:

                    if (unsignedData < 241) {
                        procData[j] = unsignedData;
//                        recDataString.append(unsignedData + ",");
                        j++;
                    }

                    if (j == 640) {
                        j = 0;
                        dataPckRec++;
  //                      setData(procData);
//                        mDataField.setText(recDataString);
//                        recDataString.delete(0, recDataString.length());                            //clear all string data
                    }

                    state = READ_HEADER;

                    break;

                case SDN_READ:

                    if (unsignedData < 241) {
                        procData[j] = -(unsignedData);
//                        procData[j] = -(240 - unsignedData);
//                        recDataString.append("-" + unsignedData + ",");
                        j++;
                    }
                    if (j == 640) {
                        j = 0;
                        dataPckRec++;
 //                       setData(procData);
//                        mDataField.setText(recDataString);
//                        recDataString.delete(0, recDataString.length());
                    }

                    state = READ_HEADER;

                    break;

                case SVD_READ:

                    vDiv = (byte) unsignedData;

                    if(vDiv<5 && vDiv>=0)
                        mVDivision.setText(vDivArray.get(vDiv));

                    state = READ_HEADER;

                    break;

                case STD_READ:

                    tDiv = (byte) unsignedData;

                    if(tDiv<5 && tDiv>=0)
                        mTDivision.setText(tDivArray.get(tDiv));

                    state = READ_HEADER;

                    break;

                default:

                    state = READ_HEADER;

                    break;
            }
        }
    }


    //Runs the chart display one time per second
    private void updateChart() {
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {

            @Override
            public void run() {

                //allows to draw in the main ui activity
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                        setData(procData);

                    }
                });
            }
        },0,500);
    }



    public void setData(int[] inData) {
        /*_____________________________________________________________*/
        //for debugging, second between receptions counter
        tsLong = System.currentTimeMillis();
        tsLongShow = tsLong - tsLongPrev;
        String ts = tsLongShow.toString();

        mTs.setText(ts+" mS");

        tsLongPrev = tsLong;

        float v = ((receivedDataLength/1284)-1)*100;
        dataLoss.setText(String.valueOf(v));
        receivedDataLength = 0;


        mDataField.setText(String.valueOf(dataPckRec));

        /*_____________________________________________________________*/


        ArrayList<Entry> values = new ArrayList<Entry>();


        for (int i = 0; i < inData.length; i++) {
            values.add(new Entry(i, inData[i]));
        }


        // create a dataset and give it a type
        LineDataSet set1 = new LineDataSet(values, "DataSet 1");
        set1.setAxisDependency(YAxis.AxisDependency.LEFT);
        set1.setColor(Color.rgb(255, 0, 0));
        set1.setValueTextColor(ColorTemplate.getHoloBlue());
        set1.setLineWidth(1.5f);
        set1.setDrawCircles(false);
        set1.setDrawValues(false);
        set1.setFillAlpha(65);
        set1.setFillColor(ColorTemplate.getHoloBlue());
        set1.setHighlightEnabled(false);                    //no cursor
        //set1.setHighLightColor(Color.rgb(0, 0, 0));       //cursor color
        set1.setDrawCircleHole(false);

        // create a data object with the datasets
        LineData data = new LineData(set1);
        data.setValueTextColor(Color.WHITE);
        data.setValueTextSize(9f);

        // set data
        mChart.setData(data);


        //bezier approximation
        List<ILineDataSet> sets = mChart.getData().getDataSets();
/*
        for (ILineDataSet iSet : sets) {

            LineDataSet set = (LineDataSet) iSet;
            set.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        }
*/
        mChart.invalidate();

    }
}



