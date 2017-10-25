
package com.reactlibrary;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

import android.content.Context;
import android.util.Log;
import com.facebook.react.modules.core.DeviceEventManagerModule;
import com.facebook.react.bridge.Arguments;

import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.Callback;

public class RNSimpleCompassModule extends ReactContextBaseJavaModule implements SensorEventListener {

  private final ReactApplicationContext reactContext;

  private static Context mApplicationContext;
  private int mAzimuth = 0; // degree
  private int mFilter = 1;
  private SensorManager mSensorManager;
  private Sensor mSensor;
    private Sensor gSensor;
    private Sensor magSensor;
  private float[] orientation = new float[3];
  private float[] rMat = new float[9];
    private float[] mGravity = new float[3];
    private float[] mGeomagnetic = new float[3];
    private float azimuth = 0f;



  public RNSimpleCompassModule(ReactApplicationContext reactContext) {
    super(reactContext);
    this.reactContext = reactContext;
    mApplicationContext = reactContext.getApplicationContext();
      Log.d("RNSimpleCompassModule", "constructor finished");
  }

  @Override
  public String getName() {
    return "RNSimpleCompass";
  }

  @ReactMethod
  public void start(int filter) {
      Log.d("RNSimpleCompassModule", "start filter: " + filter);

      if (mSensorManager == null) {
          mSensorManager = (SensorManager) mApplicationContext.getSystemService(Context.SENSOR_SERVICE);
      }

      if (mSensor == null) {
          mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
          Log.d("RNSimpleCompassModule", "mSensor set");
      }

      if (gSensor == null) {
          gSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
          Log.d("RNSimpleCompassModule", "gSensor set");
      }

      if (magSensor == null) {
          magSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
          Log.d("RNSimpleCompassModule", "magSensor set");
      }

      mFilter = filter;
      if (mSensor != null) {
          mSensorManager.registerListener(this, mSensor, SensorManager.SENSOR_DELAY_UI);
          Log.d("RNSimpleCompassModule", "mSensor registered");
      }
      if (gSensor != null) {
          mSensorManager.registerListener(this, gSensor, SensorManager.SENSOR_DELAY_UI);
          Log.d("RNSimpleCompassModule", "gSensor registered");
      }
      if (magSensor != null) {
          mSensorManager.registerListener(this, magSensor, SensorManager.SENSOR_DELAY_UI);
          Log.d("RNSimpleCompassModule", "magSensor registered");
      }
  }

  @ReactMethod
  public void stop() {
    if (mSensorManager != null) {
      mSensorManager.unregisterListener(this);
    }
  }

  @Override
  public void onSensorChanged(SensorEvent event) {
      if( event.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR ){
          Log.d("RNSimpleCompassModule", "onSensorChanged TYPE_ROTATION_VECTOR");
          // calculate th rotation matrix
          SensorManager.getRotationMatrixFromVector(rMat, event.values);
          // get the azimuth value (orientation[0]) in degree
          int newAzimuth = (int) ( Math.toDegrees( SensorManager.getOrientation( rMat, orientation )[0] ) + 360 ) % 360;

          //dont react to changes smaller than the filter value
          if (Math.abs(mAzimuth - newAzimuth) < mFilter) {
              return;
          }

          mAzimuth = newAzimuth;

          getReactApplicationContext()
                  .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                  .emit("HeadingUpdated", mAzimuth);

      } else if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER || event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
          Log.d("RNSimpleCompassModule", "onSensorChanged TYPE_ACCELEROMETER or TYPE_MAGNETIC_FIELD");
          final float alpha = 0.97f;

          synchronized (this) {
              if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                  mGravity[0] = alpha * mGravity[0] + (1 - alpha) * event.values[0];
                  mGravity[1] = alpha * mGravity[1] + (1 - alpha) * event.values[1];
                  mGravity[2] = alpha * mGravity[2] + (1 - alpha) * event.values[2];
              }

              if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
                  mGeomagnetic[0] = alpha * mGeomagnetic[0] + (1 - alpha) * event.values[0];
                  mGeomagnetic[1] = alpha * mGeomagnetic[1] + (1 - alpha) * event.values[1];
                  mGeomagnetic[2] = alpha * mGeomagnetic[2] + (1 - alpha) * event.values[2];
              }

              float R[] = new float[9];
              float I[] = new float[9];

              boolean success = SensorManager.getRotationMatrix(R, I, mGravity, mGeomagnetic);

              if (success) {
                  float orientation[] = new float[3];
                  SensorManager.getOrientation(R, orientation);

                  azimuth = (float) Math.toDegrees(orientation[0]); // orientation
                  int newAzimuth = (int)((azimuth + 360) % 360);

                  if (Math.abs(mAzimuth - newAzimuth) >= mFilter) {
                      mAzimuth = newAzimuth;
                      getReactApplicationContext()
                              .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                              .emit("HeadingUpdated", mAzimuth);
                  }
              }
          }
      }
  }


  @Override
  public void onAccuracyChanged(Sensor sensor, int accuracy) {

  }
}
