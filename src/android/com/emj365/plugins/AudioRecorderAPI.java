package com.emj365.plugins;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONException;
import android.media.MediaRecorder;
import android.media.MediaPlayer;
import android.media.AudioManager;
import android.os.CountDownTimer;
import android.os.Environment;
import android.content.Context;

import java.util.Date;
import java.util.UUID;
import java.io.FileInputStream;
import java.io.File;
import java.io.IOException;

import org.apache.cordova.CordovaInterface;
import org.apache.cordova.LOG;
import android.content.pm.PackageManager;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;

public class AudioRecorderAPI extends CordovaPlugin {

  private MediaRecorder myRecorder;
  private String outputFile;
  private CountDownTimer countDowntimer;
  private Integer seconds;
  private Integer duration;
  private CallbackContext callbackContext;
  private static final String LOG_TAG = "CordovaPermissionHelper";
  public static final int PERMISSION_DENIED_ERROR = 20;
  private long lastBeginRecord = 0L;

  @Override
  public boolean execute(String action, JSONArray args, final CallbackContext callbackContext) throws JSONException {
    Context context = cordova.getActivity().getApplicationContext();
    this.callbackContext = callbackContext;
    if (action.equals("record")) {
      if (args.length() >= 1) {
        this.seconds = args.getInt(0);
      } else {
        this.seconds = 7;
      }
      this.record();
      return true;
    }

    if (action.equals("stop")) {
      stopRecord(callbackContext);
      return true;
    }

    if (action.equals("checkPermission")) {
      Boolean has = this.hasPermission(this, "android.permission.RECORD_AUDIO");
      if(!has) {
        this.requestPermissions(this, 0, new String[] {"android.permission.RECORD_AUDIO"});
        PluginResult r = new PluginResult(PluginResult.Status.NO_RESULT);
        r.setKeepCallback(true);
        callbackContext.sendPluginResult(r);
      } else {
        callbackContext.success();
      }
      return true;
    }

    if (action.equals("playback")) {
      MediaPlayer mp = new MediaPlayer();
      mp.setAudioStreamType(AudioManager.STREAM_MUSIC);
      try {
        FileInputStream fis = new FileInputStream(new File(outputFile));
        mp.setDataSource(fis.getFD());
      } catch (IllegalArgumentException e) {
        e.printStackTrace();
      } catch (SecurityException e) {
        e.printStackTrace();
      } catch (IllegalStateException e) {
        e.printStackTrace();
      } catch (IOException e) {
        e.printStackTrace();
      }
      try {
        mp.prepare();
      } catch (IllegalStateException e) {
        e.printStackTrace();
      } catch (IOException e) {
        e.printStackTrace();
      }
      mp.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
        public void onCompletion(MediaPlayer mp) {
          callbackContext.success("playbackComplete");
        }
      });
      mp.start();
      return true;
    }

    return false;
  }

  /**
   * Checks at runtime to see if the application has been granted a permission. This is a helper
   * method alternative to cordovaInterface.hasPermission() that does not require the project to
   * be built with cordova-android 5.0.0+
   *
   * @param plugin        The plugin the permission is being checked against
   * @param permission    The permission to be checked
   *
   * @return              True if the permission has already been granted and false otherwise
   */
  public boolean hasPermission(CordovaPlugin plugin, String permission) {
      try {
          Method hasPermission = CordovaInterface.class.getDeclaredMethod("hasPermission", String.class);

          // If there is no exception, then this is cordova-android 5.0.0+
          return (Boolean) hasPermission.invoke(plugin.cordova, permission);
      } catch (NoSuchMethodException noSuchMethodException) {
          // cordova-android version is less than 5.0.0, so permission is implicitly granted
          LOG.d(LOG_TAG, "No need to check for permission " + permission);
          return true;
      } catch (IllegalAccessException illegalAccessException) {
          // Should never be caught; this is a public method
          LOG.e(LOG_TAG, "IllegalAccessException when checking permission " + permission, illegalAccessException);
      } catch(InvocationTargetException invocationTargetException) {
          // This method does not throw any exceptions, so this should never be caught
          LOG.e(LOG_TAG, "invocationTargetException when checking permission " + permission, invocationTargetException);
      }
      return false;
  }

  public void requestPermissions(CordovaPlugin plugin, int requestCode, String[] permissions) {
      try {
          Method requestPermission = CordovaInterface.class.getDeclaredMethod(
                  "requestPermissions", CordovaPlugin.class, int.class, String[].class);

          // If there is no exception, then this is cordova-android 5.0.0+
          requestPermission.invoke(plugin.cordova, plugin, requestCode, permissions);
      } catch (NoSuchMethodException noSuchMethodException) {
          // cordova-android version is less than 5.0.0, so permission is implicitly granted
          LOG.d(LOG_TAG, "No need to request permissions " + Arrays.toString(permissions));

          // Notify the plugin that all were granted by using more reflection
          // deliverPermissionResult(plugin, requestCode, permissions);
      } catch (IllegalAccessException illegalAccessException) {
          // Should never be caught; this is a public method
          LOG.e(LOG_TAG, "IllegalAccessException when requesting permissions " + Arrays.toString(permissions), illegalAccessException);
      } catch(InvocationTargetException invocationTargetException) {
          // This method does not throw any exceptions, so this should never be caught
          LOG.e(LOG_TAG, "invocationTargetException when requesting permissions " + Arrays.toString(permissions), invocationTargetException);
      }
  }

  public void onRequestPermissionResult(int requestCode, String[] permissions,
                                        int[] grantResults) throws JSONException
  {
      for(int r:grantResults)
      {
        if(r == PackageManager.PERMISSION_DENIED)
        {
          this.callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.ERROR, PERMISSION_DENIED_ERROR));
          return;
        }
      }
    this.callbackContext.success();
  }

  public void record() {
    Context context = cordova.getActivity().getApplicationContext();

    outputFile = context.getFilesDir().getAbsoluteFile() + "/"
      + UUID.randomUUID().toString() + ".m4a";
    myRecorder = new MediaRecorder();
    myRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
    myRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
    myRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
    myRecorder.setAudioSamplingRate(44100);
    myRecorder.setAudioChannels(1);
    myRecorder.setAudioEncodingBitRate(32000);
    myRecorder.setOutputFile(outputFile);

      // cordova.getThreadPool().execute(new Runnable() {
      //   public void run() {
          try {
            lastBeginRecord = (new Date()).getTime();
          myRecorder.prepare();
          myRecorder.start();
          } catch (final Exception e) {
            cordova.getThreadPool().execute(new Runnable() {
              public void run() {
                callbackContext.error(e.getMessage());
              }
            });
          }
      //   }
      // });

    countDowntimer = new CountDownTimer(this.seconds * 1000, 1000) {
      public void onTick(long millisUntilFinished) {
        duration = seconds - (int) millisUntilFinished / 1000;
      }
      public void onFinish() {
        stopRecord(callbackContext);
      }
    };
    countDowntimer.start();
  }


  private void stopRecord(final CallbackContext callbackContext) {
    try {
      long curTime = (new Date()).getTime();
      if (curTime - lastBeginRecord < 1000) return;
      countDowntimer.cancel();
      myRecorder.stop();
      myRecorder.release();
      cordova.getThreadPool().execute(new Runnable() {
        public void run() {
          try {
            callbackContext.success(composeCallback());
          } catch (JSONException e) {
            callbackContext.error(e.getMessage());
          }
        }
      });
    } catch (Exception e) {
      LOG.e(LOG_TAG, e.toString());
    }
  }
  
  private JSONObject composeCallback() throws JSONException {
    File f = new File(outputFile);
    if(f.exists() && f.length() > 0)
    {
      String json = "{ path: '" + outputFile + "', duration: " + duration + " }";
      return new JSONObject(json);
    }
    else
    {
      String json = "{ path: '', duration: 0 }";
      return new JSONObject(json);
    }
  }

}
