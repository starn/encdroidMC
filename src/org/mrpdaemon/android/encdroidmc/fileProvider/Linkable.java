package org.mrpdaemon.android.encdroidmc.fileProvider;

import java.io.Serializable;

import android.app.Activity;

public interface Linkable extends Serializable {
	public boolean isLinked(Activity activity);
	public void link(Activity activity);
	public boolean onResume(Activity activity);
}
