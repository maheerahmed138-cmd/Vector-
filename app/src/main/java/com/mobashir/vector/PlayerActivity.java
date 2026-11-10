package com.mobashir.vector;

import android.app.Activity;
import android.os.Bundle;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.graphics.Color;

import androidx.media3.common.MediaItem;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.PlayerView;

public class PlayerActivity extends Activity {

    public static final String EXTRA_VIDEO_URL = "com.mobashir.vector.VIDEO_URL";
    public static final String EXTRA_VIDEO_TITLE = "com.mobashir.vector.VIDEO_TITLE";

    private ExoPlayer player;
    private PlayerView playerView;
    private String videoUrl;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        videoUrl = getIntent().getStringExtra(EXTRA_VIDEO_URL);
        String videoTitle = getIntent().getStringExtra(EXTRA_VIDEO_TITLE);

        if (videoUrl == null || videoUrl.trim().isEmpty()) {
            Toast.makeText(this, "Video link missing", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        getWindow().setStatusBarColor(Color.rgb(11, 18, 32));
        getWindow().setNavigationBarColor(Color.BLACK);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.rgb(11, 18, 32));

        TextView titleView = new TextView(this);
        titleView.setText(videoTitle == null || videoTitle.trim().isEmpty()
                ? "VECTOR Player" : videoTitle);
        titleView.setTextColor(Color.WHITE);
        titleView.setTextSize(16);
        titleView.setGravity(Gravity.CENTER_VERTICAL);
        titleView.setPadding(16, 12, 16, 12);
        root.addView(titleView, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        playerView = new PlayerView(this);
        playerView.setUseController(true);
        playerView.setShowBuffering(PlayerView.SHOW_BUFFERING_ALWAYS);
        playerView.setKeepContentOnPlayerReset(true);
        playerView.setBackgroundColor(Color.BLACK);

        LinearLayout.LayoutParams playerParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0);
        playerParams.weight = 1;
        root.addView(playerView, playerParams);

        setContentView(root);
    }

    @Override
    protected void onStart() {
        super.onStart();
        initializePlayer();
    }

    private void initializePlayer() {
        if (player != null || videoUrl == null) return;

        player = new ExoPlayer.Builder(this).build();
        playerView.setPlayer(player);

        player.addListener(new Player.Listener() {
            @Override
            public void onPlayerError(PlaybackException error) {
                Toast.makeText(PlayerActivity.this,
                        "Video playback failed. Link may be expired or unsupported.",
                        Toast.LENGTH_LONG).show();
            }
        });

        MediaItem mediaItem = MediaItem.fromUri(videoUrl);
        player.setMediaItem(mediaItem);
        player.prepare();
        player.play();
    }

    @Override
    protected void onStop() {
        super.onStop();
        releasePlayer();
    }

    private void releasePlayer() {
        if (player != null) {
            playerView.setPlayer(null);
            player.release();
            player = null;
        }
    }
}
