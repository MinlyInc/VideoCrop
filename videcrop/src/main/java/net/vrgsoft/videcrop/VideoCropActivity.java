package net.vrgsoft.videcrop;


import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Rect;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.window.layout.WindowMetrics;
import androidx.window.layout.WindowMetricsCalculator;

import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.arthenica.mobileffmpeg.ExecuteCallback;
import com.arthenica.mobileffmpeg.FFmpeg;
import com.bumptech.glide.Glide;
import com.google.android.exoplayer2.util.Util;

import net.vrgsoft.videcrop.cropview.window.CropVideoView;
import net.vrgsoft.videcrop.player.VideoPlayer;
import net.vrgsoft.videcrop.view.ProgressView;
import net.vrgsoft.videcrop.view.rangeslider.VideoRangeSeekBar;

import java.io.File;
import java.util.Formatter;
import java.util.Locale;


public class VideoCropActivity extends AppCompatActivity implements VideoPlayer.OnProgressUpdateListener {
    private static final String VIDEO_CROP_INPUT_PATH = "VIDEO_CROP_INPUT_PATH";
    private static final String VIDEO_CROP_OUTPUT_PATH = "VIDEO_CROP_OUTPUT_PATH";
    private static final int STORAGE_REQUEST = 100;

    // ffmpeg return codes
    private static final int RETURN_CODE_SUCCESS = 0;
    private static final int RETURN_CODE_CANCEL = 255;

    private VideoPlayer mVideoPlayer;
    private StringBuilder formatBuilder;
    private Formatter formatter;

    private AppCompatImageView mIvPlay;
    private AppCompatImageView mIvDone;
    private VideoRangeSeekBar videoRangeSeekBar;
    private CropVideoView mCropVideoView;
    private TextView mTvDuration;
    private ImageView preview;
    private ProgressBar mProgressBar;

    private String inputPath;
    private String outputPath;
    private boolean isVideoPlaying = false;
    private long ffmpegSessionId = -1;
    MediaMetadataRetriever retriever;

    private long totalDuration = 0;
    private int aspectRatioX = 10;
    private int aspectRatioY = 10;
    private float maximumDuration = 10000f;
    private float minmumDuration = 3000f;
    private WindowMetrics windowMetrics;

    public static Intent createIntent(Context context, String inputPath, String outputPath) {
        Intent intent = new Intent(context, VideoCropActivity.class);
        intent.putExtra(VIDEO_CROP_INPUT_PATH, inputPath);
        intent.putExtra(VIDEO_CROP_OUTPUT_PATH, outputPath);
        return intent;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_crop);

        formatBuilder = new StringBuilder();
        formatter = new Formatter(formatBuilder, Locale.getDefault());

        inputPath = getIntent().getStringExtra(VIDEO_CROP_INPUT_PATH);
        outputPath = getIntent().getStringExtra(VIDEO_CROP_OUTPUT_PATH);

        if (TextUtils.isEmpty(inputPath) || TextUtils.isEmpty(outputPath)) {
            Toast.makeText(this, "input and output paths must be valid and not null", Toast.LENGTH_SHORT).show();
            setResult(RESULT_CANCELED);
            finish();
        }

        findViews();

        this.windowMetrics = WindowMetricsCalculator.getOrCreate().computeCurrentWindowMetrics(this);
        this.aspectRatioX = windowMetrics.getBounds().width() / 4;
        this.aspectRatioY = windowMetrics.getBounds().width() / 4;
        mCropVideoView.setFixedAspectRatio(true);
        mCropVideoView.setAspectRatio(aspectRatioX, aspectRatioY);
        mTvDuration.setText(Util.getStringForTime(formatBuilder, formatter, (long) maximumDuration));
        initListeners();

