/*
 *
 * Copyright (C) 2014 www.yaacc.de 
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

import java.util.ArrayList;
import java.util.List;

import org.fourthline.cling.support.model.DIDLObject;
import org.fourthline.cling.support.model.container.Container;
import org.fourthline.cling.support.model.container.StorageFolder;
import org.fourthline.cling.support.model.item.Item;

import de.yaacc.R;

/**
 * Browser  for the image folder.
 * 
 * 
 * @author openbit (Tobias Schoene)
 * 
 */
public class ImagesFolderBrowser extends ContentBrowser {

    public ImagesFolderBrowser(Context context) {
        super(context);
    }

    @Override
	public DIDLObject browseMeta(YaaccContentDirectory contentDirectory, String myId) {
		
		StorageFolder folder = new StorageFolder(ContentDirectoryIDs.IMAGES_FOLDER.getId(), ContentDirectoryIDs.ROOT.getId(), getContext().getString(R.string.images), "yaacc", 4,
				907000L);
		return folder;
	}


	
	@Override
	public List<Container> browseContainer(YaaccContentDirectory contentDirectory, String myId) {
		List<Container> result = new ArrayList<Container>();
        result.add((Container)new ImagesAllFolderBrowser(getContext()).browseMeta(contentDirectory, ContentDirectoryIDs.IMAGES_ALL_FOLDER.getId()));
        result.add((Container)new ImagesByBucketNamesFolderBrowser(getContext()).browseMeta(contentDirectory, ContentDirectoryIDs.IMAGES_BY_BUCKET_NAMES_FOLDER.getId()));
        return result;
	}

	@Override
	public List<Item> browseItem(YaaccContentDirectory contentDirectory, String myId) {
		List<Item> result = new ArrayList<Item>();
		
		return result;
		
	}

}
