/*
 *
 * Copyright (C) 2013 www.yaacc.de
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 */
package de.yaacc.upnp;

import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.Resources;
import android.media.AudioManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;
import android.webkit.MimeTypeMap;
import android.widget.Toast;

import org.fourthline.cling.UpnpService;
import org.fourthline.cling.UpnpServiceConfiguration;
import org.fourthline.cling.android.AndroidUpnpService;
import org.fourthline.cling.controlpoint.ControlPoint;
import org.fourthline.cling.controlpoint.SubscriptionCallback;
import org.fourthline.cling.model.Namespace;
import org.fourthline.cling.model.UnsupportedDataException;
import org.fourthline.cling.model.ValidationException;
import org.fourthline.cling.model.gena.CancelReason;
import org.fourthline.cling.model.gena.GENASubscription;
import org.fourthline.cling.model.gena.RemoteGENASubscription;
import org.fourthline.cling.model.message.UpnpResponse;
import org.fourthline.cling.model.meta.Action;
import org.fourthline.cling.model.meta.Device;
import org.fourthline.cling.model.meta.DeviceDetails;
import org.fourthline.cling.model.meta.DeviceIdentity;
import org.fourthline.cling.model.meta.Icon;
import org.fourthline.cling.model.meta.LocalDevice;
import org.fourthline.cling.model.meta.RemoteDevice;
import org.fourthline.cling.model.meta.Service;
import org.fourthline.cling.model.meta.StateVariable;
import org.fourthline.cling.model.meta.UDAVersion;
import org.fourthline.cling.model.resource.Resource;
import org.fourthline.cling.model.state.StateVariableValue;
import org.fourthline.cling.model.types.BooleanDatatype;
import org.fourthline.cling.model.types.Datatype;
import org.fourthline.cling.model.types.DeviceType;
import org.fourthline.cling.model.types.ServiceId;
import org.fourthline.cling.model.types.ServiceType;
import org.fourthline.cling.model.types.UDAServiceId;
import org.fourthline.cling.model.types.UDAServiceType;
import org.fourthline.cling.model.types.UDN;
import org.fourthline.cling.registry.Registry;
import org.fourthline.cling.registry.RegistryListener;
import org.fourthline.cling.support.contentdirectory.DIDLParser;
import org.fourthline.cling.support.contentdirectory.callback.Browse.Status;
import org.fourthline.cling.support.model.BrowseFlag;
import org.fourthline.cling.support.model.DIDLContent;
import org.fourthline.cling.support.model.DIDLObject;
import org.fourthline.cling.support.model.PositionInfo;
import org.fourthline.cling.support.model.Res;
import org.fourthline.cling.support.model.SortCriterion;
import org.fourthline.cling.support.model.container.Container;
import org.fourthline.cling.support.model.item.AudioItem;
import org.fourthline.cling.support.model.item.ImageItem;
import org.fourthline.cling.support.model.item.Item;

import java.net.URI;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import de.yaacc.R;
import de.yaacc.browser.Position;
import de.yaacc.imageviewer.ImageViewerActivity;
import de.yaacc.musicplayer.BackgroundMusicService;
import de.yaacc.player.PlayableItem;
import de.yaacc.player.Player;
import de.yaacc.player.PlayerFactory;
import de.yaacc.upnp.callback.contentdirectory.ContentDirectoryBrowseActionCallback;
import de.yaacc.upnp.callback.contentdirectory.ContentDirectoryBrowseResult;
import de.yaacc.upnp.model.types.SyncOffset;
import de.yaacc.upnp.server.YaaccUpnpServerService;
import de.yaacc.upnp.server.avtransport.AvTransport;
import de.yaacc.upnp.server.contentdirectory.YaaccContentDirectory;
import de.yaacc.util.FileDownloader;

/**
 * A client facade to the upnp lookup and access framework. This class provides
 * all services to manage devices.
 *
 * @author Tobias Schoene (TheOpenBit)
 */
public class UpnpClient implements RegistryListener, ServiceConnection {
    public static String LOCAL_UID = "LOCAL_UID";
    private static UpnpClient instance;


    private List<UpnpClientListener> listeners = new ArrayList<UpnpClientListener>();
    private Set<Device> knownDevices = new HashSet<Device>();
    private AndroidUpnpService androidUpnpService;
    private Context context;
    SharedPreferences preferences;
    private boolean mute = false;

    public UpnpClient() {
        Log.d("BZA", "UPNP client" + "constructor"  );
    }

    public static UpnpClient getInstance(Context context) {
        Log.d("BZA", "UPNP client" + "getInstance"  );
        if (instance == null) {
            instance = new UpnpClient();
            instance.initialize(context);
        }
        return instance;
    }

    /**
     * Initialize the Object.
     *
     * @param context the context
     * @return true if initialization completes correctly
     */
    public boolean initialize(Context context) {
        this.context = context;
        this.preferences = PreferenceManager.getDefaultSharedPreferences(context);


        // FIXME check if this is right: Context.BIND_AUTO_CREATE kills the
        // service after closing the activity
        return context.bindService(new Intent(context, UpnpRegistryService.class), this, Context.BIND_AUTO_CREATE);
    }

    private SyncOffset getDeviceSyncOffset() {
        int offsetValue = Integer.valueOf(preferences.getString(getContext().getString(R.string.settings_device_playback_offset_key), "0"));
        if (offsetValue > 999) {
            Editor editor = preferences.edit();
            editor.putString(getContext().getString(R.string.settings_device_playback_offset_key), String.valueOf(999));
            editor.apply();
            offsetValue = 999;
        }
        return new SyncOffset(true, 0, 0, 0, offsetValue, 0, 0);
    }

    private void deviceAdded(@SuppressWarnings("rawtypes") final Device device) {
        fireDeviceAdded(device);
    }

