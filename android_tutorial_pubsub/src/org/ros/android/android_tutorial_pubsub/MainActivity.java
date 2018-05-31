package org.ros.android.android_tutorial_pubsub;

import android.os.Bundle;
import org.ros.android.MessageCallable;
import org.ros.android.RosActivity;
import org.ros.android.view.RosTextView;
import org.ros.concurrent.CancellableLoop;
import org.ros.namespace.GraphName;
import org.ros.node.AbstractNodeMain;
import org.ros.node.ConnectedNode;
import org.ros.node.NodeConfiguration;
import org.ros.node.NodeMainExecutor;
import org.ros.node.topic.Publisher;
import org.ros.rosjava_tutorial_pubsub.Talker;
import empatica_e4_msgs.DataArrays;
import org.ros.message.MessageFactory;
import org.ros.node.NodeConfiguration;


import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.empatica.empalink.ConnectionNotAllowedException;
import com.empatica.empalink.EmpaDeviceManager;
import com.empatica.empalink.config.EmpaSensorStatus;
import com.empatica.empalink.config.EmpaSensorType;
import com.empatica.empalink.config.EmpaStatus;
import com.empatica.empalink.delegate.EmpaDataDelegate;
import com.empatica.empalink.delegate.EmpaStatusDelegate;

public class MainActivity extends RosActivity implements EmpaDataDelegate, EmpaStatusDelegate {

  private RosTextView<std_msgs.String> rosTextView;
  private Talker talker;

  private static final int REQUEST_ENABLE_BT = 1;
  private static final int REQUEST_PERMISSION_ACCESS_COARSE_LOCATION = 1;

  private static final long STREAMING_TIME = 3600000; // Stops streaming 1 hour after connection

  private static final String EMPATICA_API_KEY = "290b36eae902472c891ea98e7bb2368d"; // TODO insert your API Key here

  private EmpaDeviceManager deviceManager = null;

  private TextView accel_xLabel;
  private TextView accel_yLabel;
  private TextView accel_zLabel;
  private TextView bvpLabel;
  private TextView edaLabel;
  private TextView ibiLabel;
  private TextView temperatureLabel;
  private TextView batteryLabel;
  private TextView statusLabel;
  private TextView deviceNameLabel;
  private RelativeLayout dataCnt;

  int data_accel_x;
  int data_accel_y;
  int data_accel_z;
  float data_bvp;
  float data_eda;
  float data_ibi;
  float data_temp;
  double data_time;
  int data_status;
  int cnt = 0;
  double sin_ft;
  float [] dataListBVP = new float[30];
  double [] dataListBVP_Time = new double[30];

  float [] dataListEDA = new float [10];
  double [] dataListEDA_Time = new double [10];

  float [] dataListIBI = new float [10];
  double [] dataListIBI_Time = new double [10];

  float [] dataListTEMP = new float [10];
  double [] dataListTEMP_Time = new double [10];

  int [] dataListAccX = new int [30];
  int [] dataListAccY = new int [30];
  int [] dataListAccZ = new int [30];
  float [] dataListAcc_Time = new float [30];

