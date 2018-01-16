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

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

import android.app.Activity;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import javax.xml.datatype.Duration;

import de.yaacc.R;
import de.yaacc.upnp.SynchronizationInfo;
import de.yaacc.upnp.UpnpClient;

/**
 * @author Tobias Schoene (openbit)
 */
public abstract class AbstractPlayer implements Player {

    public static final String PROPERTY_ITEM = "item";
    private List<PlayableItem> items = new ArrayList<PlayableItem>();
    private int previousIndex = 0;
    private int currentIndex = 0;
    private Timer playerTimer;
    private Timer execTimer;
    private boolean isPlaying = false;
    private boolean isProcessingCommand = false;

    private UpnpClient upnpClient;
    private String name;


    private final PropertyChangeSupport pcs = new PropertyChangeSupport(this);
    private SynchronizationInfo syncInfo;
    private boolean paused;
    private Object loadedItem = null;
    private int currentLoadedIndex = -1;

    /**
     * @param upnpClient
     */
    public AbstractPlayer(UpnpClient upnpClient) {
        super();
        this.upnpClient = upnpClient;
    }

    /**
     * @return the context
     */
    public Context getContext() {
        return upnpClient.getContext();
    }

    /**
     * @return the upnpClient
     */
    public UpnpClient getUpnpClient() {
        return upnpClient;
    }

    /*
     * (non-Javadoc)
     *
     * @see de.yaacc.player.Player#next()
     */
    @Override
    public void next() {
        if (isProcessingCommand()) {
            return;
        }
        setProcessingCommand(true);


        paused = false;
        previousIndex = currentIndex;
        cancelTimer();
        currentIndex++;
        if (currentIndex > items.size() - 1) {
            currentIndex = 0;
            SharedPreferences preferences = PreferenceManager
                    .getDefaultSharedPreferences(getContext());
            boolean replay = preferences.getBoolean(
                    getContext().getString(
                            R.string.settings_replay_playlist_chkbx), true);
            if (!replay) {
                stop();
                return;
            }

        }
        Context context = getUpnpClient().getContext();
        if (context instanceof Activity) {
            ((Activity) context).runOnUiThread(new Runnable() {
                public void run() {
                    Toast toast = Toast.makeText(getContext(), getContext()
                            .getResources().getString(R.string.next)
                            + getPositionString(), Toast.LENGTH_SHORT);

                    toast.show();
                }
            });
        }
        setProcessingCommand(false);
        Log.e("BZA", "Abstract Player Play after next");
        play();

    }

    //

    /*
     * (non-Javadoc)
     *
     * @see de.yaacc.player.Player#previous()
     */
    @Override
    public void previous() {
        if (isProcessingCommand()) {
            return;
        }
        setProcessingCommand(true);

        paused = false;
        previousIndex = currentIndex;
        cancelTimer();
        currentIndex--;
        if (currentIndex < 0) {
            if (items.size() > 0) {
                currentIndex = items.size() - 1;
            } else {
                currentIndex = 0;
            }
        }
        Context context = getUpnpClient().getContext();
        if (context instanceof Activity) {
            ((Activity) context).runOnUiThread(new Runnable() {
                public void run() {
                    Toast toast = Toast.makeText(getContext(), getContext()
                            .getResources().getString(R.string.previous)
                            + getPositionString(), Toast.LENGTH_SHORT);
                    toast.show();
                }
            });
        }
        setProcessingCommand(false);
        play();

    }

    /*
     * (non-Javadoc)
     *
     * @see de.yaacc.player.Player#pause()
     */
    @Override
    public void pause() {
        if (isProcessingCommand())
            return;
        setProcessingCommand(true);
        executeCommand(new TimerTask() {
            @Override
            public void run() {
                cancelTimer();
                Context context = getUpnpClient().getContext();
                if (context instanceof Activity) {
                    ((Activity) context).runOnUiThread(new Runnable() {
                        public void run() {
                            Toast toast = Toast.makeText(getContext(), getContext()
                                    .getResources().getString(R.string.pause)
                                    + getPositionString(), Toast.LENGTH_SHORT);
                            toast.show();
                        }
                    });
                }
                isPlaying = false;
                paused = true;
                doPause();
                setProcessingCommand(false);
            }
        }, getExecutionTime());
    }