    private void deviceRemoved(@SuppressWarnings("rawtypes") final Device device) {
        fireDeviceRemoved(device);
    }

    private void deviceUpdated(@SuppressWarnings("rawtypes") final Device device) {
        fireDeviceUpdated(device);
    }

    private void fireDeviceAdded(Device<?, ?, ?> device) {
        for (UpnpClientListener listener : new ArrayList<UpnpClientListener>(listeners)) {
            listener.deviceAdded(device);
        }
    }

    private void fireDeviceRemoved(Device<?, ?, ?> device) {
        for (UpnpClientListener listener : listeners) {
            listener.deviceRemoved(device);
        }
    }

    private void fireDeviceUpdated(Device<?, ?, ?> device) {
        for (UpnpClientListener listener : listeners) {
            listener.deviceUpdated(device);
        }
    }

    // interface implementation ServiceConnection
    // monitor android service creation and destruction
    /*
     * (non-Javadoc)
	 * 
	 * @see
	 * android.content.ServiceConnection#onServiceConnected(android.content.
	 * ComponentName, android.os.IBinder)
	 */
    @Override
    public void onServiceConnected(ComponentName className, IBinder service) {
        setAndroidUpnpService(((AndroidUpnpService) service));
        refreshUpnpDeviceCatalog();

    }


    public class ListenerThread implements Runnable {


        public void run() {
            //	Looper.prepare();
            Log.d("ZAA", "LooperThread started ");
            int i = 0;
            while(getProviderDevice() == null){
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            Service service = getProviderDevice().findService(new UDAServiceId("ContentDirectory"));

            SubscriptionCallback callback = new SubscriptionCallback(service, 600) {

                @Override
                public void established(GENASubscription sub) {
                    Log.d("BZA", "established: " );

                }

                @Override
                protected void failed(GENASubscription subscription,
                                      UpnpResponse responseStatus,
                                      Exception exception,
                                      String defaultMsg) {
                    System.err.println(defaultMsg);
                    Log.d("BZA", "failed: " );
                }

                @Override
                public void ended(GENASubscription sub,
                                  CancelReason reason,
                                  UpnpResponse response) {
                    // assertNull(reason);
                }

                @Override
                public void eventReceived(GENASubscription sub) {

                    Log.d("BZA", "eventReceived: " );

                    Map<String, StateVariableValue> values = sub.getCurrentValues();
                    StateVariableValue status = values.get("Status");

                    //// assertEquals(status.getDatatype().getClass(), BooleanDatatype.class);
                    // assertEquals(status.getDatatype().getBuiltin(), Datatype.Builtin.BOOLEAN);

                    Log.d("BZA", "eventReceived: 123 yess " + status.toString() );

                }

                @Override
                public void eventsMissed(GENASubscription sub, int numberOfMissedEvents) {
                    Log.d("BZA", "eventsMissed: " );
                }

                @Override
                protected void invalidMessage(RemoteGENASubscription sub,
                                              UnsupportedDataException ex) {
                    // Log/send an error report?
                }
            };
           while(true) {
               Log.d("BZA", "Loopinggggg: " );
               getControlPoint().execute(callback);
               try {
                   Thread.sleep(500);
               } catch (InterruptedException e) {
                   e.printStackTrace();
               }
           }


        }

        //Looper.loop();


    }
    /*
     * (non-Javadoc)
     *
     * @see
     * android.content.ServiceConnection#onServiceDisconnected(android.content
     * .ComponentName)
     */
    @Override
    public void onServiceDisconnected(ComponentName className) {
        setAndroidUpnpService(null);
    }

