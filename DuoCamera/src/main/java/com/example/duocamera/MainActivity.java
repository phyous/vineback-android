package com.example.duocamera;

import android.content.Intent;
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
import android.widget.ImageView;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

/**
 * Docs on video recodring
 * <p/>
 * http://stackoverflow.com/questions/1817742/how-can-i-capture-a-video-mRecording-on-android
 * <p/>
 * http://developer.android.com/guide/topics/media/camera.html#manifest
 * <p/>
 * https://github.com/vanevery/Custom-Video-Capture-with-Preview
 */


public class MainActivity extends Activity implements OnClickListener, SurfaceHolder.Callback {

    public static final String TAG = "VIDEOCAPTURE";
    public static final String EXTRA_VIDEOS = "EXTRA_VIDEOS";

    private MediaRecorder mRecorder;
    private SurfaceHolder mHolder;
    private CamcorderProfile mCamcorderProfile;
    private Camera mCamera;
    private ArrayList<String> mVideoList;
    private ImageView mRecordButton;

    private boolean mRecording = false;
    private boolean mPreviewRunning = false;
    private RecordingState mRecordingState = RecordingState.BEGIN;

    public void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "onCreate");
        super.onCreate(savedInstanceState);
        mVideoList = new ArrayList<String>(2);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        // Set up mRecording profile
        mCamcorderProfile = CamcorderProfile.get(CamcorderProfile.QUALITY_LOW);
        mCamcorderProfile.fileFormat = MediaRecorder.OutputFormat.MPEG_4;

        setContentView(R.layout.activity_main);

        prepareRecordingSurface(R.id.camera_a);
        mRecordButton = (ImageView) findViewById(R.id.record_button_image_view);

        // Set up clicking logic
        View layout = findViewById(R.id.recordingLayout);
        layout.setClickable(true);
        layout.setOnClickListener(this);
    }

    @Override public void onPause() {
        super.onPause();
        Log.i(TAG, "onPause");
        releaseRecorder();
        releaseCamera();
    }

    @Override public void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "onDestroy");
        releaseRecorder();
        releaseCamera();
    }

    private void prepareRecordingSurface(int viewId) {
        Log.v(TAG, "prepareRecordingSurface");
        SurfaceView cameraView = (SurfaceView) findViewById(viewId);
        mHolder = cameraView.getHolder();
        mHolder.addCallback(this);
        mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }

    private void clearRecodingSurface() {
        Log.v(TAG, "clearRecodingSurface");
        mHolder.getSurface().release();
        releaseRecorder();
        releaseCamera();
    }

    private void prepareRecorder() {
        Log.v(TAG, "prepareRecorder");
        mRecorder = new MediaRecorder();
        mRecorder.setPreviewDisplay(mHolder.getSurface());

        mCamera.unlock();
        mRecorder.setCamera(mCamera);

        mRecorder.setAudioSource(MediaRecorder.AudioSource.DEFAULT);
        mRecorder.setVideoSource(MediaRecorder.VideoSource.DEFAULT);

        mRecorder.setProfile(mCamcorderProfile);

        try {
            File newFile = File.createTempFile("videocapture", ".mp4", Environment.getExternalStorageDirectory());
            final String filePath = newFile.getAbsolutePath();
            mVideoList.add(filePath);
            Log.v(TAG, String.format("Created file: %s", filePath));
            mRecorder.setOutputFile(filePath);
        } catch (IOException e) {
            Log.v(TAG, "Couldn't create file");
            e.printStackTrace();
            finish();
        }

        // TODO: Set mRecording length constraints
        //mRecorder.setMaxDuration(50000); // 50 seconds
        //mRecorder.setMaxFileSize(5000000); // Approximately 5 megabytes

        try {
            mRecorder.prepare();
        } catch (IllegalStateException e) {
            e.printStackTrace();
            finish();
        } catch (IOException e) {
            e.printStackTrace();
            finish();
        }
    }

    public void onClick(View v) {
        Log.v(TAG, "onClick");
        switch (mRecordingState) {
            case BEGIN:
                startRecording();
                mRecordButton.setImageResource(R.drawable.photo_camera_red);
                mRecordingState = RecordingState.RECORDING_A;
                break;
            case RECORDING_A:
                stopRecording();
                clearRecodingSurface();
                prepareRecordingSurface(R.id.camera_b);
                mRecordButton.setImageResource(R.drawable.photo_camera_green);
                surfaceCreated(mHolder);
                surfaceChanged(mHolder, 0, 0, 0);
                //prepareRecorder();
                mRecordingState = RecordingState.PENDING_B;
                break;
            case PENDING_B:
                startRecording();
                mRecordButton.setImageResource(R.drawable.photo_camera_red);
                mRecordingState = RecordingState.RECORDING_B;
                break;
            case RECORDING_B:
                stopRecording();
                mRecordButton.setImageResource(R.drawable.right);
                mRecordingState = RecordingState.PREVIEW;
                break;
            case PREVIEW:
                clearRecodingSurface();
                startVideoPreview();
                break;
            default:
                break;
        }
    }

    private void startVideoPreview() {
        Intent intent = new Intent(this, PlaybackActivity.class);
        intent.putExtra(EXTRA_VIDEOS, mVideoList);
        startActivity(intent);
    }

    private void stopRecording() {
        Log.v(TAG, "stopRecording");
        mRecorder.stop();
        try {
            mCamera.reconnect();
        } catch (IOException e) {
            e.printStackTrace();
        }
        mRecorder.release();
        mRecording = false;
    }

    private void startRecording() {
        Log.v(TAG, "startRecording");
        mRecording = true;
        mRecorder.start();
    }

    public void surfaceCreated(SurfaceHolder holder) {
        Log.v(TAG, "surfaceCreated");

        // TODO: handle if mCamera can't be opened
        // TODO: enable choosing of camera (front, back)
        //mCamera = Camera.open();
        mCamera = Camera.open(Camera.CameraInfo.CAMERA_FACING_FRONT);

        try {
            mCamera.setPreviewDisplay(holder);
            mCamera.setDisplayOrientation(90);
            mCamera.startPreview();
            mPreviewRunning = true;
        } catch (IOException e) {
            Log.e(TAG, e.getMessage());
            e.printStackTrace();
        }
    }


    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        Log.v(TAG, "surfaceChanged");

        if (!mRecording) {
            if (mPreviewRunning) {
                mCamera.stopPreview();
            }

            try {
                Camera.Parameters p = mCamera.getParameters();

                p.setPreviewSize(mCamcorderProfile.videoFrameWidth, mCamcorderProfile.videoFrameHeight);
                p.setPreviewFrameRate(mCamcorderProfile.videoFrameRate);

                mCamera.setParameters(p);

                mCamera.setPreviewDisplay(holder);
                mCamera.startPreview();
                mPreviewRunning = true;
            } catch (IOException e) {
                Log.e(TAG, e.getMessage());
                e.printStackTrace();
            }

            prepareRecorder();
        }
    }


    public void surfaceDestroyed(SurfaceHolder holder) {
        Log.v(TAG, "surfaceDestroyed");
        releaseRecorder();
        releaseCamera();

        finish();
    }

    private void releaseRecorder() {
        Log.v(TAG, "releaseRecorder");
        if (mRecorder != null) {
            if (mRecording) {
                mRecorder.stop();
                mRecording = false;
            }
            mRecorder.setPreviewDisplay(null);
            mRecorder.release();
            mPreviewRunning = false;
        }
    }

    private void releaseCamera() {
        Log.v(TAG, "releaseCamera");
        if (mCamera != null) {
            mCamera.lock();
            mCamera.stopPreview();
            mPreviewRunning = false;
            mCamera.setPreviewCallback(null);
            mCamera.release();
            mCamera = null;
        }
    }
}