    /*
     * (non-Javadoc)
     *
     * @see de.yaacc.player.Player#start()
     */
    @Override
    public void play() {
        Log.d("BZA", "Abstract Player play()");
        if (isProcessingCommand())
            return;
        setProcessingCommand(true);
        int possibleNextIndex = currentIndex;
        if (possibleNextIndex >= 0 && possibleNextIndex < items.size()) {
            loadItem(possibleNextIndex);
        }
        executeCommand(new TimerTask() {
            @Override
            public void run() {
                if (currentIndex < items.size()) {
                    Context context = getUpnpClient().getContext();
                    if (context instanceof Activity) {
                        ((Activity) context).runOnUiThread(new Runnable() {
                            public void run() {
                                Toast toast = Toast.makeText(getContext(), getContext()
                                        .getResources().getString(R.string.play)
                                        + getPositionString(), Toast.LENGTH_SHORT);
                                toast.show();
                            }
                        });
                    }
                    isPlaying = true;
                    if (paused) {
                        doResume();
                    } else {
                        paused = false;
                        Log.d("BZA", "Abstract Player play() before loadItem");
                        loadItem(previousIndex, currentIndex);
                    }
                    setProcessingCommand(false);
                }
            }
        }, getExecutionTime());


    }

    /*
     * (non-Javadoc)
     *
     * @see de.yaacc.player.Player#stop()
     */
    @Override
    public void stop() {
        if (isProcessingCommand())
            return;
        setProcessingCommand(true);
        currentLoadedIndex = -1;
        loadedItem = null;
        executeCommand(new TimerTask() {
            @Override
            public void run() {
                cancelTimer();
                currentIndex = 0;
                Context context = getUpnpClient().getContext();
                if (context instanceof Activity) {
                    ((Activity) context).runOnUiThread(new Runnable() {
                        public void run() {
                            Toast toast = Toast.makeText(getContext(), getContext()
                                    .getResources().getString(R.string.stop)
                                    + getPositionString(), Toast.LENGTH_SHORT);
                            toast.show();
                        }
                    });
                }
                if (items.size() > 0) {
                    stopItem(items.get(currentIndex));
                }
                isPlaying = false;
                paused = false;
                setProcessingCommand(false);
            }
        }, getExecutionTime());
    }

    /*
     * (non-Javadoc)
     *
     * @see de.yaacc.player.Player#setItems(de.yaacc.player.PlayableItem[])
     */
    @Override
    public void setItems(PlayableItem... playableItems) {
        Log.e("BZA", "AVTransportPlayer setItems after");
        List<PlayableItem> itemsList = Arrays.asList(playableItems);

        if (isShufflePlay()) {
            Collections.shuffle(itemsList);
        }
        items.addAll(itemsList);
        showNotification();
    }


    /**
     * is shuffle play enabled.
     *
     * @return true, if shuffle play is enabled
     */

    protected boolean isShufflePlay() {
        return false;
    }

    /*
     * (non-Javadoc)
     *
     * @see de.yaacc.player.Player#clear()
     */
    @Override
    public void clear() {
        items.clear();
    }

    protected void cancelTimer() {
        if (playerTimer != null) {
            playerTimer.cancel();
        }
    }

    public boolean isPlaying() {
        return isPlaying;
    }

    public void setPlaying(boolean isPlaying) {
        this.isPlaying = isPlaying;
    }


    public int getCurrentIndex() {
        return currentIndex;
    }

    public void setCurrentIndex(int currentIndex) {
        this.currentIndex = currentIndex;
    }

    public List<PlayableItem> getItems() {
        return items;
    }

    /**
     * returns the current item position in the playlist
     *
     * @return the position string
     */
    public String getPositionString() {
        return " (" + (currentIndex + 1) + "/" + items.size() + ")";
    }

    /**
     * returns the title of the current item
     *
     * @return the title
     */
    public String getCurrentItemTitle() {
        String result = "";
        if (currentIndex < items.size()) {

            result = items.get(currentIndex).getTitle();
        }
        return result;
    }

