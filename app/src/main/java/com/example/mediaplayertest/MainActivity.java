package com.example.mediaplayertest;

import android.Manifest;
import android.app.Activity;
import android.app.Service;
import android.content.Context;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.SimpleFormatter;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private MediaPlayer mediaPlayer = new MediaPlayer();
    private TextView musicLength,musicCur;
    private SeekBar seekBar;

    private AudioManager audioManager;

    private Timer timer;

    private boolean isSeekBarChanging;//互斥变量，防止进度条与定时器冲突。
    private int currentPosition;//当前音乐播放的进度

    SimpleDateFormat format;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        audioManager = (AudioManager) getSystemService(Service.AUDIO_SERVICE);
        format = new SimpleDateFormat("mm:ss");

        Button play = (Button) findViewById(R.id.play);
        Button pause = (Button) findViewById(R.id.pause);
        Button stop = (Button) findViewById(R.id.stop);


        musicLength = (TextView) findViewById(R.id.music_length);
        musicCur = (TextView) findViewById(R.id.music_cur);

        seekBar = (SeekBar) findViewById(R.id.seekBar);
        seekBar.setOnSeekBarChangeListener(new MySeekBar());

        play.setOnClickListener(this);
        pause.setOnClickListener(this);
        stop.setOnClickListener(this);
        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
        }else {
            initMediaPlayer();
        }
    }

    private void initMediaPlayer() {
        try{
//            File file = new File(Environment.getExternalStorageDirectory(),"千里行走.mp3");
//            mediaPlayer.setDataSource(file.getPath());

            mediaPlayer.setDataSource("http://10.0.2.2:8080/test/起风了.mp3");
            mediaPlayer.prepare();
            mediaPlayer.setLooping(true);
            mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                public void onPrepared(MediaPlayer mp) {
                    String time = Integer.toString(mediaPlayer.getCurrentPosition());
                    Log.d("时长", time);
                    seekBar.setMax(mediaPlayer.getDuration());
                    musicLength.setText(format.format(mediaPlayer.getDuration())+"");
                    musicCur.setText("00:00");
                }
            });

        } catch (Exception e){
            e.printStackTrace();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode){
            case 1:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    initMediaPlayer();
                } else {
                    Toast.makeText(this, "拒绝将导致程序无法使用", Toast.LENGTH_SHORT).show();
                    finish();
                }
                break;
            default:
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.play:
                if (!mediaPlayer.isPlaying()){
                    mediaPlayer.start(); //开始播放
//                    mediaPlayer.seekTo(currentPosition);

                    //监听播放时回调函数
                    timer = new Timer();
                    timer.schedule(new TimerTask() {

                        Runnable updateUI = new Runnable() {
                            @Override
                            public void run() {
                                musicCur.setText(format.format(mediaPlayer.getCurrentPosition())+"");
                            }
                        };
                        @Override
                        public void run() {
                            if(!isSeekBarChanging){
                                seekBar.setProgress(mediaPlayer.getCurrentPosition());
                                runOnUiThread(updateUI);
                            }
                        }
                    },0,50);

                }
                break;
            case R.id.pause:
                if (mediaPlayer.isPlaying()){
                    mediaPlayer.pause();
                }

                break;
            case R.id.stop:
                if (mediaPlayer.isPlaying()){
                    mediaPlayer.reset();
                    initMediaPlayer();
                }
                break;
            default:
                break;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null){
            mediaPlayer.stop();
            mediaPlayer.release();
        }
        if (timer != null){
            timer.cancel();
            timer = null;
        }
    }
    //进度条事件处理
    public class MySeekBar implements SeekBar.OnSeekBarChangeListener {

        public void onProgressChanged(SeekBar seekBar, int progress,
                                      boolean fromUser) {
        }

        /*滚动时,应当暂停后台定时器*/
        public void onStartTrackingTouch(SeekBar seekBar) {
            isSeekBarChanging = true;
        }
        /*滑动结束后，重新设置值*/
        public void onStopTrackingTouch(SeekBar seekBar) {
            isSeekBarChanging = false;
            mediaPlayer.seekTo(seekBar.getProgress());
        }
    }

}