    // ----------Implementation Upnp RegistryListener Interface
    @Override
    public void remoteDeviceDiscoveryStarted(Registry registry, RemoteDevice remotedevice) {
        // TODO Auto-generated method stub
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * org.fourthline.cling.registry.RegistryListener#remoteDeviceDiscoveryFailed
     * (org.fourthline.cling.registry.Registry,
     * org.fourthline.cling.model.meta.RemoteDevice, java.lang.Exception)
     */
    @Override
    public void remoteDeviceDiscoveryFailed(Registry registry, RemoteDevice remotedevice, Exception exception) {
        Log.d(getClass().getName(), "remoteDeviceDiscoveryFailed: " + remotedevice.getDisplayString(), exception);
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * org.fourthline.cling.registry.RegistryListener#remoteDeviceAdded(org.
     * fourthline .cling.registry.Registry,
     * org.fourthline.cling.model.meta.RemoteDevice)
     */
    @Override
    public void remoteDeviceAdded(Registry registry, RemoteDevice remotedevice) {
        Log.d(getClass().getName(), "remoteDeviceAdded: " + remotedevice.getDisplayString());
        deviceAdded(remotedevice);
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * org.fourthline.cling.registry.RegistryListener#remoteDeviceUpdated(org
     * .fourthline .cling.registry.Registry,
     * org.fourthline.cling.model.meta.RemoteDevice)
     */
    @Override
    public void remoteDeviceUpdated(Registry registry, RemoteDevice remotedevice) {
        Log.d(getClass().getName(), "remoteDeviceUpdated: " + remotedevice.getDisplayString());
        deviceUpdated(remotedevice);
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * org.fourthline.cling.registry.RegistryListener#remoteDeviceRemoved(org
     * .fourthline .cling.registry.Registry,
     * org.fourthline.cling.model.meta.RemoteDevice)
     */
    @Override
    public void remoteDeviceRemoved(Registry registry, RemoteDevice remotedevice) {
        Log.d(getClass().getName(), "remoteDeviceRemoved: " + remotedevice.getDisplayString());
        deviceRemoved(remotedevice);
    }

    /*
     * (non-Javadoc)
     *
     * @see org.fourthline.cling.registry.RegistryListener#localDeviceAdded(org.
     * fourthline .cling.registry.Registry,
     * org.fourthline.cling.model.meta.LocalDevice)
     */
    @Override
    public void localDeviceAdded(Registry registry, LocalDevice localdevice) {
        Log.d(getClass().getName(), "localDeviceAdded: " + localdevice.getDisplayString());
        this.getRegistry().addDevice(localdevice);
        this.deviceAdded(localdevice);
        ListenerThread r = new ListenerThread();
        Thread t = new Thread(r);
        t.start();
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * org.fourthline.cling.registry.RegistryListener#localDeviceRemoved(org
     * .fourthline .cling.registry.Registry,
     * org.fourthline.cling.model.meta.LocalDevice)
     */
    @Override
    public void localDeviceRemoved(Registry registry, LocalDevice localdevice) {
        Registry currentRegistry = this.getRegistry();
        if (localdevice != null && currentRegistry != null) {
            Log.d(getClass().getName(), "localDeviceRemoved: " + localdevice.getDisplayString());
            this.deviceRemoved(localdevice);
            this.getRegistry().removeDevice(localdevice);
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * org.fourthline.cling.registry.RegistryListener#beforeShutdown(org.fourthline
     * . cling.registry.Registry)
     */
    @Override
    public void beforeShutdown(Registry registry) {
        Log.d(getClass().getName(), "beforeShutdown: " + registry);
    }

    /*
     * (non-Javadoc)
     *
     * @see org.fourthline.cling.registry.RegistryListener#afterShutdown()
     */
    @Override
    public void afterShutdown() {
        Log.d(getClass().getName(), "afterShutdown ");
    }

    // ****************************************************

    /**
     * Returns a Service of type AVTransport
     *
     * @param device the device which provides the service
     * @return the service of null
     */
    public Service getAVTransportService(Device<?, ?, ?> device) {
        if (device == null) {
            Log.d("BZA", "Device is null!");
            return null;
        }
        ServiceId serviceId = new UDAServiceId("AVTransport");
        Log.d("BZA", "Client getAVTransportService");
        Service service = device.findService(serviceId);
        if (service != null) {
            Log.d(getClass().getName(), "Service found: " + service.getServiceId() + " Type: " + service.getServiceType());
        }
        return service;
    }

    /**
     * Returns a Service of type RenderingControl
     *
     * @param device the device which provides the service
     * @return the service of null
     */
    public Service getRenderingControlService(Device<?, ?, ?> device) {
        if (device == null) {
            Log.d(getClass().getName(), "Device is null!");
            return null;
        }
        ServiceId serviceId = new UDAServiceId("RenderingControl");
        Service service = device.findService(serviceId);
        if (service != null) {
            Log.d(getClass().getName(), "Service found: " + service.getServiceId() + " Type: " + service.getServiceType());
        }
        return service;
    }

    /**
     * Start an intent with Action.View;
     *
     * @param uris the uri to start
     * @param mime mime type
     */
    protected void intentView(String mime, Uri... uris) {
        if (uris == null || uris.length == 0)
            return;
        Intent intent = null;
        if (mime != null) {
            // test if special activity to choose
            if (mime.indexOf("audio") > -1) {
                boolean background = preferences.getBoolean(context.getString(R.string.settings_audio_app), true);
                if (background) {
                    Log.d(getClass().getName(), "Starting Background service... ");
                    Intent svc = new Intent(context, BackgroundMusicService.class);
                    if (uris.length == 1) {
                        svc.setData(uris[0]);
                    } else {
                        svc.putExtra(BackgroundMusicService.URIS, uris);
                    }
                    context.startService(svc);
                    return;
                } else {
                    intent = new Intent(Intent.ACTION_VIEW);
                    if (uris.length == 1) {
                        intent.setDataAndType(uris[0], mime);
                    } else {
                        // FIXME How to handle this...
                        throw new IllegalStateException("Not yet implemented");
                    }
                }
            } else if (mime.indexOf("image") > -1) {
                boolean yaaccImageViewer = preferences.getBoolean(context.getString(R.string.settings_image_app), true);
                if (yaaccImageViewer) {
                    intent = new Intent(context, ImageViewerActivity.class);
                    if (uris.length == 1) {
                        intent.setDataAndType(uris[0], mime);
                    } else {
                        intent.putExtra(ImageViewerActivity.URIS, uris);
                    }
                } else {
                    intent = new Intent(Intent.ACTION_VIEW);
                    if (uris.length == 1) {
                        intent.setDataAndType(uris[0], mime);
                    } else {
                        // FIXME How to handle this...
                        throw new IllegalStateException("Not yet implemented");
                    }
                }
            }
        }
        if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK |Intent.FLAG_ACTIVITY_NEW_TASK);
            try {
                context.startActivity(intent);
            } catch (ActivityNotFoundException anfe) {
                Resources res = getContext().getResources();
                String text = String.format(res.getString(R.string.error_no_activity_found), mime);
                Toast toast = Toast.makeText(getContext(), text, Toast.LENGTH_LONG);
                toast.show();
            }
        }
    }

    /**
     * Add an listener.
     *
     * @param listener the listener to be added
     */
    public void addUpnpClientListener(UpnpClientListener listener) {
        listeners.add(listener);
    }

    /**
     * Remove the given listener.
     *
     * @param listener the listener which is to be removed
     */
    public void removeUpnpClientListener(UpnpClientListener listener) {
        listeners.remove(listener);
    }

    /**
     * returns the AndroidUpnpService
     *
     * @return the service
     */
    protected AndroidUpnpService getAndroidUpnpService() {
        return androidUpnpService;
    }

    /**
     * Returns all registered UpnpDevices.
     *
     * @return the upnpDevices
     */
    public Collection<Device> getDevices() {
        if (isInitialized()) {
            return getRegistry().getDevices();
        }
        return new ArrayList<Device>();
    }

