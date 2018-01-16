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

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.fourthline.cling.support.model.DIDLObject;
import org.fourthline.cling.support.model.Res;
import org.fourthline.cling.support.model.DIDLObject.Property.UPNP;
import org.fourthline.cling.support.model.container.Container;
import org.fourthline.cling.support.model.container.PhotoAlbum;
import org.fourthline.cling.support.model.item.Item;
import org.fourthline.cling.support.model.item.Photo;
import org.seamless.util.MimeType;

import android.content.Context;
import android.database.Cursor;
import android.provider.MediaStore;
import android.util.Log;
import android.webkit.MimeTypeMap;
import de.yaacc.upnp.server.YaaccUpnpServerService;
/**
 * Browser  for the image folder.
 * 
 * 
 * @author TheOpenBit  (Tobias Schoene)
 * 
 */
public class ImagesByBucketNameFolderBrowser extends ContentBrowser {

    public ImagesByBucketNameFolderBrowser(Context context) {
        super(context);
    }

    @Override
	public DIDLObject browseMeta(YaaccContentDirectory contentDirectory, String myId) {
		
		PhotoAlbum photoAlbum = new PhotoAlbum(myId, ContentDirectoryIDs.IMAGES_BY_BUCKET_NAMES_FOLDER.getId(),getName(
				contentDirectory, myId), "yaacc", getSize(contentDirectory, myId));
		return photoAlbum;
	}

	private Integer getSize(YaaccContentDirectory contentDirectory, String myId){
		 Integer result = 0;
				String[] projection = { "count(*) as count" };
				String selection = MediaStore.Images.Media.BUCKET_ID + "=?";
				String[] selectionArgs = new String[] { myId.substring(myId
						.indexOf(ContentDirectoryIDs.IMAGES_BY_BUCKET_NAME_PREFIX.getId())) };
				Cursor cursor = contentDirectory.getContext().getContentResolver().query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, projection, selection,
						selectionArgs, null);

				if (cursor != null) {
					cursor.moveToFirst();
					result = Integer.valueOf(cursor.getString(0));
					cursor.close();
				}
				return result;
	}
	
	private String getName(YaaccContentDirectory contentDirectory, String myId) {
		String result = "";
		String[] projection = { MediaStore.Images.Media.BUCKET_ID,  MediaStore.Images.Media.BUCKET_DISPLAY_NAME};
		String selection = MediaStore.Images.Media.BUCKET_ID + "=?";
		String[] selectionArgs = new String[] { myId.substring(ContentDirectoryIDs.IMAGES_BY_BUCKET_NAME_PREFIX.getId().length()) };
		Cursor cursor = contentDirectory
				.getContext()
				.getContentResolver()
				.query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
						projection, selection, selectionArgs, null);

		if (cursor != null) {
			cursor.moveToFirst();
			result = cursor.getString(0);
			cursor.close();
		}
		return result;
	}
	@Override
	public List<Container> browseContainer(YaaccContentDirectory contentDirectory, String myId) {
		
		return new ArrayList<Container>();
	}

	@Override
	public List<Item> browseItem(YaaccContentDirectory contentDirectory, String myId) {
		List<Item> result = new ArrayList<Item>();
		// Query for all images on external storage
		String[] projection = { MediaStore.Images.Media._ID, MediaStore.Images.Media.DISPLAY_NAME, MediaStore.Images.Media.MIME_TYPE,
				MediaStore.Images.Media.SIZE,MediaStore.Images.Media.DATE_TAKEN  };
		String selection = MediaStore.Images.Media.BUCKET_ID + "=?";
		String[] selectionArgs = new String[] { myId.substring(ContentDirectoryIDs.IMAGES_BY_BUCKET_NAME_PREFIX.getId().length()) };
		Cursor mImageCursor = contentDirectory.getContext().getContentResolver().query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, projection, selection,
				selectionArgs, MediaStore.Images.Media.BUCKET_DISPLAY_NAME + " ASC");
		if (mImageCursor != null) {
			mImageCursor.moveToFirst();
			while (!mImageCursor.isAfterLast()) {
				String id = mImageCursor.getString(mImageCursor.getColumnIndex(MediaStore.Images.ImageColumns._ID));
				String name = mImageCursor.getString(mImageCursor.getColumnIndex(MediaStore.Images.ImageColumns.DISPLAY_NAME));
				Long size = Long.valueOf(mImageCursor.getString(mImageCursor.getColumnIndex(MediaStore.Images.ImageColumns.SIZE)));
				Log.d(getClass().getName(),
						"Mimetype: " + mImageCursor.getString(mImageCursor.getColumnIndex(MediaStore.Images.ImageColumns.MIME_TYPE)));
				MimeType mimeType = MimeType.valueOf(mImageCursor.getString(mImageCursor.getColumnIndex(MediaStore.Images.ImageColumns.MIME_TYPE)));
				// file parameter only needed for media players which decide the
				// ability of playing a file by the file extension
                String uri = getUriString(contentDirectory, id, mimeType);
				Res resource = new Res(mimeType, size, uri);
				
				Photo photo = new Photo(ContentDirectoryIDs.IMAGE_BY_BUCKET_PREFIX.getId()+id, myId, name, "", "", resource);
				URI albumArtUri = URI.create("http://"
						+ contentDirectory.getIpAddress() + ":"
						+ YaaccUpnpServerService.PORT + "/?thumb=" + id);
				photo.replaceFirstProperty(new UPNP.ALBUM_ART_URI(
						albumArtUri));
				
				result.add(photo);
				Log.d(getClass().getName(), "Image: " + id + " Name: " + name + " uri: " + uri);
				mImageCursor.moveToNext();
			}
			mImageCursor.close();
		} else {
			Log.d(getClass().getName(), "System media store is empty.");
		}
		return result;
		
	}

}
