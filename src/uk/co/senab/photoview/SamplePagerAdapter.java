package uk.co.senab.photoview;

import java.util.HashMap;
import java.util.Map;

import fr.starn.webdavServer.HttpServer;
import android.graphics.Bitmap;
import android.net.Uri;
import android.support.v4.view.PagerAdapter;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;

public class SamplePagerAdapter extends PagerAdapter {
		ViewPagerActivity parent;
		Map<Integer,PhotoView> photoViewList;
		
		SamplePagerAdapter(ViewPagerActivity parent){
			this.parent=parent;
			this.photoViewList=new HashMap<Integer,PhotoView>();
			
		}

//		private static int[] sDrawables = { R.drawable.wallpaper, R.drawable.wallpaper, R.drawable.wallpaper,
//				R.drawable.wallpaper, R.drawable.wallpaper, R.drawable.wallpaper };

		@Override
		public int getCount() {
			return parent.playableFiles.size();//urlImages.size();
		}

		@Override
		public View instantiateItem(ViewGroup container, int position) {
			PhotoView photoView=new PhotoView(position,parent,this, container.getContext());
			//photoView.setImageResource(sDrawables[position]);

			//photoView.setImageUrl(parent.urlImages.get(position));
			HttpServer server = HttpServer.getInstance();
			String id = server.setFile(parent.playableFiles.get(position));
			
			photoView.setImageUrl("http://127.0.0.1:8080/"+id);
			photoViewList.put(position, photoView);
			
			// Now just add PhotoView to ViewPager and return it
			container.addView(photoView, LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);

			return photoView;
		}
		
		
		//used to add image
		public void setImageBitmap(int position, Bitmap bm){
			if (photoViewList.get(position)!=null) {
				photoViewList.get(position).setImageBitmap(bm);
				photoViewList.get(position).refreshDrawableState();
			}
			
		}
		

		
		
		//used to add loading icon
		public void setImageResource(int position, int resId){
			if (photoViewList.get(position)!=null) photoViewList.get(position).setImageResource(resId);
		}

		@Override
		public void destroyItem(ViewGroup container, int position, Object object) {
			container.removeView((View) object);
			photoViewList.remove(position);
		}

		@Override
		public boolean isViewFromObject(View view, Object object) {
			return view == object;
		}

	}