    /**
     * Returns all registered UpnpDevices with a ContentDirectory Service.
     *
     * @return the upnpDevices
     */
    public Collection<Device> getDevicesProvidingContentDirectoryService() {
        if (isInitialized()) {
            return getRegistry().getDevices(new UDAServiceType("ContentDirectory"));
        }
        return new ArrayList<Device>();
    }

    public Collection<Device> getDevicesProvidingSRTfindingService() {
        if (isInitialized()) {
            return getRegistry().getDevices(new UDAServiceType("SRTDiscovery"));
        }
        return new ArrayList<Device>();
    }

    /**
     * Returns all registered UpnpDevices with an AVTransport Service.
     *
     * @return the upnpDevices
     */
    public Collection<Device> getDevicesProvidingAvTransportService() {
        ArrayList<Device> result = new ArrayList<Device>();
        result.add(getLocalDummyDevice());
        if (isInitialized()) {
            result.addAll(getRegistry().getDevices(new UDAServiceType("AVTransport")));
        }
        return result;
    }

    /**
     * Returns a registered UpnpDevice.
     *
     * @return the upnpDevice null if not found
     */
    public Device<?, ?, ?> getDevice(String identifier) {
        if (LOCAL_UID.equals(identifier)) {
            return getLocalDummyDevice();
        }
        if (isInitialized()) {
            return getRegistry().getDevice(new UDN(identifier), true);
        }
        return null;
    }

    /**
     * Returns the cling UpnpService.
     *
     * @return the cling UpnpService
     */
    public UpnpService getUpnpService() {
        if (!isInitialized()) {
            return null;
        }
        return androidUpnpService.get();
    }

    /**
     * True if the client is initialized.
     *
     * @return true or false
     */
    public boolean isInitialized() {
        return getAndroidUpnpService() != null;
    }

    /**
     * returns the upnp service configuration
     *
     * @return the configuration
     */
    public UpnpServiceConfiguration getConfiguration() {
        if (!isInitialized()) {
            return null;
        }
        return androidUpnpService.getConfiguration();
    }

    /**
     * returns the upnp control point
     *
     * @return the control point
     */
    public ControlPoint getControlPoint() {
        if (!isInitialized()) {
            return null;
        }
        return androidUpnpService.getControlPoint();
    }

    /**
     * Returns the upnp registry
     *
     * @return the registry
     */
    public Registry getRegistry() {
        if (!isInitialized()) {
            return null;
        }
        return androidUpnpService.getRegistry();
    }

    /**
     * @return the context
     */
    public Context getContext() {
        return context;
    }

    /**
     * Setting an new upnpRegistryService. If the service is not null, refresh
     * the device list.
     *
     * @param upnpService
     */
    protected void setAndroidUpnpService(AndroidUpnpService upnpService) {
        this.androidUpnpService = upnpService;
    }

    /**
     * refresh the device catalog
     */
    private void refreshUpnpDeviceCatalog() {
        if (isInitialized()) {
            for (Device<?, ?, ?> device : getAndroidUpnpService().getRegistry().getDevices()) {
                // FIXME: What about removed devices?
                this.deviceAdded(device);
            }
            // Getting ready for future device advertisements
            getAndroidUpnpService().getRegistry().addListener(this);
            searchDevices();
        }
    }

    /**
     * Browse ContenDirctory synchronous
     *
     * @param device   the device to be browsed
     * @param objectID the browsing root
     * @return the browsing result
     */
    public ContentDirectoryBrowseResult browseSync(Device<?, ?, ?> device, String objectID) {
        return browseSync(device, objectID, BrowseFlag.DIRECT_CHILDREN, "*", 0L, null, new SortCriterion[0]);
    }

    /**
     * Browse ContenDirctory synchronous
     *
     * @param pos Position
     *            the device and object to be browsed
     * @return the browsing result
     */
    public ContentDirectoryBrowseResult browseSync(Position pos) {
        if(getProviderDevice() == null){
            return  null;
        }
        if (pos == null || pos.getDeviceId() == null ) {
            if(getProviderDevice() != null){
                return browseSync(getProviderDevice(),"0" , BrowseFlag.DIRECT_CHILDREN, "*", 0L, null, new SortCriterion[0]);
            }else{
                return null;
            }
        }
        if (getProviderDevice() != null && !pos.getDeviceId().equals(getProviderDevice().getIdentity().getUdn().getIdentifierString())){
            return browseSync(getProviderDevice(),"0" , BrowseFlag.DIRECT_CHILDREN, "*", 0L, null, new SortCriterion[0]);
        }
        return browseSync(getDevice(pos.getDeviceId()), pos.getObjectId(), BrowseFlag.DIRECT_CHILDREN, "*", 0L, null, new SortCriterion[0]);
    }

