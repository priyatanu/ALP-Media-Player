package de.yaacc.upnp.server.contentdirectory;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Binder;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.Log;

import org.fourthline.cling.binding.annotations.UpnpService;
import org.fourthline.cling.binding.annotations.UpnpServiceId;
import org.fourthline.cling.binding.annotations.UpnpServiceType;
import org.fourthline.cling.binding.annotations.UpnpStateVariable;
import org.fourthline.cling.model.meta.Device;
import org.fourthline.cling.model.types.UDN;
import org.fourthline.cling.support.model.BrowseFlag;
import org.fourthline.cling.support.model.SortCriterion;
import org.seamless.util.Reflections;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import de.yaacc.R;
import de.yaacc.player.AVTransportPlayer;
import de.yaacc.player.Player;
import de.yaacc.player.PlayerFactory;
import de.yaacc.player.SyncAVTransportPlayer;

import static de.yaacc.upnp.UpnpClient.LOCAL_UID;

/**
 * Created by Priyatanu on 1/1/2018.
 */

//@UpnpService(serviceId = @UpnpServiceId("SrtDiscovery"), serviceType = @UpnpServiceType(value = "SrtDiscovery", version = 1))

public class SrtDiscovery extends Service implements SRTListener {
    //@UpnpStateVariable(sendEvents = false)
    private String data;
    DBHelper db;
    Cursor cursor=null;
    public IBinder binder = new SRTServiceBinder();
    YaaccContentDirectory handlerThread =null;
    private Handler workerHandler;
    SharedPreferences preferences;
    String tablename="";
    boolean ispausestop=false;
    static  String oldvalue="";

    public SrtDiscovery() {
        super();
    }

    public SrtDiscovery(Context context){
            data="";
        Log.d("ZAA", "SrtDiscovery " );
        db= new DBHelper(context);
        try {
            db.createDataBase();
            Player player = getPlayer();

            SrtProcessor s = new SrtProcessor(context);
            Thread t = new Thread(s);
            t.start();

             preferences = PreferenceManager.getDefaultSharedPreferences(context);

        }catch (Exception e){

        }
    }

    @Override
    public void onTimePost(int time)  {
        int timepos= time/1000;
        Log.d("UpdateSeekBarThread", "SRT Discovery " + time);
      if(tablename == null || tablename.isEmpty()){
          return;
      }
      if(ispausestop){
    Message m = new Message();
    m.obj = " ";
    m.arg1 = 0;
    YaaccContentDirectory.mHandler.sendMessage(m);
          return;
     }
        Log.d("UpdateSeekBarThread", "SRT Discovery next line " + time);
        Cursor cursor = DBHelper.checkDB.rawQuery("select * from " + tablename + " where absoultetime "  + " = '" + timepos + "'"  , null);
        //Log.d("Hola", "Hello cursor tablename " + tablename + cursor.getCount());
        if (cursor.moveToFirst()) {
            while (!cursor.isAfterLast()) {
                String abtime = cursor.getString(cursor.getColumnIndex("value"));

                int val= Integer.parseInt(cursor.getString(cursor.getColumnIndex("diff")));
                //Log.d("Hola", "Hello heirrkr val" + abtime + val);
                // list.add(name);
                cursor.moveToNext();


                try {
                    Log.d("ZAA", "Hello heirrkr " + abtime);
                    if(!oldvalue.equals(abtime)) {
                        oldvalue = abtime;
                        Message m = new Message();
                        m.obj = abtime;
                        m.arg1 = val;
                        YaaccContentDirectory.mHandler.sendMessage(m);
                        break;
                    }
                }catch (Exception e){
                    Log.d("Hola", "Hello heirrkr " + e.toString());
                }
            }
        }

    }

    @Override
    public void onPauseStop() {

        ispausestop = true;
    }

    @Override
    public void resume() {
        ispausestop= false;
    }

    @Override
    public void fileplayback(String file) {
        Log.d("Hola", "SrtDiscovery fileplayback "+ file + SrtProcessor.tablenames.length);
        for(int i=0; i < SrtProcessor.tablenames.length; i ++){
            if(SrtProcessor.tablenames[i].contains(file.split(Pattern.quote("."))[0])){
                tablename = file.split(Pattern.quote("."))[0];
                Log.d("Hola", "SrtDiscovery file match ");

            }
        }
    }

    @Override
    public void valuechanged(String value) {

    }

    public Device<?, ?, ?> getProviderDevice() {
        return this.getDevice(getProviderDeviceId());
    }

    public String getProviderDeviceId() {
        //return preferences.getString(contex.getString(R.string.settings_selected_provider_title), null);
        return null;
    }
    public Device<?, ?, ?> getDevice(String identifier) {
        if (LOCAL_UID.equals(identifier)) {
            //return getLocalDummyDevice();
        }
        if (isInitialized()) {
           // return getRegistry().getDevice(new UDN(identifier), true);
        }
        return null;
    }

    public boolean isInitialized() {
        return true;
    }

    public class SRTServiceBinder extends Binder {
        public SrtDiscovery getService() {
            return SrtDiscovery.this;
        }
    }

    private Player getPlayer() {
        Player result = null;
        List<Player> players = new ArrayList<Player>();
        players.addAll(PlayerFactory
                .getCurrentPlayersOfType(AVTransportPlayer.class));
        players.addAll(PlayerFactory
                .getCurrentPlayersOfType(SyncAVTransportPlayer.class));
        if (players != null) { // assume that there
            for (Player player : players) {
                Log.d(getClass().getName(), "Found networkplayer: " + player.getId() + " Searched  for id: " + -1);
                if (player.getId() == -1) {
                    result = player;
                    break;
                }
            }
        }
        return result;
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.d("BZA", "On Bind");
        return binder;
    }

    /*
     * (non-Javadoc)
     *
     * @see android.app.Service#onStart(android.content.Intent, int)
     */
    @Override
    public void onStart(Intent intent, int startid) {
        Log.d("BZA", "On Start");
        //initialize(intent);
    }

}
