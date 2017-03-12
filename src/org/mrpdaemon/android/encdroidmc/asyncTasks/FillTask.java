package org.mrpdaemon.android.encdroidmc.asyncTasks;

import org.mrpdaemon.android.encdroidmc.EDVolumeBrowserActivity;

import android.os.AsyncTask;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;

/*
 * Task to fill the volume browser list. This is needed because fill() can
 * end up doing network I/O with certain file providers and starting with
 * API version 13 doing so results in a NetworkOnMainThreadException.
 */
public class FillTask extends AsyncTask<Void, Void, Void> {

	private ProgressBar mProgBar;
	private ListView mListView;
	private LinearLayout mLayout;
	private boolean genThumbnails;
	private EDVolumeBrowserActivity volumeBrowserActivity;
	
	public FillTask(EDVolumeBrowserActivity volumeBrowserActivity, boolean genThumbnails) {
		super();
		this.genThumbnails=genThumbnails;
		this.volumeBrowserActivity=volumeBrowserActivity;
	}

	
	@Override
	protected void onPreExecute() {
		try {//sometime it crash if the user refresh while the list is already being refreshed
			super.onPreExecute();

			// Replace the ListView with a ProgressBar
			mProgBar = new ProgressBar(volumeBrowserActivity, null,android.R.attr.progressBarStyleLarge);

			// Set the layout to fill the screen
			mListView = volumeBrowserActivity.getListView();
			mLayout = (LinearLayout) mListView.getParent();
			if (mLayout==null) return;
			mLayout.setGravity(Gravity.CENTER);
			mLayout.setLayoutParams(new FrameLayout.LayoutParams(
					LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));

			// Set the ProgressBar in the center of the layout
			LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
					LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
			layoutParams.gravity = Gravity.CENTER;
			mProgBar.setLayoutParams(layoutParams);

			// Replace the ListView with the ProgressBar
			mLayout.removeView(mListView);
			mLayout.addView(mProgBar);
			mProgBar.setVisibility(View.VISIBLE);
		} catch (Exception e){
			e.printStackTrace();
		}
	}

	
	@Override
	protected Void doInBackground(Void... arg0) {
		if (this.volumeBrowserActivity!=null) this.volumeBrowserActivity.fill();
		return null;
	}

	
	@Override
	protected void onPostExecute(Void result) {
		try {//sometime it crash if the user refresh while the list is already being refreshed
			super.onPostExecute(result);
			if (mLayout==null) return;
			// Restore the layout parameters
			mLayout.setLayoutParams(new FrameLayout.LayoutParams(
					LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT));
			mLayout.setGravity(Gravity.TOP);

			// Remove the progress bar and replace it with the list view
			mLayout.removeView(mProgBar);
			mLayout.addView(mListView);
			//if file provider is local (internal or sdcard memory) launch automaticaly the thumbnail creation  
			boolean localProvider =  (volumeBrowserActivity.getmVolume().getFileProvider().getID()==0 || volumeBrowserActivity.getmVolume().getFileProvider().getID()==1);
			volumeBrowserActivity.thumbnailsTask = new ThumbnailsTask(volumeBrowserActivity,null,!localProvider,false);
			volumeBrowserActivity.thumbnailsTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
		} catch (Exception e){
			e.printStackTrace();
		}
	}
}