    /**
     * Browse ContenDirctory synchronous
     *
     * @param device      the device to be browsed
     * @param objectID    the browsing root
     * @param flag        kind of browsing @see {@link BrowseFlag}
     * @param filter      a filter
     * @param firstResult first result
     * @param maxResults  max result count
     * @param orderBy     sorting criteria @see {@link SortCriterion}
     * @return the browsing result
     */
    public ContentDirectoryBrowseResult browseSync(Device<?, ?, ?> device, String objectID, BrowseFlag flag, String filter, long firstResult,
                                                   Long maxResults, SortCriterion... orderBy) {
        ContentDirectoryBrowseResult result = new ContentDirectoryBrowseResult();
        if (device == null) {
            return result;
        }
        Log.d("BZA", "UPNP client" + "ContentDirectoryBrowseResult"  );
        Service service = device.findService(new UDAServiceId("ContentDirectory"));
        ContentDirectoryBrowseActionCallback actionCallback = null;
        if (service != null) {
            Log.d("BZA", "#####Service found: " + service.getServiceId() + " Type: " + service.getServiceType());
            actionCallback = new ContentDirectoryBrowseActionCallback(service, objectID, flag, filter, firstResult, maxResults, result, orderBy);
            getControlPoint().execute(actionCallback);
            while (actionCallback.getStatus() == Status.LOADING && actionCallback.getUpnpFailure() == null)

                ;
        }

        SubscriptionCallback callback = new SubscriptionCallback(service, 600) {

            @Override
            public void established(GENASubscription sub) {
                Log.d("BZA", "established: " );

            }

            @Override
            protected void failed(GENASubscription subscription,
                                  UpnpResponse responseStatus,
                                  Exception exception,
                                  String defaultMsg) {
                System.err.println(defaultMsg);
                Log.d("BZA", "failed: " );
            }

            @Override
            public void ended(GENASubscription sub,
                              CancelReason reason,
                              UpnpResponse response) {
               // assertNull(reason);
            }

            @Override
            public void eventReceived(GENASubscription sub) {

                Log.d("BZA", "eventReceived: " );

                Map<String, StateVariableValue> values = sub.getCurrentValues();
                StateVariableValue status = values.get("Status");

               //// assertEquals(status.getDatatype().getClass(), BooleanDatatype.class);
               // assertEquals(status.getDatatype().getBuiltin(), Datatype.Builtin.BOOLEAN);

                Log.d("BZA", "eventReceived: 123 yess " + status.toString() );

            }

            @Override
            public void eventsMissed(GENASubscription sub, int numberOfMissedEvents) {
                Log.d("BZA", "eventsMissed: " );
            }

            @Override
            protected void invalidMessage(RemoteGENASubscription sub,
                                          UnsupportedDataException ex) {
                // Log/send an error report?
            }
        };

        //getControlPoint().execute(callback);
        Log.d("BZA", "after execute callback: " );


        if (preferences.getBoolean(context.getString(R.string.settings_browse_thumbnails_coverlookup_chkbx), false)) {
            result = enrichWithCover(result);
        }
        return result;
    }





    /**
     * Trying to add album art if there are only audiofiles and exactly one imagefile in a folder
     *
     * @param callbackResult orginal callback
     * @return if albumart and audiofiles are contained enriched audiofiles, otherwise the original result
     */
    private ContentDirectoryBrowseResult enrichWithCover(ContentDirectoryBrowseResult callbackResult) {

        DIDLContent cont = callbackResult.getResult();
        if (cont == null){
            return callbackResult;
        }
        if (cont.getContainers().size() != 0) {
            return callbackResult;
        }
        URI albumArtUri = null;
        LinkedList<Item> audioFiles = new LinkedList<Item>();

        if (cont.getItems().size() == 1) {
            //nothing to enrich
            return callbackResult;
        }

        for (Item currentItem : cont.getItems()) {
            if (!(currentItem instanceof AudioItem)) {
                if (null == albumArtUri && (currentItem instanceof ImageItem)) {
                    albumArtUri = URI.create(((ImageItem) currentItem).getFirstResource().getValue().toString());
                } else {
                    //There seem to be multiple images or other media files
                    audioFiles = null;
                    return callbackResult;
                }

            } else {

                if (null != audioFiles) {
                    audioFiles.add(currentItem);
                } else {
                    audioFiles = new LinkedList<Item>();
                    audioFiles.add(currentItem);
                }
            }
        }

        if (null == albumArtUri) {
            audioFiles = null;
            return callbackResult;
        }
        //We should only be here if there are just musicfiles and exactly one imagefile
        for (Item currentItem : audioFiles) {
            currentItem.replaceFirstProperty((new DIDLObject.Property.UPNP.ALBUM_ART_URI(albumArtUri)));
        }

        //this hopefully overwrites all previously existing contents
        cont.setItems(audioFiles);
        callbackResult.setResult(cont);

        return callbackResult;
    }

    /**
     * Browse ContenDirctory asynchronous
     *
     * @param device   the device to be browsed
     * @param objectID the browsing root
     * @return the browsing result
     */
    public ContentDirectoryBrowseResult browseAsync(Device<?, ?, ?> device, String objectID) {
        return browseAsync(device, objectID, BrowseFlag.DIRECT_CHILDREN, "*", 0L, null, new SortCriterion[0]);
    }

    /**
     * Browse ContenDirctory asynchronous
     *
     * @param device      the device to be browsed
     * @param objectID    the browsing root
     * @param flag        kind of browsing @see {@link BrowseFlag}
     * @param filter      a filter
     * @param firstResult first result
     * @param maxResults  max result count
     * @param orderBy     sorting criteria @see {@link SortCriterion}
     * @return the browsing result
     */
    public ContentDirectoryBrowseResult browseAsync(Device<?, ?, ?> device, String objectID, BrowseFlag flag, String filter, long firstResult,
                                                    Long maxResults, SortCriterion... orderBy) {
        Service service = device.findService(new UDAServiceId("ContentDirectory"));
        ContentDirectoryBrowseResult result = new ContentDirectoryBrowseResult();
        ContentDirectoryBrowseActionCallback actionCallback = null;
        if (service != null) {
            Log.d(getClass().getName(), "#####Service found: " + service.getServiceId() + " Type: " + service.getServiceType());
            actionCallback = new ContentDirectoryBrowseActionCallback(service, objectID, flag, filter, firstResult, maxResults, result, orderBy);
            getControlPoint().execute(actionCallback);
        }
        if (preferences.getBoolean(context.getString(R.string.settings_browse_thumbnails_coverlookup_chkbx), false)) {
            result = enrichWithCover(result);
        }
        return result;
    }

