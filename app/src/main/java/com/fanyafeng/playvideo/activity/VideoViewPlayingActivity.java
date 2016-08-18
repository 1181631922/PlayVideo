package com.fanyafeng.playvideo.activity;

import android.content.Context;
import android.content.res.ObbInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.os.Process;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.baidu.cyberplayer.core.BVideoView;
import com.baidu.cyberplayer.core.BVideoView.OnCompletionListener;
import com.baidu.cyberplayer.core.BVideoView.OnErrorListener;
import com.baidu.cyberplayer.core.BVideoView.OnInfoListener;
import com.baidu.cyberplayer.core.BVideoView.OnPreparedListener;
import com.baidu.cyberplayer.core.BVideoView.OnPlayingBufferCacheListener;

import com.fanyafeng.playvideo.R;
import com.fanyafeng.playvideo.BaseActivity;
import com.fanyafeng.playvideo.util.FitScreenUtil;
import com.fanyafeng.playvideo.util.MyUtils;

import java.net.FileNameMap;

public class VideoViewPlayingActivity extends BaseActivity implements OnPreparedListener, OnCompletionListener,
        OnErrorListener, OnInfoListener, OnPlayingBufferCacheListener {
    private BVideoView videoView;
    private ImageButton btnPre;
    private ImageButton btnPlay;
    private ImageButton btnNext;
    private TextView tvCurrentTime;
    private SeekBar seekBarProgress;
    private TextView tvTotalTime;
    private LinearLayout layoutVideoHolder;

    private static final String POWER_LOCK = "VideoViewPlayingActivity";
    private String AK = "b7a8758a1e1c4884aa8def8e038d2994";

    //记录播放位置
    private int lastPosition = 0;
    private String videoSource;

    //播放状态
    private enum PLAYER_STATUS {
        PLAYER_IDLE, PLAYER_PREPARING, PLAYER_PREPARED,
    }

    private PLAYER_STATUS playerStatus = PLAYER_STATUS.PLAYER_IDLE;

    private EventHandler eventHandler;
    private HandlerThread handlerThread;

    private final Object SYNC_Playing = new Object();

    private PowerManager.WakeLock wakeLock;

    private boolean isHwDecode;

    private final int EVENT_PLAY = 0;
    private final int UI_EVENT_UPDATE_CURRPOSITION = 1;

    class EventHandler extends Handler {

        public EventHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case EVENT_PLAY:
                    if (playerStatus != PLAYER_STATUS.PLAYER_IDLE) {
                        try {
                            SYNC_Playing.wait();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    videoView.setVideoPath(videoSource);
                    if (lastPosition > 0) {
                        videoView.seekTo(lastPosition);
                        lastPosition = 0;
                    }
                    videoView.showCacheInfo(false);
                    videoView.start();
                    playerStatus = PLAYER_STATUS.PLAYER_PREPARING;
                    break;
            }
        }
    }

    Handler uiHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case UI_EVENT_UPDATE_CURRPOSITION:
                    int currPosition = videoView.getCurrentPosition();
                    int duration = videoView.getDuration();
                    updateTextViewWithTimeFormat(tvCurrentTime, currPosition);
                    updateTextViewWithTimeFormat(tvTotalTime, duration);
                    seekBarProgress.setMax(duration);
                    if (videoView.isPlaying()) {
                        seekBarProgress.setProgress(currPosition);
                    }
                    uiHandler.sendEmptyMessageDelayed(UI_EVENT_UPDATE_CURRPOSITION, 200);
                    break;
            }
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_view_playing);
        title = getString(R.string.title_activity_video_view_playing);

        PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.FULL_WAKE_LOCK | PowerManager.ON_AFTER_RELEASE, POWER_LOCK);

        isHwDecode = getIntent().getBooleanExtra("isHW", false);
