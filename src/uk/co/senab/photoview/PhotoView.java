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

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.concurrent.Semaphore;

import uk.co.senab.photoview.PhotoViewAttacher.OnMatrixChangedListener;
import uk.co.senab.photoview.PhotoViewAttacher.OnPhotoTapListener;
import uk.co.senab.photoview.PhotoViewAttacher.OnViewTapListener;
import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.util.AttributeSet;
import android.widget.ImageView;

public class PhotoView extends ImageView implements IPhotoView, Runnable {

	private final PhotoViewAttacher mAttacher;

	private ScaleType mPendingScaleType;
	
	String bitmapUrl;
	Activity parent;
	SamplePagerAdapter samplePagerAdapter;
	int position;
	int resource;
	static boolean inThread = false;
	
	public PhotoView(int position, Activity parent,SamplePagerAdapter samplePagerAdapter, Context context) {
		this(context, null);
		this.parent=parent;
		this.samplePagerAdapter=samplePagerAdapter;
		this.position=position;
	}

	public PhotoView(Context context, AttributeSet attr) {
		this(context, attr, 0);
	}
	
	public PhotoView(Context context, AttributeSet attr, int defStyle) {
		super(context, attr, defStyle);
		super.setScaleType(ScaleType.MATRIX);
		mAttacher = new PhotoViewAttacher(this);

		if (null != mPendingScaleType) {
			setScaleType(mPendingScaleType);
			mPendingScaleType = null;
		}
	}

	@Override
	public boolean canZoom() {
		return mAttacher.canZoom();
	}

	@Override
	public RectF getDisplayRect() {
		return mAttacher.getDisplayRect();
	}

	@Override
	public float getMinScale() {
		return mAttacher.getMinScale();
	}

	@Override
	public float getMidScale() {
		return mAttacher.getMidScale();
	}

	@Override
	public float getMaxScale() {
		return mAttacher.getMaxScale();
	}

	@Override
	public float getScale() {
		return mAttacher.getScale();
	}

	@Override
	public ScaleType getScaleType() {
		return mAttacher.getScaleType();
	}

    @Override
    public void setAllowParentInterceptOnEdge(boolean allow) {
        mAttacher.setAllowParentInterceptOnEdge(allow);
    }

    @Override
	public void setMinScale(float minScale) {
		mAttacher.setMinScale(minScale);
	}

	@Override
	public void setMidScale(float midScale) {
		mAttacher.setMidScale(midScale);
	}

	@Override
	public void setMaxScale(float maxScale) {
		mAttacher.setMaxScale(maxScale);
	}

	@Override
	// setImageBitmap calls through to this method
	public void setImageDrawable(Drawable drawable) {
		super.setImageDrawable(drawable);
		if (null != mAttacher) {
			try {
				mAttacher.update();
			} catch (Exception e){
				//if the user skip its photos too fast,
				//an illegalStateException may appen (the image does not exist and cannot be attached).
				e.printStackTrace();
			}
		}
	}
	
	
	public void setImageUrl(String url) {
		int resource = this.getContext().getResources().getIdentifier("wait", "drawable", this.getContext().getPackageName());
		setImageResource(resource);
		//super.setImageBitmap (loadBitmap(url));
		bitmapUrl=url;
//		bitmapLoadedFromUrl = null;
		new Thread(this).start();

		

	}	
	