    /**
     * Search asynchronously for all devices.
     */
    public void searchDevices() {
        if (isInitialized()) {
            getAndroidUpnpService().getControlPoint().search();
        }
    }

    /**
     * Returns all player instances initialized with the given didl object
     *
     * @param didlObject the object which describes the content to be played
     * @return the player
     */
    public List<Player> initializePlayers(DIDLObject didlObject) {
        return initializePlayers(toItemList(didlObject));
    }

    /**
     * Returns all player instances initialized with the given didl object
     *
     * @param items the items to be played
     * @return the player
     */
    public List<Player> initializePlayers(List<Item> items) {
        LinkedList<PlayableItem> playableItems = new LinkedList<PlayableItem>();

        for (Item currentItem : items) {
            PlayableItem playableItem = new PlayableItem(currentItem, getDefaultDuration());
            playableItems.add(playableItem);
        }
        SynchronizationInfo synchronizationInfo = new SynchronizationInfo();
        synchronizationInfo.setOffset(getDeviceSyncOffset()); //device specific offset

        Calendar now = Calendar.getInstance(Locale.getDefault());
        now.add(Calendar.MILLISECOND, Integer.valueOf(preferences.getString(context.getString(R.string.settings_default_playback_delay_key), "0")));
        String referencedPresentationTime = new SyncOffset(true, now.get(Calendar.HOUR_OF_DAY), now.get(Calendar.MINUTE), now.get(Calendar.SECOND), now.get(Calendar.MILLISECOND), 0, 0).toString();
        Log.d(getClass().getName(), "CurrentTime: " + new Date().toString() + " representationTime: " + referencedPresentationTime);
        synchronizationInfo.setReferencedPresentationTime(referencedPresentationTime);
        Log.d("BZA", "Finally found source of create player");
        return PlayerFactory.createPlayer(this, synchronizationInfo, playableItems);
    }

    /**
     * Returns all player instances initialized with the given transport object
     *
     * @param transport the object which describes the content to be played
     * @return the player
     */
    public List<Player> initializePlayers(AvTransport transport) {
        Log.d("BZA", "initializePlayers AV transport");
        PlayableItem playableItem = new PlayableItem();
        List<PlayableItem> items = new ArrayList<PlayableItem>();
        if (transport == null) {
            return PlayerFactory.createPlayer(this, transport.getSynchronizationInfo(), items);
        }
        Log.d(getClass().getName(), "TransportId: " + transport.getInstanceId());
        PositionInfo positionInfo = transport.getPositionInfo();
        Log.d(getClass().getName(), "positionInfo: " + positionInfo);
        if (positionInfo == null) {
            return PlayerFactory.createPlayer(this, transport.getSynchronizationInfo(), items);
        }
        DIDLContent metadata = null;
        try {
            if (positionInfo.getTrackMetaData() != null && positionInfo.getTrackMetaData().indexOf("NOT_IMPLEMENTED") == -1) {
                metadata = new DIDLParser().parse(positionInfo.getTrackMetaData());
            } else {
                Log.d(getClass().getName(), "Warning unparsable TackMetaData: " + positionInfo.getTrackMetaData());
            }
        } catch (Exception e) {
            Log.d(getClass().getName(), "Exception while parsing metadata: ", e);
        }
        String mimeType = "";
        if (metadata != null) {
            List<Item> metadataItems = metadata.getItems();
            for (Item item : metadataItems) {
                playableItem.setTitle(item.getTitle());
                List<Res> metadataResources = item.getResources();
                for (Res res : metadataResources) {
                    if (res.getProtocolInfo() != null) {
                        mimeType = res.getProtocolInfo().getContentFormatMimeType().toString();
                        break;
                    }
                }
                break;
            }
        } else {
            playableItem.setTitle(positionInfo.getTrackURI().toString());
            String fileExtension = MimeTypeMap.getFileExtensionFromUrl(positionInfo.getTrackURI());
            mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(fileExtension);
            Log.d(getClass().getName(), "fileextension from trackURI: " + fileExtension);
        }
        playableItem.setMimeType(mimeType);
        playableItem.setUri(Uri.parse(positionInfo.getTrackURI()));
        Log.d(getClass().getName(), "positionInfo.getTrackURI(): " + positionInfo.getTrackURI());
        // FIXME Duration not supported in receiver yet
        // playableItem.setDuration(duration)
        items.add(playableItem);
        Log.d(getClass().getName(), "TransportUri: " + positionInfo.getTrackURI());
        Log.d(getClass().getName(), "Current duration: " + positionInfo.getTrackDuration());
        Log.d(getClass().getName(), "TrackMetaData: " + positionInfo.getTrackMetaData());
        Log.d(getClass().getName(), "MimeType: " + playableItem.getMimeType());
        return PlayerFactory.createPlayer(this, transport.getSynchronizationInfo(), items);
    }

    /**
     * Returns all current player instances
     * @return the player
     */
    public List<Player> getCurrentPlayers(){
        return PlayerFactory.getCurrentPlayers();
    }

