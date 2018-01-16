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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package de.yaacc.upnp.server.contentdirectory;

import android.content.Context;
import android.util.Log;
import android.webkit.MimeTypeMap;

import java.util.ArrayList;
import java.util.List;

import org.fourthline.cling.support.model.DIDLObject;
import org.fourthline.cling.support.model.container.Container;
import org.fourthline.cling.support.model.item.Item;
import org.seamless.util.MimeType;

import de.yaacc.upnp.server.YaaccUpnpServerService;


/**
 * Super class for all contentent directory browsers.
 * 
 * @author openbit (Tobias Schoene)
 * 
 */
public abstract class ContentBrowser {

    Context context;

    protected ContentBrowser(Context context) {
        this.context = context;
    }

    public Context getContext() {
        return context;
    }


	public abstract DIDLObject browseMeta(YaaccContentDirectory contentDirectory, String myId);

	public abstract List<Container> browseContainer(
			YaaccContentDirectory content, String myId);

	public abstract List<Item> browseItem(YaaccContentDirectory contentDirectory, String myId);

	public List<DIDLObject> browseChildren(YaaccContentDirectory contentDirectory, String myId) {
		List<DIDLObject> result = new ArrayList<DIDLObject>();
		result.addAll(browseContainer(contentDirectory, myId));
		result.addAll(browseItem(contentDirectory, myId));
		return result;
	}

    public String getUriString(YaaccContentDirectory contentDirectory, String id, MimeType mimeType) {
        String fileExtension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType.toString());
        if(fileExtension == null){
            Log.d(getClass().getName(), "Can't lookup file extension from mimetype: " + mimeType);
            //try subtype
            fileExtension = mimeType.getSubtype();

        }
        return "http://" + contentDirectory.getIpAddress() + ":"
                + YaaccUpnpServerService.PORT + "/?id=" + id + "&f=file." + fileExtension;
    }
}
