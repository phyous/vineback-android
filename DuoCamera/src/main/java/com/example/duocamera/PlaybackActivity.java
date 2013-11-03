package com.example.duocamera;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.AssetFileDescriptor;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.*;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;

import java.io.File;
import java.util.ArrayList;

/**
 * How we want to implement this:
 * http://stackoverflow.com/questions/9834882/how-to-play-multiple-video-files-simultaneously-in-one-layout-side-by-side-in-di/10161316#10161316
 */
public class PlaybackActivity extends FragmentActivity {
    private static final String TAG = "MediaPlayer";
    private ArrayList<String> mVideos;

    @Override public void onCreate(Bundle bundle) {
        super.onCreate(bundle);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        //Intent i = getIntent();
        //mVideos = i.getStringArrayListExtra(MainActivity.EXTRA_VIDEOS);

        setContentView(R.layout.activity_playback);
    }

    public static class VideoFragment extends Fragment implements
            OnBufferingUpdateListener, OnCompletionListener, OnPreparedListener, OnVideoSizeChangedListener, SurfaceHolder.Callback {

        private MediaPlayer mMediaPlayer;
        private SurfaceView mSurfaceView;
        private SurfaceHolder mSurfaceHolder;
        private boolean mSizeKnown;
        private boolean mVideoReady;

        @Override public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            return inflater.inflate(R.layout.multi_videos_fragment_layout, container, false);
        }

        @Override public void onActivityCreated(Bundle savedInstanceState) {
            super.onActivityCreated(savedInstanceState);
            mSurfaceView = (SurfaceView) getView().findViewById(R.id.video_surfaceview);
            mSurfaceHolder = mSurfaceView.getHolder();
            mSurfaceHolder.addCallback(this);
            mSurfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        }

        public void onBufferingUpdate(MediaPlayer player, int percent) {
            Log.d(TAG, "onBufferingUpdate percent: " + percent);
        }

        public void onCompletion(MediaPlayer player) {
            Log.d(TAG, "onCompletion called");
        }

        public void onVideoSizeChanged(MediaPlayer player, int width, int height) {
            Log.v(TAG, "onVideoSizeChanged called");
            if (width == 0 || height == 0) {
                Log.e(TAG, "invalid video width(" + width + ") or height(" + height + ")");
                return;
            }

            mSizeKnown = true;
            if (mVideoReady && mSizeKnown) {
                startVideoPlayback();
            }
        }

        public void onPrepared(MediaPlayer player) {
            Log.d(TAG, "onPrepared called");

            mVideoReady = true;
            if (mVideoReady && mSizeKnown) {
                startVideoPlayback();
            }
        }

        public void surfaceChanged(SurfaceHolder holder, int i, int j, int k) {
            Log.d(TAG, "surfaceChanged called");
        }

        public void surfaceDestroyed(SurfaceHolder holder) {
            Log.d(TAG, "surfaceDestroyed called");
        }

        public void surfaceCreated(SurfaceHolder holder) {
            Log.d(TAG, "surfaceCreated called");

            try {
                mMediaPlayer = new MediaPlayer();
                mMediaPlayer.setDataSource(this.getActivity(), Uri.fromFile(new File("/sdcard/videocapture-2068449435.mp4")));
                mMediaPlayer.setDisplay(mSurfaceHolder);
                mMediaPlayer.prepare();
                mMediaPlayer.setOnBufferingUpdateListener(this);
                mMediaPlayer.setOnCompletionListener(this);
                mMediaPlayer.setOnPreparedListener(this);
                mMediaPlayer.setOnVideoSizeChangedListener(this);
                mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            }
            catch (Exception e) { e.printStackTrace(); }
        }

        @Override public void onPause() {
            super.onPause();
            releaseMediaPlayer();
        }

        @Override public void onDestroy() {
            super.onDestroy();
            releaseMediaPlayer();
        }

        private void releaseMediaPlayer() {
            if (mMediaPlayer != null) {
                mMediaPlayer.release();
                mMediaPlayer = null;
            }
        }

        private void startVideoPlayback() {
            Log.v(TAG, "startVideoPlayback");
            mMediaPlayer.start();
        }
    }
}