        requestStoragePermission();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case STORAGE_REQUEST: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    initPlayer(inputPath);
                } else {
                    Toast.makeText(this, "You must grant a write storage permission to use this functionality", Toast.LENGTH_SHORT).show();
                    setResult(RESULT_CANCELED);
                    finish();
                }
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (isVideoPlaying) {
            mVideoPlayer.play(true);
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        mVideoPlayer.play(false);
    }

    @Override
    public void onDestroy() {
        if (retriever != null) retriever.release();
        if (mVideoPlayer != null) mVideoPlayer.release();
        if (ffmpegSessionId >= 0) FFmpeg.cancel(ffmpegSessionId);
        super.onDestroy();
    }

    @Override
    public void onFirstTimeUpdate(long duration, long currentPosition) {
        this.totalDuration = duration;
        videoRangeSeekBar.setVideoSource(this, Uri.fromFile(new File(inputPath)));
        videoRangeSeekBar.setMinProgressDiff(minmumDuration / (float) duration);
        videoRangeSeekBar.setMaxProgressDiff(maximumDuration / (float) duration);
        videoRangeSeekBar.setOnVideoRangeSeekBarListener(new VideoRangeSeekBar.VideoRangeSeekBarListener() {

            @Override
            public void onLeftProgressChanged(float leftProgress) {
                long value = (long) (videoRangeSeekBar.getLeftProgress() * ((float) totalDuration));
                long value2 = (long) (videoRangeSeekBar.getRightProgress() * ((float) totalDuration));
                mVideoPlayer.seekTo(value);
                mTvDuration.setText(Util.getStringForTime(formatBuilder, formatter, value2 - value));
            }

            @Override
            public void onRightProgressChanged(float rightProgress) {
                long value = (long) (videoRangeSeekBar.getLeftProgress() * ((float) totalDuration));
                long value2 = (long) (videoRangeSeekBar.getRightProgress() * ((float) totalDuration));
                mVideoPlayer.seekTo(value);
                mTvDuration.setText(Util.getStringForTime(formatBuilder, formatter, value2 - value));

            }

            @Override
            public void onPlayProgressChanged(float progress) {

            }

            @Override
            public void didStartDragging() {

            }

            @Override
            public void didStopDragging() {

            }
        });
    }

    @Override
    public void onProgressUpdate(long currentPosition, long duration, long bufferedPosition) {
        if (mVideoPlayer.isPlaying()) {
            preview.setVisibility(View.GONE);
        }
        long value2 = (long) (videoRangeSeekBar.getRightProgress() * ((float) totalDuration));

        if (!mVideoPlayer.isPlaying() || currentPosition >= value2) {
            if (mVideoPlayer.isPlaying()) {
                playPause();
            }
        }

    }

    private void findViews() {
        preview = findViewById(R.id.preview);
        mCropVideoView = findViewById(R.id.cropVideoView);
        mIvPlay = findViewById(R.id.ivPlay);
        mIvDone = findViewById(R.id.ivDone);
        mTvDuration = findViewById(R.id.tvDuration);
        videoRangeSeekBar = findViewById(R.id.videoRangeSeekBar);
        mProgressBar = findViewById(R.id.pbCropProgress);
    }

    private void initListeners() {
        mIvPlay.setOnClickListener(v -> playPause());
        mIvDone.setOnClickListener(v -> handleCropStart());
    }

    private void playPause() {
        isVideoPlaying = !mVideoPlayer.isPlaying();
        if (mVideoPlayer.isPlaying()) {
            mVideoPlayer.play(!mVideoPlayer.isPlaying());
            mIvPlay.setImageResource(R.drawable.ic_play);
            return;
        }
        long value = (long) (videoRangeSeekBar.getLeftProgress() * ((float) totalDuration));

        mVideoPlayer.seekTo(value);
        mVideoPlayer.play(!mVideoPlayer.isPlaying());
        mIvPlay.setImageResource(R.drawable.ic_pause);
    }

    private void initPlayer(String uri) {
        if (!new File(uri).exists()) {
            Toast.makeText(this, "File doesn't exists", Toast.LENGTH_SHORT).show();
            setResult(RESULT_CANCELED);
            finish();
            return;
        }

        mVideoPlayer = new VideoPlayer(this);
        mCropVideoView.setPlayer(mVideoPlayer.getPlayer());
        mVideoPlayer.initMediaSource(this, uri);
        mVideoPlayer.setUpdateListener(this);


        fetchVideoInfo(uri);
    }

    private void fetchVideoInfo(String uri) {
        retriever = new MediaMetadataRetriever();
        retriever.setDataSource(new File(uri).getAbsolutePath());
        int videoWidth = Integer.parseInt(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH));
        int videoHeight = Integer.parseInt(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT));
        int rotationDegrees = Integer.parseInt(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION));
        Glide.with(preview).load(retriever.getFrameAtTime()).into(preview);
        mCropVideoView.initBounds(videoWidth, videoHeight, rotationDegrees);
    }

    private void requestStoragePermission() {
        if ((ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) || (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE}, STORAGE_REQUEST);
        } else {
            initPlayer(inputPath);
        }
    }

    @SuppressLint("DefaultLocale")
    private void handleCropStart() {
        Rect cropRect = mCropVideoView.getCropRect();
        long startCrop = (long) (videoRangeSeekBar.getLeftProgress() * ((float) totalDuration));
        long durationCrop = (long) (videoRangeSeekBar.getRightProgress() * ((float) totalDuration)) - startCrop;
        String start = Util.getStringForTime(formatBuilder, formatter, startCrop);
        String duration = Util.getStringForTime(formatBuilder, formatter, durationCrop);
        start += "." + startCrop % 1000;
        duration += "." + durationCrop % 1000;
        String crop = String.format("crop=%d:%d:%d:%d", cropRect.right, cropRect.bottom, cropRect.left, cropRect.top);
        String command = String.format("-y -ss %s -i \"%s\" -t %s -vf \"%s\" %s", start, inputPath, duration, crop, outputPath);
        mProgressBar.setVisibility(View.VISIBLE);
        mIvPlay.setVisibility(View.INVISIBLE);
        mIvDone.setEnabled(false);
        mIvPlay.setEnabled(false);

        ffmpegSessionId = FFmpeg.executeAsync(command, new ExecuteCallback() {
            @Override
            public void apply(final long executionId, final int returnCode) {
                if (returnCode == RETURN_CODE_SUCCESS) {
                runOnUiThread(() -> {
                    mProgressBar.setVisibility(View.INVISIBLE);
                    mIvPlay.setVisibility(View.VISIBLE);
                    setResult(RESULT_OK);
                    finish();
                });
                } else if (returnCode == RETURN_CODE_CANCEL) {
                runOnUiThread(() -> {
                    mProgressBar.setVisibility(View.INVISIBLE);
                    mIvPlay.setVisibility(View.VISIBLE);
                    mIvDone.setEnabled(true);
                    mIvPlay.setEnabled(true);
                });
                } else {
                runOnUiThread(() -> {
                    mProgressBar.setVisibility(View.INVISIBLE);
                    mIvPlay.setVisibility(View.VISIBLE);
                    mIvDone.setEnabled(true);
                    mIvPlay.setEnabled(true);
                    Toast.makeText(VideoCropActivity.this, "Failed to crop!", Toast.LENGTH_SHORT).show();
                });
                }
            }
        });

    }

}