	public Bitmap loadBitmap(String url) {
		
	    InputStream in = null;
//	    BufferedOutputStream out = null;

	    try {
        	//while (inThread) Thread.sleep(100);
        	//inThread=true;
	        in = new URL(url).openStream();
	        byte[] data = inputStreamToByteArray(in);
	        
//	        final ByteArrayOutputStream dataStream = new ByteArrayOutputStream();
//	        out = new BufferedOutputStream(dataStream);
//	        copy(in, out);
//	        out.flush();

	        //final byte[] data = dataStream.toByteArray();
	        //BitmapFactory.Options options = new BitmapFactory.Options();
	        //options.inSampleSize = 1;


	        System.out.println(url+" => "+data.length);
        	System.out.println("****** debut:"+bitmapUrl);
        	
        	Bitmap bitmapLoadedFromUrl = BitmapFactory.decodeByteArray(data, 0, data.length);//,options);
        	System.out.println("fin:"+bitmapUrl);
        	inThread=false;
        	
	        //bitmapLoadedFromUrl = BitmapFactory.decodeStream(in);
	        in.close();
	        return bitmapLoadedFromUrl;
	    } catch (Exception e) {
	    	inThread=false;
	    	e.printStackTrace();
	        System.out.println( "Could not load Bitmap from: " + url);
			
			resource = this.getContext().getResources().getIdentifier("error", "drawable", this.getContext().getPackageName());
			parent.runOnUiThread(new Runnable() {
			     public void run() {
			    	 samplePagerAdapter.setImageResource(position,resource);
					
					if (null != mAttacher) {
						try {
							mAttacher.update();
						} catch (Exception e){
							//if the user skip its photos too fast,
							//an illegalStateException may appen (the image does not exist and cannot be attached).
							e.printStackTrace();
						}
					}

			    }
			});
			return null;
	    }

	    //return bitmap;
	}
	
	private static byte[] inputStreamToByteArray(InputStream is){
		try {
			ByteArrayOutputStream buffer = new ByteArrayOutputStream();
	
			int nRead;
			byte[] data = new byte[16384];
	
			while ((nRead = is.read(data, 0, data.length)) != -1) {
			  buffer.write(data, 0, nRead);
			}
	
			buffer.flush();
	
			return buffer.toByteArray();
		} catch (Exception e){
			throw new RuntimeException(e);
		}
	}

	@Override
	public void setImageResource(int resId) {
		try {
			super.setImageResource(resId);
			if (null != mAttacher) {
				mAttacher.update();
			}
		} catch (IllegalStateException e){
			//sometime happen when the user skip images too fast
			e.printStackTrace();
		}
	}
	
	@Override
	public void setImageBitmap(Bitmap bitmap){
		try {
			super.setImageBitmap(bitmap);
			if (null != mAttacher) {
				mAttacher.update();
			}
		} catch (IllegalStateException e){
			//sometime happen when the user skip images too fast
			e.printStackTrace();
		}
	}

	@Override
	public void setImageURI(Uri uri) {
		super.setImageURI(uri);
		if (null != mAttacher) {
			mAttacher.update();
		}
	}

	@Override
	public void setOnMatrixChangeListener(OnMatrixChangedListener listener) {
		mAttacher.setOnMatrixChangeListener(listener);
	}

	@Override
	public void setOnLongClickListener(OnLongClickListener l) {
		mAttacher.setOnLongClickListener(l);
	}

	@Override
	public void setOnPhotoTapListener(OnPhotoTapListener listener) {
		mAttacher.setOnPhotoTapListener(listener);
	}

	@Override
	public void setOnViewTapListener(OnViewTapListener listener) {
		mAttacher.setOnViewTapListener(listener);
	}

	@Override
	public void setScaleType(ScaleType scaleType) {
		if (null != mAttacher) {
			mAttacher.setScaleType(scaleType);
		} else {
			mPendingScaleType = scaleType;
		}
	}

	@Override
	public void setZoomable(boolean zoomable) {
		mAttacher.setZoomable(zoomable);
	}

	@Override
	public void zoomTo(float scale, float focalX, float focalY) {
		mAttacher.zoomTo(scale, focalX, focalY);
	}

	@Override
	protected void onDetachedFromWindow() {
		mAttacher.cleanup();
		super.onDetachedFromWindow();
	}

	@Override
	public void run() {
		Bitmap	bitmap = loadBitmap(bitmapUrl);
		parent.runOnUiThread(new UiUpdate(bitmap));

	}
	
	private class UiUpdate implements Runnable{
		Bitmap bitmap;
		public UiUpdate(Bitmap bitmap){
			this.bitmap=bitmap;
		}
			
		
		@Override
		public void run() {
	    	 samplePagerAdapter.setImageBitmap (position,bitmap);
	    	 
			if (null != mAttacher) {
				try {
					mAttacher.update();
				} catch (Exception e){
					//if the user skip its photos too fast,
					//an illegalStateException may appen (the image does not exist and cannot be attached).
					e.printStackTrace();
				}
			}
		}
		
		
	}

}

