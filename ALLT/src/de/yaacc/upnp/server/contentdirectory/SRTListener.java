package de.yaacc.upnp.server.contentdirectory;

import org.fourthline.cling.model.meta.Device;

/**
 * Created by Priyatanu on 1/2/2018.
 */

public interface SRTListener {
    void onTimePost(int time);
    void onPauseStop();
    void resume();
    void fileplayback(String file);
    void valuechanged(String value);

}