  boolean flag = true;
  public MainActivity() {
    // The RosActivity constructor configures the notification title and ticker
    // messages.
    super("Pubsub Tutorial", "Pubsub Tutorial");
  }
  @SuppressWarnings("unchecked")
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.main);
    rosTextView = (RosTextView<std_msgs.String>) findViewById(R.id.text);
    rosTextView.setTopicName("physiology_data");
    rosTextView.setMessageType(std_msgs.String._TYPE);
    rosTextView.setMessageToStringCallable(new MessageCallable<String, std_msgs.String>() {
      @Override
      public String call(std_msgs.String message) {
        return message.getData();
      }
    });

    // Initialize vars that reference UI components
    statusLabel = (TextView) findViewById(R.id.status);
    dataCnt = (RelativeLayout) findViewById(R.id.dataArea);
    accel_xLabel = (TextView) findViewById(R.id.accel_x);
    accel_yLabel = (TextView) findViewById(R.id.accel_y);
    accel_zLabel = (TextView) findViewById(R.id.accel_z);
    bvpLabel = (TextView) findViewById(R.id.bvp);
    edaLabel = (TextView) findViewById(R.id.eda);
    ibiLabel = (TextView) findViewById(R.id.ibi);
    temperatureLabel = (TextView) findViewById(R.id.temperature);
    batteryLabel = (TextView) findViewById(R.id.battery);
    deviceNameLabel = (TextView) findViewById(R.id.deviceName);

    initEmpaticaDeviceManager();
  }



  @Override
  public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
    switch (requestCode) {
      case REQUEST_PERMISSION_ACCESS_COARSE_LOCATION:
        // If request is cancelled, the result arrays are empty.
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
          // Permission was granted, yay!
          initEmpaticaDeviceManager();
        } else {
          // Permission denied, boo!
          final boolean needRationale = ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_COARSE_LOCATION);
          new AlertDialog.Builder(this)
                  .setTitle("Permission required")
                  .setMessage("Without this permission bluetooth low energy devices cannot be found, allow it in order to connect to the device.")
                  .setPositiveButton("Retry", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                      // try again
                      if (needRationale) {
                        // the "never ask again" flash is not set, try again with permission request
                        initEmpaticaDeviceManager();
                      } else {
                        // the "never ask again" flag is set so the permission requests is disabled, try open app settings to enable the permission
                        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                        Uri uri = Uri.fromParts("package", getPackageName(), null);
                        intent.setData(uri);
                        startActivity(intent);
                      }
                    }
                  })
                  .setNegativeButton("Exit application", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                      // without permission exit is the only way
                      finish();
                    }
                  })
                  .show();
        }
        break;
    }
  }

  private void initEmpaticaDeviceManager() {
    // Android 6 (API level 23) now require ACCESS_COARSE_LOCATION permission to use BLE
    if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
      ActivityCompat.requestPermissions(this, new String[] { Manifest.permission.ACCESS_COARSE_LOCATION }, REQUEST_PERMISSION_ACCESS_COARSE_LOCATION);
    } else {
      // Create a new EmpaDeviceManager. MainActivity is both its data and status delegate.
      deviceManager = new EmpaDeviceManager(getApplicationContext(), this, this);

      if (TextUtils.isEmpty(EMPATICA_API_KEY)) {
        new AlertDialog.Builder(this)
                .setTitle("Warning")
                .setMessage("Please insert your API KEY")
                .setNegativeButton("Close", new DialogInterface.OnClickListener() {
                  public void onClick(DialogInterface dialog, int which) {
                    // without permission exit is the only way
                    finish();
                  }
                })
                .show();
        return;
      }
      // Initialize the Device Manager using your API key. You need to have Internet access at this point.
      deviceManager.authenticateWithAPIKey(EMPATICA_API_KEY);
    }
  }

  @Override
  protected void onPause() {
    super.onPause();
    if (deviceManager != null) {
      deviceManager.stopScanning();
    }
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    if (deviceManager != null) {
      deviceManager.cleanUp();
    }
  }

  @Override
  public void didDiscoverDevice(BluetoothDevice bluetoothDevice, String deviceName, int rssi, boolean allowed) {
    // Check if the discovered device can be used with your API key. If allowed is always false,
    // the device is not linked with your API key. Please check your developer area at
    // https://www.empatica.com/connect/developer.php
    if (allowed) {
      // Stop scanning. The first allowed device will do.
      deviceManager.stopScanning();
      try {
        // Connect to the device
        deviceManager.connectDevice(bluetoothDevice);
        updateLabel(deviceNameLabel, "To: " + deviceName);
      } catch (ConnectionNotAllowedException e) {
        // This should happen only if you try to connect when allowed == false.
        Toast.makeText(MainActivity.this, "Sorry, you can't connect to this device", Toast.LENGTH_SHORT).show();
      }
    }
  }

  @Override
  public void didRequestEnableBluetooth() {
    // Request the user to enable Bluetooth
    Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
    startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    // The user chose not to enable Bluetooth
    if (requestCode == REQUEST_ENABLE_BT && resultCode == Activity.RESULT_CANCELED) {
      // You should deal with this
      return;
    }
    super.onActivityResult(requestCode, resultCode, data);
  }

  @Override
  public void didUpdateSensorStatus(EmpaSensorStatus status, EmpaSensorType type) {
    // No need to implement this right now
  }

  @Override
  public void didUpdateStatus(EmpaStatus status) {
    // Update the UI
    updateLabel(statusLabel, status.name());

    // The device manager is ready for use
    if (status == EmpaStatus.READY) {
      updateLabel(statusLabel, status.name() + " - Turn on your device");
      // Start scanning
      deviceManager.startScanning();
      data_status = 0;
      // The device manager has established a connection
    } else if (status == EmpaStatus.CONNECTED) {
      // Stop streaming after STREAMING_TIME
      runOnUiThread(new Runnable() {
        @Override
        public void run() {
          dataCnt.setVisibility(View.VISIBLE);
          new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
              // Disconnect device
              deviceManager.disconnect();
            }
          }, STREAMING_TIME);
        }
      });
      data_status = 1;

      // The device manager disconnected from a device
    } else if (status == EmpaStatus.DISCONNECTED) {
      updateLabel(deviceNameLabel, "");
      data_status = 2;

    }
  }

  @Override
  public void didReceiveAcceleration(int x, int y, int z, double timestamp) {
    data_accel_x = x;
    data_accel_y = y;
    data_accel_z = z;


    for(int i=0;i<29;i++){
      dataListAccX[i] = dataListAccX[i+1];
      dataListAccY[i] = dataListAccY[i+1];
      dataListAccZ[i] = dataListAccZ[i+1];

      dataListAcc_Time[i] = dataListAcc_Time[i+1];
    }
    dataListAccX[29] = x;
    dataListAccY[29] = y;
    dataListAccZ[29] = z;
    dataListAcc_Time[29] = (float) timestamp;

    updateLabel(accel_xLabel, "" + x);
    updateLabel(accel_yLabel, "" + y);
    updateLabel(accel_zLabel, "" + z);
  }

  @Override
  public void didReceiveBVP(float bvp, double timestamp) {
    data_bvp = bvp;
    data_time = timestamp;

    for(int i=0;i<29;i++){
      dataListBVP[i] = dataListBVP[i+1];
      dataListBVP_Time[i] = dataListBVP_Time[i+1];
    }
    dataListBVP[29] = bvp;
    dataListBVP_Time[29] = timestamp;
    updateLabel(bvpLabel, ""+bvp);
  }

  @Override
  public void didReceiveBatteryLevel(float battery, double timestamp) {
    updateLabel(batteryLabel, String.format("%.0f %%", battery * 100));
  }

  @Override
  public void didReceiveGSR(float gsr, double timestamp) {
    data_eda = gsr;

    for(int i=0;i<9;i++){
      dataListEDA[i] = dataListEDA[i+1];
      dataListEDA_Time[i] = dataListEDA_Time[i+1];
    }

    dataListEDA[9] = gsr;
    dataListEDA_Time[9] = timestamp;

    updateLabel(edaLabel, "" + gsr);
  }

  @Override
  public void didReceiveIBI(float ibi, double timestamp) {
    data_ibi = ibi;

    for(int i=0;i<9;i++){
      dataListIBI[i] = dataListIBI[i+1];
      dataListIBI_Time[i] = dataListIBI_Time[i+1];
    }

    dataListIBI[9] = ibi;
    dataListIBI_Time[9] = timestamp;

    updateLabel(ibiLabel, "" + ibi);
  }

  @Override
  public void didReceiveTemperature(float temp, double timestamp) {
    data_temp = temp;
//    data_time = timestamp;

    for(int i=0;i<9;i++){
      dataListTEMP[i] = dataListTEMP[i+1];
      dataListTEMP_Time[i] = dataListTEMP_Time[i+1];
    }

    dataListTEMP[9] = temp;
    dataListTEMP_Time[9] = timestamp;

    updateLabel(temperatureLabel, "" + temp);
  }

  // Update a label with some text, making sure this is run in the UI thread
  private void updateLabel(final TextView label, final String text) {
    runOnUiThread(new Runnable() {
      @Override
      public void run() {
        label.setText(text);
      }
    });
  }

