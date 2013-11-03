package com.example.duocamera;

import android.content.pm.ActivityInfo;
import android.hardware.Camera;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.app.Activity;
import android.os.Environment;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.View.OnClickListener;

import java.io.File;
import java.io.IOException;

/**
 * Docs on video recodring
 * <p/>
 * http://stackoverflow.com/questions/1817742/how-can-i-capture-a-video-recording-on-android
 * <p/>
 * http://developer.android.com/training/camera/cameradirect.html
 * <p/>
 * https://github.com/vanevery/Custom-Video-Capture-with-Preview
 */


public class MainActivity extends Activity implements OnClickListener, SurfaceHolder.Callback {

    public static final String TAG = "VIDEOCAPTURE";

    private MediaRecorder recorder;
    private SurfaceHolder holder;
    private CamcorderProfile camcorderProfile;
    private Camera camera;

    boolean recording = false;
    boolean previewRunning = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        // Set up recording profile
        camcorderProfile = CamcorderProfile.get(CamcorderProfile.QUALITY_LOW);
        camcorderProfile.fileFormat = MediaRecorder.OutputFormat.MPEG_4;

        setContentView(R.layout.activity_main);

        SurfaceView cameraView = (SurfaceView) findViewById(R.id.camera_top);
        holder = cameraView.getHolder();
        holder.addCallback(this);
        holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

        cameraView.setClickable(true);
        cameraView.setOnClickListener(this);
    }

    private void prepareRecorder() {
        recorder = new MediaRecorder();
        recorder.setPreviewDisplay(holder.getSurface());

        camera.unlock();
        recorder.setCamera(camera);

        recorder.setAudioSource(MediaRecorder.AudioSource.DEFAULT);
        recorder.setVideoSource(MediaRecorder.VideoSource.DEFAULT);

        recorder.setProfile(camcorderProfile);

        try {
            File newFile = File.createTempFile("videocapture", ".mp4", Environment.getExternalStorageDirectory());
            final String filePath = newFile.getAbsolutePath();
            Log.v(TAG, String.format("Created file: %s", filePath));
            recorder.setOutputFile(filePath);
        } catch (IOException e) {
            Log.v(TAG, "Couldn't create file");
            e.printStackTrace();
            finish();
        }
        //recorder.setMaxDuration(50000); // 50 seconds
        //recorder.setMaxFileSize(5000000); // Approximately 5 megabytes

        try {
            recorder.prepare();
        } catch (IllegalStateException e) {
            e.printStackTrace();
            finish();
        } catch (IOException e) {
            e.printStackTrace();
            finish();
        }
    }

    public void onClick(View v) {
        if (recording) {
            recorder.stop();
            try {
                camera.reconnect();
            } catch (IOException e) {
                e.printStackTrace();
            }
            recorder.release();
            recording = false;
            Log.v(TAG, "Recording Stopped");
            // Let's prepareRecorder so we can record again
            prepareRecorder();
        } else {
            recording = true;
            recorder.start();
            Log.v(TAG, "Recording Started");
        }
    }

    public void surfaceCreated(SurfaceHolder holder) {
        Log.v(TAG, "surfaceCreated");

        camera = Camera.open();

        try {
            camera.setPreviewDisplay(holder);
            camera.setDisplayOrientation(90);
            camera.startPreview();
            previewRunning = true;
        } catch (IOException e) {
            Log.e(TAG, e.getMessage());
            e.printStackTrace();
        }
    }


    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        Log.v(TAG, "surfaceChanged");

        if (!recording) {
            if (previewRunning) {
                camera.stopPreview();
            }

            try {
                Camera.Parameters p = camera.getParameters();

                p.setPreviewSize(camcorderProfile.videoFrameWidth, camcorderProfile.videoFrameHeight);
                p.setPreviewFrameRate(camcorderProfile.videoFrameRate);

                camera.setParameters(p);

                camera.setPreviewDisplay(holder);
                camera.startPreview();
                previewRunning = true;
            } catch (IOException e) {
                Log.e(TAG, e.getMessage());
                e.printStackTrace();
            }

            prepareRecorder();
        }
    }


    public void surfaceDestroyed(SurfaceHolder holder) {
        Log.v(TAG, "surfaceDestroyed");
        if (recording) {
            recorder.stop();
            recording = false;
        }
        recorder.release();
        previewRunning = false;
        //camera.lock();
        camera.release();
        finish();
    }
}
