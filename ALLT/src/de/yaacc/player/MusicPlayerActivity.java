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

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.net.URI;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.TimeZone;
import java.util.Timer;
import java.util.TimerTask;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import org.fourthline.cling.support.model.DIDLObject;

import de.yaacc.R;
import de.yaacc.musicplayer.BackgroundMusicBroadcastReceiver;
import de.yaacc.settings.SettingsActivity;
import de.yaacc.util.AboutActivity;
import de.yaacc.util.YaaccLogActivity;
import de.yaacc.util.image.ImageDownloadTask;

/**
 * A music player activity based on a background music service.
 *
 * @author Tobias Schoene (openbit)
 */
public class MusicPlayerActivity extends Activity {

    protected boolean updateTime = false;
    protected SeekBar seekBar = null;

    @Override
    protected void onPause() {
        super.onPause();
        updateTime = false;
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        updateTime = true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        setTrackInfo();
        updateTime = true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        updateTime = false;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_music_player);
        // initialize buttons
        Player player = getPlayer();
        ImageButton btnPrev = (ImageButton) findViewById(R.id.musicActivityControlPrev);
        ImageButton btnNext = (ImageButton) findViewById(R.id.musicActivityControlNext);
        ImageButton btnStop = (ImageButton) findViewById(R.id.musicActivityControlStop);
        ImageButton btnPlay = (ImageButton) findViewById(R.id.musicActivityControlPlay);
        ImageButton btnPause = (ImageButton) findViewById(R.id.musicActivityControlPause);
        ImageButton btnExit = (ImageButton) findViewById(R.id.musicActivityControlExit);
        if (player == null) {
            btnPrev.setActivated(false);
            btnNext.setActivated(false);
            btnStop.setActivated(false);
            btnPlay.setActivated(false);
            btnPause.setActivated(false);
            btnExit.setActivated(false);
        } else {
            player.addPropertyChangeListener(new PropertyChangeListener() {

                @Override
                public void propertyChange(PropertyChangeEvent event) {
                    if (LocalBackgoundMusicPlayer.PROPERTY_ITEM.equals(event.getPropertyName())) {
                        runOnUiThread(new Runnable() {
                            public void run() {
                                Log.d("BZA", "Music Player property change listener");
                                setTrackInfo();
                            }
                        });

                    }

                }
            });
            setTrackInfo();
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
                if (player != null) {
                    player.previous();
                }

            }
        });
        btnNext.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                Player player = getPlayer();
                if (player != null) {
                    player.next();
                }

            }
        });
        btnPlay.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                Player player = getPlayer();
                if (player != null) {
                    player.play();
                }

            }
        });
        btnPause.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                Player player = getPlayer();
                if (player != null) {
                    player.pause();
                }

            }
        });
        btnStop.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
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

        seekBar = (SeekBar)findViewById(R.id.musicActivitySeekBar);
        seekBar.setMax(100);
        seekBar.setProgress(0);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progresValue, boolean fromUser) {
            }

            @Override
            public  void onStartTrackingTouch(android.widget.SeekBar seekBar){

            }

            @Override
            @SuppressLint("SimpleDateFormat")
            public  void onStopTrackingTouch(android.widget.SeekBar seekBar){
                String durationString = getPlayer().getDuration();
                SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");
                dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
                try {
                    Long durationTimeMillis = dateFormat.parse(durationString).getTime();

                    int targetPosition = Double.valueOf(durationTimeMillis * (Double.valueOf(seekBar.getProgress()).doubleValue() / 100)).intValue();
                    Log.d(getClass().getName(),"TargetPosition" + targetPosition);
                    getPlayer().seekTo(targetPosition);
                }catch(ParseException pex){
                    Log.d(getClass().getName(),"Error while parsing time string" , pex);
                }

            }

        });

    }

    private Player getPlayer() {
        return PlayerFactory.getFirstCurrentPlayerOfType(LocalBackgoundMusicPlayer.class);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_music_player, menu);

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

    private void setTrackInfo() {
        Log.d("BZA", "setTrackInfo");
        doSetTrackInfo();
        updateTime();
    }
    @SuppressLint("SimpleDateFormat")
    private void doSetTrackInfo() {
        Log.d("BZA", "doSetTrackInfo");
        if (getPlayer() == null)
            return;
        TextView current = (TextView) findViewById(R.id.musicActivityCurrentItem);
        current.setText(getPlayer().getCurrentItemTitle());
        TextView position = (TextView) findViewById(R.id.musicActivityPosition);
        position.setText(getPlayer().getPositionString());
        TextView next = (TextView) findViewById(R.id.musicActivityNextItem);
        next.setText(getPlayer().getNextItemTitle());
        ImageView albumArtView = (ImageView) findViewById(R.id.musicActivityImageView);
        URI albumArtUri = getPlayer().getAlbumArt();
        if (null != albumArtUri) {
            ImageDownloadTask imageDownloadTask = new ImageDownloadTask(albumArtView);
            imageDownloadTask.execute(Uri.parse(albumArtUri.toString()));
        }
        TextView duration = (TextView) findViewById(R.id.musicActivityDuration);
        duration.setText(getPlayer().getDuration());
        TextView elapsedTime = (TextView) findViewById(R.id.musicActivityElapsedTime);
        String elapsedTimeString = getPlayer().getElapsedTime();
        elapsedTime.setText(elapsedTimeString);
        String durationString = getPlayer().getDuration();
        SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");
        dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
        try {
            Long elapsedTimeMillis = dateFormat.parse(elapsedTimeString).getTime();
            Long durationTimeMillis = dateFormat.parse(durationString).getTime();
            int progress;
            progress = Double.valueOf((elapsedTimeMillis.doubleValue()/  durationTimeMillis.doubleValue()) *100).intValue();
            if(seekBar != null) {
                seekBar.setProgress(progress);
            }
        }catch(ParseException pex){
            Log.d(getClass().getName(),"Error while parsing time string" , pex);
        }


    }

    private void updateTime() {
        Timer commandExecutionTimer = new Timer();
        commandExecutionTimer.schedule(new TimerTask() {

            @Override
            public void run() {
                runOnUiThread(new Runnable() {

                    @Override
                    public void run() {
                        doSetTrackInfo();
                        if (updateTime) {
                            updateTime();
                        }
                    }
                });
            }
        }, 1000L);

    }
}