    /**
     * returns the title of the next current item
     *
     * @return the title
     */
    public String getNextItemTitle() {
        String result = "";
        if (currentIndex + 1 < items.size()) {

            result = items.get(currentIndex + 1).getTitle();
        }
        return result;
    }


    protected Object loadItem(int toLoadIndex) {
        if (toLoadIndex == currentLoadedIndex && loadedItem != null) {
            Log.d("BZA", "returning already loaded item");
            return loadedItem;
        }
        if (toLoadIndex >= 0 && toLoadIndex <= items.size()) {
            Log.d("BZA", "loaded item" +toLoadIndex );
            currentLoadedIndex = toLoadIndex;
            loadedItem = loadItem(items.get(toLoadIndex));
            return loadedItem;
        }
        return null;
    }

    protected void loadItem(int previousIndex, int nextIndex) {
        Log.d("BZA", "Abstract Player loadItem");
        if (items == null || items.size() == 0) {
            Log.d("BZA", "items = null");
            return;
        }
        PlayableItem playableItem = items.get(nextIndex);
        Object loadedItem = loadItem(nextIndex);
        firePropertyChange(PROPERTY_ITEM, items.get(previousIndex),
                items.get(nextIndex));
        Log.d("BZA", "startItem before");
        startItem(playableItem, loadedItem);
        if (isPlaying() && items.size() > 1) {
            if(playableItem.getDuration() > -1) {
                //Only start timer if automatic track change is active
                startTimer(playableItem.getDuration() + getSilenceDuration());
            }
        }
    }

    protected void doPause() {
        //default do nothing
    }

    protected void doResume() {
        //default replay current item
        paused = false;
        Log.d("BZA", "Abstract Player loadItem() before doResume");

        loadItem(currentIndex, currentIndex);
    }

    /**
     * returns the duration between two items
     *
     * @return duration in millis
     */
    protected long getSilenceDuration() {
        return upnpClient.getSilenceDuration();
    }

    /**
     * Start a timer for the next item change
     *
     * @param duration in millis
     */
    public void startTimer(final long duration) {
        Log.d(getClass().getName(), "Start timer duration: " + duration);
        if (playerTimer != null) {
            cancelTimer();
        }
        playerTimer = new Timer();
        playerTimer.schedule(new TimerTask() {

            @Override
            public void run() {
                Log.d(getClass().getName(), "TimerEvent" + this);
                AbstractPlayer.this.next();

            }
        }, duration);

    }

    /*
     * (non-Javadoc)
     *
     * @see de.yaacc.player.Player#setName(java.lang.String)
     */
    @Override
    public void setName(String name) {
        this.name = name;

    }

    /*
     * (non-Javadoc)
     *
     * @see de.yaacc.player.Player#getName()
     */
    @Override
    public String getName() {

        return name;
    }

    public boolean isProcessingCommand() {
        return isProcessingCommand;
    }

    public void setProcessingCommand(boolean isProcessingCommand) {
        this.isProcessingCommand = isProcessingCommand;
    }

    /*
             * (non-Javadoc)
             *
             * @see de.yaacc.player.Player#exit()
             */
    @Override
    public void exit() {
        if(isPlaying()){

            stop();
        }
        PlayerFactory.shutdown(this);

    }

    /**
     * Displays the notification.
     */
    private void showNotification() {

        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(
                getContext()).setOngoing(false)
                .setSmallIcon(R.drawable.ic_launcher)
                .setContentTitle("Yaacc player")
                .setContentText(getName() == null ? "" : getName());
        PendingIntent contentIntent = getNotificationIntent();
        if (contentIntent != null) {
            mBuilder.setContentIntent(contentIntent);
        }
        NotificationManager mNotificationManager = (NotificationManager) getContext()
                .getSystemService(Context.NOTIFICATION_SERVICE);
        // mId allows you to update the notification later on.
        mNotificationManager.notify(getNotificationId(), mBuilder.build());
    }

    /**
     * Cancels the notification.
     */
    private void cancleNotification() {
        NotificationManager mNotificationManager = (NotificationManager) getContext()
                .getSystemService(Context.NOTIFICATION_SERVICE);
        // mId allows you to update the notification later on.
        Log.d(getClass().getName(), "Cancle Notification with ID: " + getNotificationId());
        mNotificationManager.cancel(getNotificationId());

    }

