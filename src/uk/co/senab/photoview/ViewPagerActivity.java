/*******************************************************************************
 * Copyright 2011, 2012 Chris Banes.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package uk.co.senab.photoview;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.mrpdaemon.android.encdroidmc.EDVolumeBrowserActivity;
import org.mrpdaemon.android.encdroidmc.MimeManagement;
import org.mrpdaemon.android.encdroidmc.PinCodeActivity;
import org.mrpdaemon.sec.encfs.EncFSFile;

import fr.starn.webdavServer.HttpServer;
import uk.co.senab.photoview.PhotoView;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.ViewGroup.LayoutParams;

public class ViewPagerActivity extends Activity {

	private ViewPager mViewPager;
	//public List<String> urlImages;
	public List<EncFSFile> playableFiles;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		try {
			requestWindowFeature(Window.FEATURE_NO_TITLE);
		} catch (Exception e) {e.printStackTrace();}
		
		Intent intent = this.getIntent();
		String selectedFile = intent.getStringExtra("selectedFile");
		playableFiles = new ArrayList<EncFSFile>();
		
		
		int position = 0;
		
		for (EncFSFile file:EDVolumeBrowserActivity.childEncFSFiles){
			if (MimeManagement.isImage(file.getName())){
				playableFiles.add(file);
			}
		}
		Collections.sort(playableFiles);
		
		//search position of current image
		int i = 0;
		for (EncFSFile file:playableFiles){
				if (file.getName().equals(selectedFile)){
					position=i;
				}
				i++;
		}
		
		mViewPager = new HackyViewPager(this);
		

		PagerAdapter pa = new SamplePagerAdapter(this);
		mViewPager.setAdapter(pa);
		mViewPager.setCurrentItem(position);
		
		setContentView(mViewPager);
		
		if(android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.HONEYCOMB) {
		    getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
		if (PinCodeActivity.needPinCode(this)){
			Intent pinCode = new Intent(this, PinCodeActivity.class);
			startActivity(pinCode);
		}
	}


}