//  public interface Msgs extends org.ros.internal.message.Message {
//    static final java.lang.String _Type="android_tutorial_pubsub/Msgs";
//    static final java.lang.String _DEFINITION="int64 a \n string b";
//
//    long getA();
//    void setA(long value);
//    String getB();
//    void setB(String value);
//  }

  @Override
  protected void init(NodeMainExecutor nodeMainExecutor) {
    talker = new Talker();

    // At this point, the user has already been prompted to either enter the URI
    // of a master to use or to start a master locally.

    // The user can easily use the selected ROS Hostname in the master chooser
    // activity.
    NodeConfiguration nodeConfiguration = NodeConfiguration.newPublic(getRosHostname());


    nodeConfiguration.setMasterUri(getMasterUri());
    nodeMainExecutor.execute(talker, nodeConfiguration);
    // The RosTextView is also a NodeMain that must be executed in order to
    // start displaying incoming messages.
    nodeMainExecutor.execute(rosTextView, nodeConfiguration);
  }
  public class Talker extends AbstractNodeMain {
    private String topic_name;

    public Talker() {
      this.topic_name = "physiology_data";
    }

//    public Talker(String topic) {
//      this.topic_name = topic;
//    }

    public GraphName getDefaultNodeName() {
      return GraphName.of("Empatica_E4_talker");
    }

    public void onStart(ConnectedNode connectedNode) {
      final Publisher publisher = connectedNode.newPublisher(this.topic_name, "empatica_e4_msgs/DataArrays");
//            Publisher<Msgs> publisher =   
//            connectedNode.newPublisher(topic_name, Msgs._Type);
//            
      connectedNode.executeCancellableLoop(new CancellableLoop() {
        private int sequenceNumber;

        protected void setup() {
          this.sequenceNumber = 0;
        }

        protected void loop() throws InterruptedException {

          empatica_e4_msgs.DataArrays data = (empatica_e4_msgs.DataArrays)publisher.newMessage();

          String tmp_topic = "";

          if (data_status == 0) {
            tmp_topic = "READY";
          } else if (data_status == 2) {
            tmp_topic = "DISCONNECTED";
          } else{
            tmp_topic = "CONNECTED";
            data.setAccelX(dataListAccX);
            data.setAccelY(dataListAccY);
            data.setAccelZ(dataListAccZ);
            data.setBvp(dataListBVP);
            data.setEda(dataListEDA);
            data.setIbi(dataListIBI);
            data.setSkinTemp(dataListTEMP);
            data.setTimestamp(dataListAcc_Time[0]);
            data.setCount(this.sequenceNumber);
            data.setLoopRate(4);
          }

          publisher.publish(data);
          ++this.sequenceNumber;
          Thread.sleep(1000/250);
        }
      });
    }
  }

}