package com.cajor.dk.dlna;

import android.content.Context;
import android.os.Bundle;
import android.app.Activity;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import org.fourthline.cling.controlpoint.SubscriptionCallback;
import org.fourthline.cling.model.UnsupportedDataException;
import org.fourthline.cling.model.gena.CancelReason;
import org.fourthline.cling.model.gena.GENASubscription;
import org.fourthline.cling.model.gena.RemoteGENASubscription;
import org.fourthline.cling.model.message.UpnpResponse;
import org.fourthline.cling.model.meta.Service;
import org.fourthline.cling.model.state.StateVariableValue;

import java.util.Map;
import java.util.regex.Pattern;

public class WorkerActivity extends Activity {
    public static ListenerThread r =null;
    public static int toastduration=0;
    public static TextView textElement;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_worker);
        textElement = (TextView) findViewById(R.id.this_is_id_name);
        if(r == null) {
            Log.d("BZA", "LooperThread r is null started ");
            r  = new ListenerThread(this);
            Thread t = new Thread(r );
            t.start();
        }
    }


    public class ListenerThread implements Runnable {
        Context context;
        ListenerThread(Context ctx){
            context = ctx;
        }

        private Toast mToastToShow;
        public void run() {
            //	Looper.prepare();
            Log.d("ZAA", "LooperThread started ");
            int i = 0;
            Service service = MainActivity.mCurrentDevice.getContentDirectory(); //getProviderDevice().findService(new UDAServiceId("ContentDirectory"));
            final SubscriptionCallback callback = new SubscriptionCallback(service, 50) {

                @Override
                public void established(GENASubscription sub) {
                    Log.d("BZA", "established: ");

                }

                @Override
                protected void failed(GENASubscription subscription,
                                      UpnpResponse responseStatus,
                                      Exception exception,
                                      String defaultMsg) {
                    System.err.println(defaultMsg);
                    Log.d("BZA", "failed: ");
                }

                @Override
                public void ended(GENASubscription sub,
                                  CancelReason reason,
                                  UpnpResponse response) {
                    // assertNull(reason);
                }

                @Override
                public void eventReceived(GENASubscription sub) {

                    Log.d("BZA", "eventReceived: ");

                    Map<String, StateVariableValue> values = sub.getCurrentValues();
                    final StateVariableValue status = values.get("Status");

                    Log.d("BZA", "eventReceived: 123 status1 " + status.toString() );
                    if(!status.toString().equalsIgnoreCase("false")) {
                        toastduration = Integer.parseInt(status.toString().split(Pattern.quote("|"))[1]);
                        int toastDurationInMilliSeconds = (toastduration * 1000);
                        //Toast.makeText(context, status.toString().split(Pattern.quote("$"))[0], toastDurationInMilliSeconds);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                textElement.setText(status.toString().split(Pattern.quote("|"))[0]);
                            }
                        });




                        // Set the countdown to display the toast
                        // CountDownTimer toastCountDown;
                        // toastCountDown = new CountDownTimer(toastDurationInMilliSeconds, 1000 /*Tick duration*/) {
                        //////    public void onTick(long millisUntilFinished) {
                        //    mToastToShow.show();
                        // }

                          /*  public void onFinish() {
                                mToastToShow.cancel();
                            }
                        };*/

                        // Show the toast and starts the countdown
                        // mToastToShow.show();
                        // toastCountDown.start();
                    }
                }

                @Override
                public void eventsMissed(GENASubscription sub, int numberOfMissedEvents) {
                    Log.d("BZA", "eventsMissed: ");
                }

                @Override
                protected void invalidMessage(RemoteGENASubscription sub,
                                              UnsupportedDataException ex) {
                    // Log/send an error report?
                }
            };


            ContentDirectoryBrowseTaskFragment.mService.getControlPoint().execute(callback);
        }
    }



}
