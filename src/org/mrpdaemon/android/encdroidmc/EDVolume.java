/*
 * encdroid - EncFS client application for Android
 * Copyright (C) 2012  Mark R. Pariente
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.mrpdaemon.android.encdroidmc;

import org.mrpdaemon.sec.encfs.EncFSVolume;

public class EDVolume {

	// Volume types
//	public final static int LOCAL_VOLUME = 0;
//	public final static int DROPBOX_VOLUME = 1;
//	public final static int EXT_SD_VOLUME = 2;
	//public final static int WEBDAV_VOLUME = 3;

	// Volume name
	private String name;

	// Volume path
	private String path;

	// Volume type
	private int fileProviderId;
	
	private String serializedFileProviderParams;

	// Whether volume is locked or not
	private boolean isLocked;

	// EncFS volume associated with this volume
	private EncFSVolume volume;

	public EDVolume(String name, String path, int fileProviderId,String serializedFileProviderParams) {
		super();
		this.name = name;
		this.path = path;
		this.fileProviderId = fileProviderId;
		this.serializedFileProviderParams = serializedFileProviderParams;
		this.isLocked = true;
		this.volume = null;
	}
	
//	public EncdroidFileProvider getEncdroidFileProvider(){
//		return EncdroidFileProvider.getProvider(fileProviderType,serializedFileProviderParams, path);
//	}

	// Unlock the volume by passing in an EncFSVolume instance
	public void unlock(EncFSVolume volume) {
		if (this.isLocked) {
			this.volume = volume;
			this.isLocked = false;
		}
	}

	// Lock the volume
	public void lock() {
		if (!this.isLocked) {
			this.volume = null;
			this.isLocked = true;
		}
	}

	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * @param name
	 *            the name to set
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * @return the path
	 */
	public String getPath() {
		return path;
	}



	public int getFileProviderId() {
		return fileProviderId;
	}

	public String getSerializedFileProviderParams() {
		return serializedFileProviderParams;
	}

	/**
	 * @return the isLocked
	 */
	public boolean isLocked() {
		return isLocked;
	}

	/**
	 * @return the volume
	 */
	public EncFSVolume getVolume() {
		return volume;
	}
}