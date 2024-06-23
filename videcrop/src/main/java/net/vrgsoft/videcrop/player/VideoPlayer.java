package net.vrgsoft.videcrop.player;

import static androidx.media3.common.C.TIME_UNSET;

import android.content.Context;
import android.os.Handler;


import androidx.annotation.OptIn;
import androidx.media3.common.MediaItem;
import androidx.media3.common.PlaybackParameters;
import androidx.media3.common.Player;
import androidx.media3.common.Timeline;
import androidx.media3.common.VideoSize;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.datasource.FileDataSource;
import androidx.media3.exoplayer.DefaultRenderersFactory;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.source.MediaSource;
import androidx.media3.exoplayer.source.ProgressiveMediaSource;
import androidx.media3.ui.TimeBar;

@OptIn(markerClass = UnstableApi.class)
public class VideoPlayer implements Player.Listener, TimeBar.OnScrubListener {

    private ExoPlayer player;
    private OnProgressUpdateListener mUpdateListener;
    private Handler progressHandler;
    private Runnable progressUpdater;

    public VideoPlayer(Context context) {
        DefaultRenderersFactory renderersFactory = new DefaultRenderersFactory(context);
        renderersFactory.setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_OFF);
        player =  new ExoPlayer.Builder(context, renderersFactory).build();
        player.setRepeatMode(Player.REPEAT_MODE_ONE);
        player.addListener(this);
        progressHandler = new Handler();
    }

    public void initMediaSource(Context context, String uri) {
        MediaSource videoSource = new ProgressiveMediaSource.Factory(new FileDataSource.Factory()).createMediaSource(MediaItem.fromUri(uri));
        player.addMediaSource(videoSource);
        player.prepare();
        player.addListener(this);
    }

    public ExoPlayer getPlayer() {
        return player;
    }

    public void play(boolean play) {
        player.setPlayWhenReady(play);
        if (!play) {
            removeUpdater();
        }
    }

    public void release() {
        player.release();
        removeUpdater();
        player = null;
    }

    public boolean isPlaying() {
        return player.getPlayWhenReady();
    }

    @Override
    public void onTimelineChanged(Timeline timeline, int reason) {
        Player.Listener.super.onTimelineChanged(timeline, reason);
        updateProgress();

    }

    @Override
    public void onPlaybackStateChanged(int playbackState) {
        Player.Listener.super.onPlaybackStateChanged(playbackState);
        updateProgress();

    }

    @Override
    public void onRepeatModeChanged(int repeatMode) {

    }

    @Override
    public void onShuffleModeEnabledChanged(boolean shuffleModeEnabled) {

    }

    @Override
    public void onPositionDiscontinuity(Player.PositionInfo oldPosition, Player.PositionInfo newPosition, int reason) {
        Player.Listener.super.onPositionDiscontinuity(oldPosition, newPosition, reason);
        updateProgress();
    }

    @Override
    public void onPlaybackParametersChanged(PlaybackParameters playbackParameters) {

    }


    @Override
    public void onScrubStart(TimeBar timeBar, long position) {

    }

    @Override
    public void onScrubMove(TimeBar timeBar, long position) {
        seekTo(position);
        updateProgress();
    }

    @Override
    public void onScrubStop(TimeBar timeBar, long position, boolean canceled) {
        seekTo(position);
        updateProgress();
    }

    private void updateProgress() {
        if (mUpdateListener != null) {
            mUpdateListener.onProgressUpdate(
                    player.getCurrentPosition(),
                    player.getDuration() == TIME_UNSET ? 0L : player.getDuration(),
                    player.getBufferedPosition());
        }
        initUpdateTimer();
    }

    private void initUpdateTimer() {
        long position = player.getCurrentPosition();
        int playbackState = player.getPlaybackState();
        long delayMs;
        if (playbackState != ExoPlayer.STATE_IDLE && playbackState != ExoPlayer.STATE_ENDED) {
            if (player.getPlayWhenReady() && playbackState == ExoPlayer.STATE_READY) {
                delayMs = 1000 - (position % 1000);
                if (delayMs < 200) {
                    delayMs += 1000;
                }
            } else {
                delayMs = 1000;
            }

            removeUpdater();
            progressUpdater = new Runnable() {
                @Override
                public void run() {
                    updateProgress();
                }
            };

            progressHandler.postDelayed(progressUpdater, delayMs);
        }
    }

    private void removeUpdater() {
        if (progressUpdater != null)
            progressHandler.removeCallbacks(progressUpdater);
    }

    public void seekTo(long position) {
        player.seekTo(position);
    }

    public void setUpdateListener(OnProgressUpdateListener updateListener) {
        mUpdateListener = updateListener;
    }

    @Override
    public void onVideoSizeChanged(VideoSize videoSize) {
        Player.Listener.super.onVideoSizeChanged(videoSize);
        if(mUpdateListener != null){
            mUpdateListener.onFirstTimeUpdate(player.getDuration(), player.getCurrentPosition());
        }
    }


    @Override
    public void onRenderedFirstFrame() {

    }

    public interface OnProgressUpdateListener {
        void onProgressUpdate(long currentPosition, long duration, long bufferedPosition);

        void onFirstTimeUpdate(long duration, long currentPosition);
    }
}