    /**
     * Returns all current player instances for the given transport object
     *
     * @param transport the object which describes the content to be played
     * @return the player
     */
    public List<Player> getCurrentPlayers(AvTransport transport) {
        List<PlayableItem> items = new ArrayList<PlayableItem>();
        SynchronizationInfo synchronizationInfo = transport.getSynchronizationInfo();
        synchronizationInfo.setOffset(getDeviceSyncOffset());
        if (transport == null)
            return PlayerFactory.createPlayer(this, synchronizationInfo, items);
        Log.d(getClass().getName(), "TransportId: " + transport.getInstanceId());
        PositionInfo positionInfo = transport.getPositionInfo();
        if (positionInfo == null) {
            return PlayerFactory.createPlayer(this, synchronizationInfo, items);
        }
        DIDLContent metadata = null;
        try {
            if(positionInfo.getTrackMetaData() != null){
              metadata = new DIDLParser().parse(positionInfo.getTrackMetaData());
            }
        } catch (Exception e) {
            Log.d(getClass().getName(), "Exception while parsing metadata: ", e);
        }
        String mimeType = "";
        PlayableItem playableItem = null;
        if (metadata != null) {
            List<Item> metadataItems = metadata.getItems();
            for (Item item : metadataItems) {
                playableItem = new PlayableItem(item, getDefaultDuration());
                playableItem.setTitle(item.getTitle());
                List<Res> metadataResources = item.getResources();
                for (Res res : metadataResources) {
                    if (res.getProtocolInfo() != null) {
                        mimeType = res.getProtocolInfo().getContentFormatMimeType().toString();
                        break;
                    }
                }
                break;
            }
            if(mimeType  == null || mimeType.equals("")){
                String fileExtension = MimeTypeMap.getFileExtensionFromUrl(positionInfo.getTrackURI());
                mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(fileExtension);
            }
        } else {
            playableItem = new PlayableItem();
            playableItem.setDuration(getDefaultDuration());
            playableItem.setTitle(positionInfo.getTrackURI().toString());
            String fileExtension = MimeTypeMap.getFileExtensionFromUrl(positionInfo.getTrackURI());
            mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(fileExtension);
        }
        playableItem.setMimeType(mimeType);
        playableItem.setUri(Uri.parse(positionInfo.getTrackURI()));
        Log.d(getClass().getName(), "MimeType: " + playableItem.getMimeType());
        List<Player> avTransportPlayers = PlayerFactory.getCurrentPlayersOfType(PlayerFactory.getPlayerClassForMimeType(mimeType), synchronizationInfo);
        return avTransportPlayers;
    }

    /**
     * returns a list of all items including items in containers for the given didlContent
     * @param  didlContent the content
     * @return all items included in the content
     **/
    public List<Item> toItemList(DIDLContent didlContent) {
        List<Item> items = new ArrayList<Item>();
        if(didlContent == null){
            return items;
        }
        items.addAll(didlContent.getItems());
        for(Container c : didlContent.getContainers()){
            items.addAll(toItemList(c));
        }
        return items;
    }

    /**
     * Converts the content of a didlObject into a list of cling items.
     *
     * @param didlObject the content
     * @return the list of cling items
     */
    public List<Item> toItemList(DIDLObject didlObject) {
        List<Item> items = new ArrayList<Item>();
        if (didlObject instanceof Container) {
            DIDLContent content = loadContainer((Container) didlObject);
            if (content != null) {
                items.addAll(content.getItems());
                for (Container includedContainer : content.getContainers()) {
                    items.addAll(toItemList(includedContainer));
                }
            }
        } else if (didlObject instanceof Item) {
            items.add((Item) didlObject);
        }
        return items;
    }

    /**
     * load the content of the container.
     *
     * @param container the container to be loaded
     * @return the loaded content
     */
    private DIDLContent loadContainer(Container container) {
        ContentDirectoryBrowseResult result = browseSync(getProviderDevice(), container.getId());
        if (result.getUpnpFailure() != null) {
            Toast toast = Toast.makeText(getContext(), result.getUpnpFailure().getDefaultMsg(), Toast.LENGTH_LONG);
            toast.show();
            return null;
        }
        return result.getResult();
    }

    /**
     * Gets the receiver IDs, if none is defined the local device will be
     * returned
     *
     * @return the receiverDeviceIds
     */
    public Set<String> getReceiverDeviceIds() {
        Log.d("BZA", "UPNP " + "getReceiverDeviceIds"  );
        HashSet<String> defaultReceiverSet = new HashSet<String>();
        defaultReceiverSet.add(UpnpClient.LOCAL_UID);
        Set<String> receiverDeviceIds = preferences.getStringSet(context.getString(R.string.settings_selected_receivers_title), defaultReceiverSet);
        return receiverDeviceIds;
    }

    /**
     * Returns the receiverIds stored in the preferences. If an receiver id is
     * unknown it will be removed.
     *
     * @return the receiverDevices
     */
    public Collection<Device> getReceiverDevices() {
        Log.d("BZA", "UPNP Client" + "getReceiverDevices"  );
        ArrayList<Device> result = new ArrayList<Device>();
        ArrayList<String> unknowsIds = new ArrayList<String>(); // Maybe the the
        // receiverDevice
        // in the
        // preferences
        // isn't
        // available any
        // more
        Set<String> receiverDeviceIds = getReceiverDeviceIds();
        for (String id : receiverDeviceIds) {
            Device receiver = this.getDevice(id);
            if (receiver != null) {
                result.add(this.getDevice(id));
                break;
            } else {
                unknowsIds.add(id);
            }
        }
        // remove all unknown ids
        receiverDeviceIds.removeAll(unknowsIds);
        setReceiverDeviceIds(receiverDeviceIds);
        return result;
    }

    /**
     * add a receiver device
     *
     * @param receiverDevice
     */
    public void addReceiverDevice(Device receiverDevice) {
        assert (receiverDevice != null);
        Collection<Device> receiverDevices = getReceiverDevices();
        receiverDevices.add(receiverDevice);
        setReceiverDevices(receiverDevices);
    }

    /**
     * remove a receiver device
     *
     * @param receiverDevice
     */
    public void removeReceiverDevice(Device receiverDevice) {
        assert (receiverDevice != null);
        Collection<Device> receiverDevices = getReceiverDevices();
        receiverDevices.remove(receiverDevice);
        setReceiverDevices(receiverDevices);
    }