//        Uri uri = getIntent().getData();
        Uri uri = Uri.parse("http://www.jmzsjy.com/UploadFile/%E5%BE%AE%E8%AF%BE/%E5%9C%B0%E6%96%B9%E9%A3%8E%E5%91%B3%E5%B0%8F%E5%90%83%E2%80%94%E2%80%94%E5%AE%AB%E5%BB%B7%E9%A6%99%E9%85%A5%E7%89%9B%E8%82%89%E9%A5%BC.mp4");
        if (uri != null) {
            String scheme = uri.getScheme();
            if (scheme != null) {
                videoSource = uri.toString();
            } else {
                videoSource = uri.getPath();
            }
        }

        initView();
        initData();
    }

    private void initView() {

        layoutVideoHolder = (LinearLayout) findViewById(R.id.layoutVideoHolder);
        int screenWidth = MyUtils.getScreenWidth(this);
        int height = screenWidth / 4 * 3;

        FitScreenUtil.FixScreenXY(layoutVideoHolder, screenWidth, height);

        BVideoView.setAK(AK);
        videoView = (BVideoView) findViewById(R.id.videoView);

        btnPre = (ImageButton) findViewById(R.id.btnPre);
        btnPlay = (ImageButton) findViewById(R.id.btnPlay);
        btnNext = (ImageButton) findViewById(R.id.btnNext);
        tvCurrentTime = (TextView) findViewById(R.id.tvCurrentTime);
        seekBarProgress = (SeekBar) findViewById(R.id.seekBarProgress);
        tvTotalTime = (TextView) findViewById(R.id.tvTotalTime);

        registerCallbackForControl();

        videoView.setOnPreparedListener(this);
        videoView.setOnCompletionListener(this);
        videoView.setOnErrorListener(this);
        videoView.setOnInfoListener(this);

        videoView.setDecodeMode(isHwDecode ? BVideoView.DECODE_HW : BVideoView.DECODE_SW);
//        videoView.setWatermarkText("测试滚动字幕");内存占用相当明显
    }

    private void initData() {
        handlerThread = new HandlerThread("event handler thread", Process.THREAD_PRIORITY_BACKGROUND);
        handlerThread.start();
        eventHandler = new EventHandler(handlerThread.getLooper());
    }

    @Override
    public void onCompletion() {
        synchronized (SYNC_Playing) {
            SYNC_Playing.notify();
        }
        playerStatus = PLAYER_STATUS.PLAYER_IDLE;
        uiHandler.removeMessages(UI_EVENT_UPDATE_CURRPOSITION);
    }

    @Override
    public boolean onError(int what, int extra) {
        synchronized (SYNC_Playing) {
            SYNC_Playing.notify();
        }
        playerStatus = PLAYER_STATUS.PLAYER_IDLE;
        uiHandler.removeMessages(UI_EVENT_UPDATE_CURRPOSITION);
        return true;
    }

    @Override
    public boolean onInfo(int what, int extra) {
        switch (what) {
            case BVideoView.MEDIA_INFO_BUFFERING_START://开始缓冲
                break;
            case BVideoView.MEDIA_INFO_BUFFERING_END://结束缓冲
                break;
        }
        return true;
    }

    @Override
    public void onPlayingBufferCache(int i) {

    }

    @Override
    public void onPrepared() {
        playerStatus = PLAYER_STATUS.PLAYER_PREPARED;
        uiHandler.sendEmptyMessage(UI_EVENT_UPDATE_CURRPOSITION);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (playerStatus == PLAYER_STATUS.PLAYER_PREPARED) {
            lastPosition = videoView.getCurrentPosition();
            videoView.stopPlayback();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (wakeLock != null && (!wakeLock.isHeld())) {
            wakeLock.acquire();
        }

        eventHandler.sendEmptyMessage(EVENT_PLAY);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handlerThread.quit();
    }


    private void registerCallbackForControl() {
        btnPlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (videoView.isPlaying()) {
                    btnPlay.setImageResource(R.drawable.play_btn_style);
                    videoView.pause();
                } else {
                    btnPlay.setImageResource(R.drawable.pause_btn_style);
                    videoView.resume();
                }
            }
        });

        btnPre.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (playerStatus != PLAYER_STATUS.PLAYER_IDLE) {
                    videoView.stopPlayback();
                }
                if (eventHandler.hasMessages(EVENT_PLAY)) {
                    eventHandler.removeMessages(EVENT_PLAY);
                }
                eventHandler.sendEmptyMessage(EVENT_PLAY);
            }
        });

        btnNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

            }
        });

        SeekBar.OnSeekBarChangeListener onSeekBarChangeListener = new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                updateTextViewWithTimeFormat(tvCurrentTime, i);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                uiHandler.removeMessages(UI_EVENT_UPDATE_CURRPOSITION);
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                int seekPos = seekBar.getProgress();
                videoView.seekTo(seekPos);
            }
        };

        seekBarProgress.setOnSeekBarChangeListener(onSeekBarChangeListener);
    }

    private void updateTextViewWithTimeFormat(TextView textView, int second) {
        int hh = second / 3600;
        int mm = second % 3600 / 60;
        int ss = second % 60;
        String stringTemp = null;
        if (0 != hh) {
            stringTemp = String.format("%02d:%02d:%02d", hh, mm, ss);
        } else {
            stringTemp = String.format("%02d:%02d", mm, ss);
        }
        textView.setText(stringTemp);
    }
}
