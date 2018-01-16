/*
 * Copyright (C) 2013 www.yaacc.de 
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package de.yaacc.player;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.PixelFormat;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import android.view.SurfaceView;
import android.widget.SeekBar;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

import de.yaacc.R;
import de.yaacc.musicplayer.BackgroundMusicBroadcastReceiver;
import de.yaacc.musicplayer.BackgroundMusicService;
import de.yaacc.settings.SettingsActivity;
import de.yaacc.upnp.server.contentdirectory.SrtDiscovery;
import de.yaacc.upnp.server.contentdirectory.YaaccContentDirectory;
import de.yaacc.util.AboutActivity;
import de.yaacc.util.YaaccLogActivity;

import static de.yaacc.musicplayer.BackgroundMusicBroadcastReceiver.*;

/**
 * A multi content player activity based on the multi content player.
 *
 * @author Tobias Schoene (openbit)
 */
public class MultiContentPlayerActivity extends Activity implements SurfaceHolder.Callback,ServiceConnection {

    MediaPlayer mediaPlayer;
    SurfaceView surfaceView;
    SurfaceHolder surfaceHolder;
    boolean pausing = false;
    private Handler threadHandler = new Handler();
    private SeekBar seekBar;
    int currentPosition=0;
    private SrtDiscovery backgroundSrtDiscoveryService;
    String filename="";
     Uri myUri=null;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_multi_content_player);
        // initialize buttons
        Log.d("BZA", "Multi Content Player oncreate");

        getWindow().setFormat(PixelFormat.UNKNOWN);
        surfaceView = (SurfaceView)findViewById(R.id.surfaceview);
        surfaceHolder = surfaceView.getHolder();
        surfaceHolder.addCallback(this);
        surfaceHolder.setFixedSize(176, 144);
        surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        mediaPlayer = new MediaPlayer();
        Player player = getPlayer();
        Intent i = getIntent();

        myUri = Uri.parse(i.getExtras().getString("de.yaacc.musicplayer.ActionSetDataUriParam"));
        filename= i.getExtras().getString("FileName");
        Log.d("BZA", "Multi Content Player myUri FileName" + myUri + filename);



        ImageButton btnPrev = (ImageButton) findViewById(R.id.multiContentPlayerActivityControlPrev);
        ImageButton btnNext = (ImageButton) findViewById(R.id.multiContentPlayerActivityControlNext);
        ImageButton btnStop = (ImageButton) findViewById(R.id.multiContentPlayerActivityControlStop);
        ImageButton btnPlay = (ImageButton) findViewById(R.id.multiContentPlayerActivityControlPlay);
        ImageButton btnPause = (ImageButton) findViewById(R.id.multiContentPlayerActivityControlPause);
        ImageButton btnExit = (ImageButton) findViewById(R.id.multiContentPlayerActivityControlExit);
        this.seekBar= (SeekBar) this.findViewById(R.id.multiActivitySeekBar);
        seekBar.setMax(100);
        seekBar.setProgress(0);

                     Log.d("Hii", "Hello Damaru " + currentPosition);

                    seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

                        @Override
                        @SuppressLint("SimpleDateFormat")
                        public  void onStopTrackingTouch(android.widget.SeekBar seekBar){
                            String durationString = getDuration();
                            SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");
                            dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
                            try {
                                Long durationTimeMillis = dateFormat.parse(durationString).getTime();

                                int targetPosition = Double.valueOf(durationTimeMillis * (Double.valueOf(seekBar.getProgress()).doubleValue() / 100)).intValue();
                                int calculte= (int)( (targetPosition * 100)  / mediaPlayer.getDuration());
                                Log.d("Hi","TargetPosition calculte" + targetPosition +" " +calculte);
                                /*mediaPlayer.pause();
                                mediaPlayer.start();
                                mediaPlayer.seekTo(targetPosition);
                                seekBar.setProgress(calculte);*/

                            }catch(ParseException pex){
                                Log.d("Hi","Error while parsing time string" , pex);
                            }

                        }



                        @Override
                        public void onStartTrackingTouch(SeekBar seekBar) {
                            Log.d("Hii", "Hello runOnUiThread onStartTrackingTouch " + currentPosition);

                        }

                        @Override
                        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                            // TODO Auto-generated method stub
                            if(fromUser){
                                Log.d("Hii", "Hello runOnUiThread onProgressChanged " + progress);
                                // mediaPlayer.seekTo(progress);
                                int calculations = mediaPlayer.getDuration();
                                int value = (int) ((progress * calculations )/100);
                                int currentPosition = mediaPlayer.getCurrentPosition();
                                Log.d("Hii", "Hello runOnUiThread calculations currentPosition" + calculations + " " + value +" " +currentPosition );
                                //mediaPlayer.seekTo(value);
                                //seekBar.setProgress(progress);
                            }
                        }
                    });





        this.seekBar.setClickable(true);

        if (player == null) {
            btnPrev.setActivated(false);
            btnNext.setActivated(false);
            btnStop.setActivated(false);
            btnPlay.setActivated(false);
            btnPause.setActivated(false);
            btnExit.setActivated(false);
        } else {
            btnPrev.setActivated(true);
            btnNext.setActivated(true);
            btnStop.setActivated(true);
            btnPlay.setActivated(true);
            btnPause.setActivated(true);
            btnExit.setActivated(true);
        }


        btnPrev.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                Player player = getPlayer();
                int currentPosition = mediaPlayer.getCurrentPosition();
                int duration = mediaPlayer.getDuration();
                // 5 seconds.
                int SUBTRACT_TIME = 5000;

                if(currentPosition - SUBTRACT_TIME > 0 )  {
                    mediaPlayer.seekTo(currentPosition - SUBTRACT_TIME);
                }
                if (player != null) {
                   // player.previous();
                }

            }
        });



        btnNext.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                Player player = getPlayer();
                int currentPosition = mediaPlayer.getCurrentPosition();
                int duration = mediaPlayer.getDuration();
                // 5 seconds.
                int ADD_TIME = 5000;

                if(currentPosition + ADD_TIME < duration) {
                    mediaPlayer.seekTo(currentPosition + ADD_TIME);
                }
                if (player != null) {
                 //   player.next();
                }

            }
        });


        btnPlay.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                Player player = getPlayer();
                pausing = false;
                int duration = mediaPlayer.getDuration();

                int currentPosition = mediaPlayer.getCurrentPosition();
                if(currentPosition== 0)  {
                    seekBar.setMax(duration);
                    String maxTimeString = millisecondsToString(duration);

                }
                if(mediaPlayer.isPlaying()) {
                    mediaPlayer.reset();


                    mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
                    mediaPlayer.setDisplay(surfaceHolder);

                    try {
                        mediaPlayer.setDataSource(getApplicationContext(), myUri);
                        mediaPlayer.prepareAsync();
                    } catch (IllegalArgumentException e) {
                        e.printStackTrace();
                    } catch (IllegalStateException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    mediaPlayer.start();
                }


                if (player != null) {
                   // player.play();
                }

            }//onclick ends
        });



        btnPause.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                Player player = getPlayer();
                if(pausing){
                    pausing = false;
                    mediaPlayer.start();
                    backgroundSrtDiscoveryService.resume();
                }
                else{
                    pausing = true;
                    mediaPlayer.pause();
                    backgroundSrtDiscoveryService.onPauseStop();
                }
                if (player != null) {
                   // player.pause();
                }

            }
        });
        btnStop.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                mediaPlayer.stop();
                Player player = getPlayer();
                if (player != null) {
                    player.stop();
                }

            }
        });
        btnExit.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                Player player = getPlayer();
                if (player != null) {
                    player.exit();
                }
                finish();

            }
        });

        this.startService(new Intent(this, SrtDiscovery.class));
        this.bindService(new Intent(this, SrtDiscovery.class), MultiContentPlayerActivity.this, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
        Log.d("BZA", "Multi Content Player onServiceConnected" +
                "");
        backgroundSrtDiscoveryService = ((SrtDiscovery.SRTServiceBinder) iBinder).getService();
        backgroundSrtDiscoveryService.fileplayback(filename);
    }

    @Override
    public void onServiceDisconnected(ComponentName componentName) {

    }

    public String getElapsedTime() {
        //if (!isMusicServiceBound()) return "";
        return formatMillis(mediaPlayer.getCurrentPosition());
    }

    @SuppressLint("SimpleDateFormat")
    private String formatMillis(long millis) {

        StringBuffer buf = new StringBuffer();

        int hours = (int) (millis / (1000 * 60 * 60));
        int minutes = (int) ((millis % (1000 * 60 * 60)) / (1000 * 60));
        int seconds = (int) (((millis % (1000 * 60 * 60)) % (1000 * 60)) / 1000);

        buf
                .append(String.format("%02d", hours))
                .append(":")
                .append(String.format("%02d", minutes))
                .append(":")
                .append(String.format("%02d", seconds));

        return buf.toString();


    }

    class UpdateSeekBarThread implements Runnable {

        public void run()  {
             currentPosition = mediaPlayer.getCurrentPosition();
            String currentPositionStr = millisecondsToString(currentPosition);
            Log.d("UpdateSeekBarThread", "Hello currentPosition " + currentPosition);
            backgroundSrtDiscoveryService.onTimePost(currentPosition);
            String elapsedTimeString = getElapsedTime();
            String durationString = getDuration();
           // Log.d("Hi", "Hello elapsedTimeString durationString " + elapsedTimeString + " " + durationString);
            SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");
            dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
            Long elapsedTimeMillis = null;
            Long durationTimeMillis = null;
            try {
                elapsedTimeMillis = dateFormat.parse(elapsedTimeString).getTime();
                 durationTimeMillis = dateFormat.parse(durationString).getTime();
            } catch (ParseException e) {
                e.printStackTrace();
            }
            //Log.d("Hi", "Hello elapsedTimeString durationTimeMillis.doubleValue() " + Double.valueOf(elapsedTimeMillis.doubleValue()).intValue() +" " + Double.valueOf(durationTimeMillis.doubleValue()).intValue());
              //seekBar.setProgress(currentPosition);
            int progress;

            progress = (int)(((Double.valueOf(elapsedTimeMillis.doubleValue()).intValue() * 100)/  (Double.valueOf(durationTimeMillis.doubleValue()).intValue())) );
            //Log.d("Hi", "Hello currentPosition " + progress);
            if(seekBar != null) {
                seekBar.setProgress(progress);
            }




            // Delay thread 50 milisecond.
            threadHandler.postDelayed(this, 50);
        }
    }

    // Convert millisecond to string.
    private String millisecondsToString(int milliseconds)  {
        long minutes = TimeUnit.MILLISECONDS.toMinutes((long) milliseconds);
        long seconds =  TimeUnit.MILLISECONDS.toSeconds((long) milliseconds) ;
        return minutes+":"+ seconds;
    }
    private Player getPlayer() {
        Log.d("BZA", "Multi Content Player getPlayer");
        ////return PlayerFactory
              //  .getFirstCurrentPlayerOfType(MultiContentPlayer.class);
        return PlayerFactory.getFirstCurrentPlayerOfType(LocalBackgoundMusicPlayer.class);
    }

    public String getDuration() {
        //if (!isMusicServiceBound()) return "";
        return formatMillis(mediaPlayer.getDuration());

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_multi_content_player, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_settings:
                Intent i = new Intent(this, SettingsActivity.class);
                startActivity(i);
                return true;
            case R.id.yaacc_about:
                AboutActivity.showAbout(this);
                return true;
            case R.id.yaacc_log:
                YaaccLogActivity.showLog(this);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        Log.d("BZA", "Multi Content Player surfaceCreated");
        mediaPlayer.setDisplay(surfaceHolder);
        int duration = this.mediaPlayer.getDuration();
        Log.d("Hii", "Multi Duration" + duration);
        int currentPosition = this.mediaPlayer.getCurrentPosition();
        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);

        try {
            mediaPlayer.setDataSource(getApplicationContext(), myUri);
            mediaPlayer.prepareAsync();
            // You can show progress dialog here untill it prepared to play
            mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mp) {
                    // Now dismis progress dialog, Media palyer will start playing
                     mp.start();
                }
            });
            mediaPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
                @Override
                public boolean onError(MediaPlayer mp, int what, int extra) {
                    // dissmiss progress bar here. It will come here when
                    // MediaPlayer
                    // is not able to play file. You can show error message to user
                    Log.d("Hii", "setOnErrorListener" + what);
                    return false;
                }
            });
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (IllegalStateException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        //mediaPlayer.start();
        UpdateSeekBarThread updateSeekBarThread= new UpdateSeekBarThread();
        threadHandler.postDelayed(updateSeekBarThread,50);

        if(currentPosition== 0)  {
            seekBar.setMax(duration);
            seekBar.setProgress(0);
            String maxTimeString = millisecondsToString(duration);

        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
        mediaPlayer.reset();
    }
}