    /**
     * set the receiverDevices to the devices in the given collection.
     *
     * @param receiverDevices the devices
     */
    public void setReceiverDevices(Collection<Device> receiverDevices) {
        assert (receiverDevices != null);
        HashSet<String> receiverIds = new HashSet<String>();
        for (Device receiver : receiverDevices) {
            Log.d(this.getClass().getName(), "Receiver: " + receiver);
            receiverIds.add(receiver.getIdentity().getUdn().getIdentifierString());
        }
        setReceiverDeviceIds(receiverIds);
    }

    /**
     * Set the list of receiver device ids.
     *
     * @param receiverDeviceIds the device ids.
     */
    protected void setReceiverDeviceIds(Set<String> receiverDeviceIds) {
        assert (receiverDeviceIds != null);
        Editor prefEdit = preferences.edit();
        prefEdit.putStringSet(context.getString(R.string.settings_selected_receivers_title), receiverDeviceIds);
        prefEdit.apply();
    }

    /**
     * @return the providerDeviceId
     */
    public String getProviderDeviceId() {
        return preferences.getString(context.getString(R.string.settings_selected_provider_title), null);
    }

    /**
     * @param provider
     */
    public void setProviderDevice(Device provider) {
        Editor prefEdit = preferences.edit();
        prefEdit.putString(context.getString(R.string.settings_selected_provider_title), provider.getIdentity().getUdn().getIdentifierString());
        prefEdit.apply();
    }

    /**
     * @return the provider device
     */
    public Device<?, ?, ?> getProviderDevice() {
        return this.getDevice(getProviderDeviceId());
    }

    /**
     * Check's whether local or remote playback is enabled
     *
     * @return true if local playback is enabled, false otherwise
     */
    public Boolean isLocalPlaybackEnabled() {
        return getReceiverDeviceIds().contains(LOCAL_UID);
    }

    /**
     * Shutdown the upnp client and all players
     */
    public void shutdown() {
        // shutdown UpnpRegistry
        boolean result = getContext().stopService(new Intent(context, UpnpRegistryService.class));
        Log.d(getClass().getName(), "Stopping UpnpRegistryService succsessful= " + result);
        // shutdown yaacc server service
        result = getContext().stopService(new Intent(context, YaaccUpnpServerService.class));
        Log.d(getClass().getName(), "Stopping YaaccUpnpServerService succsessful= " + result);
        // stop all players
        PlayerFactory.shutdown();
    }

    /**
     * Return the configured default duration
     *
     * @return the duration
     */
    public int getDefaultDuration() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        return Integer.parseInt(preferences.getString(getContext().getString(R.string.settings_default_duration_key), "0"));
    }

    /**
     * Return the configured silence duration
     *
     * @return the duration
     */
    public int getSilenceDuration() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        return Integer.parseInt(preferences.getString(getContext().getString(R.string.settings_silence_duration_key), "2000"));
    }

    public Device<?, ?, ?> getLocalDummyDevice() {
        Device result = null;
        try {
            result = new LocalDummyDevice();
        } catch (ValidationException e) {
            // TODO Auto-generated catch block
            // Ignore
            Log.d(this.getClass().getName(), "Something wrong with the LocalDummyDevice...", e);
        }
        return result;
    }

    /**
     * set the mute state
     *
     * @param mute the state
     */
    public void setMute(boolean mute) {
        this.mute = mute;
        AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        audioManager.setStreamMute(AudioManager.STREAM_MUSIC, mute);
    }

    /**
     * returns the mute state
     *
     * @return the state
     */
    public boolean isMute() {
        return mute;
    }

    /**
     * set the volume in the range of 0-100
     *
     * @param desired
     */
    public void setVolume(int desired) {
        if (desired < 0) {
            desired = 0;
        }
        if (desired > 100) {
            desired = 100;
        }
        AudioManager audioManager = (AudioManager) getContext().getSystemService(Context.AUDIO_SERVICE);
        int maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        int volume = desired * maxVolume / 100;
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, volume, AudioManager.FLAG_SHOW_UI);
    }

    /**
     * returns the current volume level
     *
     * @return the value in the range of 0-100
     */
    public int getVolume() {

        AudioManager audioManager = (AudioManager) getContext().getSystemService(Context.AUDIO_SERVICE);
        int maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        int currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        int volume = currentVolume * 100 / maxVolume;
        return volume;
    }

    public void downloadItem(DIDLObject selectedDIDLObject) {
        AsyncTask<DIDLObject, Void, Void> fileDownloader = new FileDownloader(this).execute(selectedDIDLObject);
    }


    @SuppressWarnings({"unchecked", "rawtypes"})
    private class LocalDummyDevice extends Device {
        public LocalDummyDevice() throws ValidationException {
            super(new DeviceIdentity(new UDN(LOCAL_UID)));
        }

        @Override
        public Service[] getServices() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public Device[] getEmbeddedDevices() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public Device getRoot() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public Device findDevice(UDN udn) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public Resource[] discoverResources(Namespace namespace) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public Device newInstance(UDN arg0, UDAVersion arg1, DeviceType arg2, DeviceDetails arg3, Icon[] arg4, Service[] arg5, List arg6)
                throws ValidationException {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public Service newInstance(ServiceType servicetype, ServiceId serviceid, URI uri, URI uri1, URI uri2, Action[] aaction,
                                   StateVariable[] astatevariable) throws ValidationException {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public Device[] toDeviceArray(Collection collection) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public Service[] newServiceArray(int i) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public Service[] toServiceArray(Collection collection) {
            // TODO Auto-generated method stub
            return null;
        }

        /*
         * (non-Javadoc)
         *
         * @see org.fourthline.cling.model.meta.Device#getDisplayString()
         */
        @Override
        public String getDisplayString() {
            return android.os.Build.MODEL;
        }

        @Override
        public DeviceDetails getDetails() {
            return new DeviceDetails(android.os.Build.MODEL);
        }


    }
}