    /**
     * Returns the notification id of the player
     *
     * @return
     */
    protected int getNotificationId() {

        return 0;
    }

    /**
     * Returns the intent which is to be started by pushing the notification
     * entry
     *
     * @return the peneding intent
     */
    public PendingIntent getNotificationIntent() {
        return null;
    }

    protected abstract void stopItem(PlayableItem playableItem);

    protected abstract Object loadItem(PlayableItem playableItem);

    protected abstract void startItem(PlayableItem playableItem,
                                      Object loadedItem);

    /*
     * (non-Javadoc)
     *
     * @see de.yaacc.player.Player#onDestroy()
     */
    @Override
    public void onDestroy() {
        stop();
        cancleNotification();
        items.clear();

    }

    /*
     * (non-Javadoc)
     *
     * @see de.yaacc.player.Player#getId()
     */
    @Override
    public int getId() {
        return getNotificationId();
    }

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        this.pcs.addPropertyChangeListener(listener);
    }

    public void removePropertyChangeListener(PropertyChangeListener listener) {
        this.pcs.removePropertyChangeListener(listener);
    }

    protected void firePropertyChange(String property, Object oldValue,
                                      Object newValue) {
        this.pcs.firePropertyChange(property, oldValue, newValue);
    }

    @Override
    public String getDuration() {
        return "";
    }

    @Override
    public String getElapsedTime() {
        return "";
    }


    @Override
    public URI getAlbumArt() {
        return null;
    }

    @Override
    public void setSyncInfo(SynchronizationInfo syncInfo) {
        if (syncInfo == null) {
            syncInfo = new SynchronizationInfo();
        }
        this.syncInfo = syncInfo;
    }

    @Override
    public SynchronizationInfo getSyncInfo() {
        return syncInfo;
    }

    protected Date getExecutionTime() {
        Calendar execTime = Calendar.getInstance(Locale.getDefault());
        execTime.set(Calendar.HOUR_OF_DAY, getSyncInfo().getReferencedPresentationTimeOffset().getHour());
        execTime.set(Calendar.MINUTE, getSyncInfo().getReferencedPresentationTimeOffset().getMinute());
        execTime.set(Calendar.SECOND, getSyncInfo().getReferencedPresentationTimeOffset().getSecond());
        execTime.set(Calendar.MILLISECOND, getSyncInfo().getReferencedPresentationTimeOffset().getMillis());
        execTime.add(Calendar.HOUR, getSyncInfo().getOffset().getHour());
        execTime.add(Calendar.MINUTE, getSyncInfo().getOffset().getMinute());
        execTime.add(Calendar.SECOND, getSyncInfo().getOffset().getSecond());
        execTime.add(Calendar.MILLISECOND, getSyncInfo().getOffset().getMillis());
        Log.d(getClass().getName(), "ReferencedRepresentationTimeOffset: " + getSyncInfo().getReferencedPresentationTimeOffset());
        Log.d(getClass().getName(), "current time: " + new Date().toString() + " get execution time: " + execTime.getTime().toString());
        if (execTime.getTime().getTime() <= System.currentTimeMillis()){
            Log.d(getClass().getName(), "ExecutionTime is in past!! We will start immediately");
            execTime = Calendar.getInstance(Locale.getDefault());
            execTime.add(Calendar.MILLISECOND, 100);

        }
        return execTime.getTime();
    }

    protected void executeCommand(TimerTask command, Date executionTime) {
        if (execTimer != null) {
            execTimer.cancel();
        }
        Timer execTimer = new Timer();
        execTimer.schedule(command, executionTime);
    }

    public boolean getMute(){
        return upnpClient.isMute();
    }


    public void setMute(boolean mute){
        upnpClient.setMute(mute);
    }

    public void setVolume(int volume){
        upnpClient.setVolume(volume);
    }

    public int getVolume(){
        return upnpClient.getVolume();
    }

    public int getIconResourceId(){

        return R.drawable.ic_launcher;
    }

    public String getDeviceId() {
        return UpnpClient.LOCAL_UID;
    }

    public abstract void seekTo(long millisecondsFromStart);

}
